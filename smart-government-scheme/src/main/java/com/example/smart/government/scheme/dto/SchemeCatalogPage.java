package com.example.smart.government.scheme.dto;

import java.util.List;
import java.util.Map;

import com.example.smart.government.scheme.model.CatalogScheme;

public record SchemeCatalogPage(
        long total,
        int page,
        int size,
        int totalPages,
        List<CatalogScheme> schemes,
        Map<String, List<CatalogFacet>> facets,
        String source,
        String notice) {
}
