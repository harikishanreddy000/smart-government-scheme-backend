package com.example.smart.government.scheme.dto;

public record EligibilityRequest(
        Integer age,
        String gender,
        Integer annualIncome,
        String socialCategory,
        String state,
        String residence,
        String occupation,
        String specialStatus) {
}
