package com.example.smart.government.scheme.model;

import java.util.List;

public record Scheme(
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
        String url) {
}
