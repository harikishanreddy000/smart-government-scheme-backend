package com.example.smart.government.scheme.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.smart.government.scheme.dto.EligibilityRequest;
import com.example.smart.government.scheme.dto.EligibilityResponse;
import com.example.smart.government.scheme.dto.HealthResponse;
import com.example.smart.government.scheme.dto.SchemeCatalogPage;
import com.example.smart.government.scheme.model.CatalogScheme;
import com.example.smart.government.scheme.service.OfficialSchemeCatalogService;
import com.example.smart.government.scheme.service.SchemeService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:5173}")
public class SchemeController {

    private final SchemeService schemeService;
    private final OfficialSchemeCatalogService catalogService;

    public SchemeController(SchemeService schemeService, OfficialSchemeCatalogService catalogService) {
        this.schemeService = schemeService;
        this.catalogService = catalogService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "smart-government-scheme");
    }

    @GetMapping("/schemes")
    public SchemeCatalogPage listSchemes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "all") String level,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return catalogService.search(level, state, category, query, page, size);
    }

    @GetMapping("/schemes/{id}")
    public CatalogScheme getScheme(@PathVariable String id) {
        SchemeCatalogPage result = catalogService.search("all", null, null, id, 0, 100);
        return result.schemes().stream()
                .filter(scheme -> scheme.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new SchemeService.SchemeNotFoundException(id));
    }

    @PostMapping("/eligibility/check")
    public EligibilityResponse checkEligibility(@RequestBody EligibilityRequest request) {
        return schemeService.checkEligibility(request);
    }
}
