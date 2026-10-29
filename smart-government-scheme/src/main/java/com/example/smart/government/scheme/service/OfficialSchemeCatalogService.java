package com.example.smart.government.scheme.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.smart.government.scheme.dto.CatalogFacet;
import com.example.smart.government.scheme.dto.SchemeCatalogPage;
import com.example.smart.government.scheme.model.CatalogScheme;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OfficialSchemeCatalogService {

    private static final String SOURCE = "myScheme — Government of India";
    private static final String NOTICE = "Catalogue information is supplied by the myScheme government portal. Listing a scheme does not confirm current availability or eligibility; follow its official portal for the latest details.";
    private static final List<String> FALLBACK_CRITERIA = List.of("Eligibility, documents, application process, and current availability are maintained by the scheme authority. Open the official myScheme page for the latest details.");

    private final RestTemplate restTemplate;
    private final String searchUrl;

    public OfficialSchemeCatalogService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${app.myscheme.base-url:https://www.myscheme.gov.in}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.searchUrl = baseUrl.replaceAll("/$", "") + "/api/apisetu/search/schemes";
    }

    public SchemeCatalogPage search(String level, String state, String category, String query, int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 100));
        String filters = buildFilters(level, state, category);
        int from = Math.multiplyExact(page, size);

        URI uri = UriComponentsBuilder.fromUriString(searchUrl)
                .queryParam("lang", "en")
                .queryParam("q", filters)
                .queryParam("keyword", query == null ? "" : query.trim())
                .queryParam("sort", "multiple_sort")
                .queryParam("from", from)
                .queryParam("size", size)
                .build()
                .encode()
                .toUri();

        String body = restTemplate.getForObject(uri, String.class);
        if (body == null || body.isBlank()) {
            throw new RestClientException("The official scheme catalogue returned an empty response.");
        }

        JsonNode root;
        try {
            root = new ObjectMapper().readTree(body);
        } catch (Exception exception) {
            throw new RestClientException("The official scheme catalogue returned an invalid response.", exception);
        }
        if (!root.has("data")) {
            throw new RestClientException("The official scheme catalogue returned an empty response.");
        }
        JsonNode data = root.path("data");
        JsonNode items = data.path("hits").path("items");
        List<CatalogScheme> schemes = new ArrayList<>();
        if (items.isArray()) {
            items.forEach(item -> schemes.add(mapScheme(item)));
        }

        long total = data.path("summary").path("total").asLong(schemes.size());
        int totalPages = (int) Math.ceil((double) total / size);
        return new SchemeCatalogPage(total, page, size, totalPages, schemes, mapFacets(data.path("facets")), SOURCE, NOTICE);
    }

    private String buildFilters(String level, String state, String category) {
        List<Map<String, String>> filters = new ArrayList<>();
        String normalizedLevel = level == null ? "all" : level.trim().toLowerCase(Locale.ROOT);
        if (normalizedLevel.equals("central")) {
            filters.add(filter("level", "Central"));
            filters.add(filter("beneficiaryState", "All"));
        } else if (normalizedLevel.equals("state")) {
            filters.add(filter("level", "State"));
            if (state != null && !state.isBlank()) {
                filters.add(filter("beneficiaryState", state.trim()));
            }
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all schemes")) {
            filters.add(filter("schemeCategory", category.trim()));
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(filters);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Could not prepare catalogue filters.", exception);
        }
    }

    private Map<String, String> filter(String identifier, String value) {
        Map<String, String> filter = new LinkedHashMap<>();
        filter.put("identifier", identifier);
        filter.put("value", value);
        return filter;
    }

    private CatalogScheme mapScheme(JsonNode item) {
        JsonNode fields = item.path("fields");
        String slug = text(fields, "slug", text(item, "id", "scheme"));
        String fullName = text(fields, "schemeName", "Government scheme");
        String shortName = text(fields, "schemeShortTitle", fullName);
        String category = firstText(fields.path("schemeCategory"), "Other");
        String level = text(fields, "level", "State");
        List<String> beneficiaryStates = strings(fields.path("beneficiaryState"));
        String description = text(fields, "briefDescription", "Explore the official scheme page for an overview and current details.");
        String ministry = text(fields, "nodalMinistryName", "");
        List<String> tags = strings(fields.path("tags"));

        return new CatalogScheme(
                slug,
                shortName,
                fullName,
                category,
                iconFor(category),
                accentFor(category),
                description,
                "See the official scheme page for current benefits and assistance.",
                FALLBACK_CRITERIA,
                SOURCE,
                "https://www.myscheme.gov.in/schemes/" + slug,
                level,
                beneficiaryStates,
                ministry,
                tags);
    }

    private Map<String, List<CatalogFacet>> mapFacets(JsonNode facets) {
        Map<String, List<CatalogFacet>> result = new LinkedHashMap<>();
        if (!facets.isArray()) return result;
        facets.forEach(facet -> {
            String identifier = text(facet, "identifier", "");
            if (identifier.isBlank()) return;
            List<CatalogFacet> entries = new ArrayList<>();
            JsonNode rawEntries = facet.path("entries");
            if (rawEntries.isArray()) {
                rawEntries.forEach(entry -> entries.add(new CatalogFacet(text(entry, "label", ""), entry.path("count").asLong())));
            }
            result.put(identifier, entries);
        });
        return result;
    }

    private String text(JsonNode node, String key, String fallback) {
        JsonNode value = node.path(key);
        return value.isMissingNode() || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
    }

    private String firstText(JsonNode node, String fallback) {
        if (node.isArray() && !node.isEmpty()) return node.get(0).asText(fallback);
        if (!node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) return node.asText();
        return fallback;
    }

    private List<String> strings(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) node.forEach(value -> { if (!value.asText().isBlank()) result.add(value.asText()); });
        else if (!node.isMissingNode() && !node.isNull() && !node.asText().isBlank()) result.add(node.asText());
        return result;
    }

    private String iconFor(String category) {
        String value = category.toLowerCase(Locale.ROOT);
        if (value.contains("agriculture") || value.contains("rural")) return "leaf";
        if (value.contains("health")) return "heart";
        if (value.contains("education")) return "compass";
        if (value.contains("housing")) return "home";
        if (value.contains("women") || value.contains("child")) return "user";
        if (value.contains("energy") || value.contains("utility")) return "sun";
        return "shield";
    }

    private String accentFor(String category) {
        String value = category.toLowerCase(Locale.ROOT);
        if (value.contains("agriculture") || value.contains("rural")) return "green";
        if (value.contains("health")) return "blue";
        if (value.contains("education")) return "violet";
        if (value.contains("housing")) return "orange";
        if (value.contains("women") || value.contains("child")) return "gold";
        return "green";
    }
}
