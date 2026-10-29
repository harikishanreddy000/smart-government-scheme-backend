package com.example.smart.government.scheme.dto;

import java.time.Instant;
import java.util.List;

public record EligibilityResponse(Instant checkedAt, String disclaimer, List<SchemeMatch> matches) {
}
