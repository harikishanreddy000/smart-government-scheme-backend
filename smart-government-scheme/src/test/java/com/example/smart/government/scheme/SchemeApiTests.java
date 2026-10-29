package com.example.smart.government.scheme;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.smart.government.scheme.dto.CatalogFacet;
import com.example.smart.government.scheme.dto.SchemeCatalogPage;
import com.example.smart.government.scheme.model.CatalogScheme;
import com.example.smart.government.scheme.service.OfficialSchemeCatalogService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@SpringBootTest
@AutoConfigureMockMvc
class SchemeApiTests {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
        private OfficialSchemeCatalogService catalogService;

        private static final CatalogScheme PM_KISAN = new CatalogScheme(
            "pm-kisan", "PM-KISAN", "Pradhan Mantri Kisan Samman Nidhi", "Agriculture", "leaf", "green",
            "Income support for eligible farmer families.", "See the official scheme page for current benefits.",
            List.of("Check current details on the official portal."), "myScheme — Government of India",
            "https://www.myscheme.gov.in/schemes/pm-kisan", "Central", List.of("All"), "Ministry of Agriculture", List.of("Farmer"));

        private SchemeCatalogPage catalogPage() {
        return new SchemeCatalogPage(725, 0, 24, 31, List.of(PM_KISAN),
            Map.of("schemeCategory", List.of(new CatalogFacet("Agriculture", 85))),
            "myScheme — Government of India", "Catalogue listing does not confirm eligibility.");
        }

    @Test
    void healthEndpointReportsServiceUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("smart-government-scheme"));
    }

    @Test
    void schemeApiAllowsTheLocalFrontendOrigin() throws Exception {
        when(catalogService.search(anyString(), any(), any(), any(), anyInt(), anyInt())).thenReturn(catalogPage());
        mockMvc.perform(get("/api/schemes").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void catalogCanBeFilteredByCategoryAndSearchTerm() throws Exception {
        when(catalogService.search("all", null, "Agriculture", "kisan", 0, 24)).thenReturn(catalogPage());
        mockMvc.perform(get("/api/schemes").param("category", "Agriculture").param("q", "kisan"))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(725))
            .andExpect(jsonPath("$.schemes[0].id").value("pm-kisan"))
            .andExpect(jsonPath("$.schemes[0].beneficiaryStates[0]").value("All"))
            .andExpect(jsonPath("$.facets.schemeCategory[0].label").value("Agriculture"));
        }

        @Test
        void catalogAcceptsCentralStateAndPaginationFilters() throws Exception {
        when(catalogService.search("state", "Karnataka", null, null, 2, 24)).thenReturn(catalogPage());
        mockMvc.perform(get("/api/schemes")
                .param("level", "state")
                .param("state", "Karnataka")
                .param("page", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schemes[0].level").value("Central"));
    }

    @Test
    void catalogCanListTelanganaStateSchemes() throws Exception {
        when(catalogService.search("state", "Telangana", null, null, 0, 24)).thenReturn(catalogPage());
        mockMvc.perform(get("/api/schemes")
                .param("level", "state")
                .param("state", "Telangana"))
            .andExpect(status().isOk());
        verify(catalogService).search("state", "Telangana", null, null, 0, 24);
    }

    @Test
    void schemeDetailsReturnNotFoundForUnknownScheme() throws Exception {
        when(catalogService.search("all", null, null, "not-a-scheme", 0, 100))
            .thenReturn(new SchemeCatalogPage(0, 0, 100, 0, List.of(), Map.of(), "myScheme", ""));
        mockMvc.perform(get("/api/schemes/not-a-scheme"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No scheme found with id 'not-a-scheme'."));
    }

    @Test
    void eligibilityCheckReturnsPotentialAndReviewResults() throws Exception {
        mockMvc.perform(post("/api/eligibility/check")
                        .contentType("application/json")
                        .content("""
                                {
                                  "age": 30,
                                  "gender": "woman",
                                  "annualIncome": 180000,
                                  "socialCategory": "obc",
                                  "state": "Karnataka",
                                  "residence": "rural",
                                  "occupation": "farmer",
                                  "specialStatus": "none"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].scheme.id").value("pm-kisan"))
                .andExpect(jsonPath("$.matches[0].status").value("review"))
                .andExpect(jsonPath("$.matches[1].scheme.id").value("pm-ujjwala"))
                .andExpect(jsonPath("$.matches[1].status").value("review"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

        @Test
        void officialCatalogueFailureReturnsGatewayError() throws Exception {
        when(catalogService.search(anyString(), any(), any(), any(), anyInt(), anyInt()))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("upstream timeout"));
        mockMvc.perform(get("/api/schemes"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.message").value("The official myScheme catalogue is temporarily unavailable. Please retry in a moment."));
        }

    @Test
    void eligibilityCheckRejectsInvalidProfile() throws Exception {
        mockMvc.perform(post("/api/eligibility/check")
                        .contentType("application/json")
                        .content("{\"age\":0,\"gender\":\"woman\",\"annualIncome\":180000,\"socialCategory\":\"obc\",\"state\":\"Karnataka\",\"residence\":\"rural\",\"occupation\":\"farmer\",\"specialStatus\":\"none\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Age must be between 1 and 100."));
    }

    @Test
    void eligibilityCheckRejectsUnknownAnswerValues() throws Exception {
        mockMvc.perform(post("/api/eligibility/check")
                        .contentType("application/json")
                        .content("{\"age\":30,\"gender\":\"woman\",\"annualIncome\":180000,\"socialCategory\":\"obc\",\"state\":\"Karnataka\",\"residence\":\"rural\",\"occupation\":\"hacker\",\"specialStatus\":\"none\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or missing value for occupation."));
    }
}
