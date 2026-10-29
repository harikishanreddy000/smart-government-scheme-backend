package com.example.smart.government.scheme.dto;

import com.example.smart.government.scheme.model.Scheme;

public record SchemeMatch(Scheme scheme, String status, String reason) {
}
