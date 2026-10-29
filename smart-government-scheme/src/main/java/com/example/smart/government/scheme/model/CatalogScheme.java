package com.example.smart.government.scheme.model;

import java.util.List;

public record CatalogScheme(
        String id,
        String name,
        String fullName,
        String category,
        String icon,
        String accent,
        String summary,
        String benefit,
        List<String> criteria,
        String source,
        String url,
        String level,
        List<String> beneficiaryStates,
        String nodalMinistry,
        List<String> tags) {
}
