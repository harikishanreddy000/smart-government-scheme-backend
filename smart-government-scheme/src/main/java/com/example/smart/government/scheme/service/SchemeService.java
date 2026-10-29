package com.example.smart.government.scheme.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.smart.government.scheme.dto.EligibilityRequest;
import com.example.smart.government.scheme.dto.EligibilityResponse;
import com.example.smart.government.scheme.dto.SchemeMatch;
import com.example.smart.government.scheme.model.Scheme;

@Service
public class SchemeService {

    private static final String DISCLAIMER = "This is an informational screening only, not an official eligibility decision. Scheme rules, availability, and state implementation can change. Confirm requirements with the official source.";
    private static final Set<String> GENDERS = Set.of("woman", "man", "other", "prefer-not");
    private static final Set<String> SOCIAL_CATEGORIES = Set.of("general", "sc", "st", "obc", "prefer-not");
    private static final Set<String> RESIDENCES = Set.of("rural", "urban");
    private static final Set<String> OCCUPATIONS = Set.of("farmer", "student", "street-vendor", "self-employed", "employed", "other");
    private static final Set<String> SPECIAL_STATUSES = Set.of("none", "disability", "senior-citizen", "minority", "ex-serviceman");
        private static final Set<String> STATES = Set.of(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat", "Haryana",
            "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
            "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
            "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands",
            "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Jammu and Kashmir", "Ladakh",
            "Lakshadweep", "Puducherry");

    private final List<Scheme> schemes = List.of(
            new Scheme(
                    "pm-kisan", "PM-KISAN", "Pradhan Mantri Kisan Samman Nidhi", "Agriculture", "leaf", "green",
                    "Income support for eligible landholding farmer families across India.",
                    "₹6,000 per year, paid in three instalments to eligible beneficiaries.",
                    List.of(
                            "The applicant should be part of a landholding farmer family with cultivable land.",
                            "Certain institutional landholders and people in specified higher-income or public-service categories are excluded.",
                            "Land records and e-KYC must meet the current scheme requirements."),
                    "PM-KISAN official portal", "https://pmkisan.gov.in/"),
            new Scheme(
                    "pm-ujjwala", "PM Ujjwala Yojana", "Pradhan Mantri Ujjwala Yojana", "Energy & household", "flame", "orange",
                    "LPG connection support for adult women from eligible low-income households.",
                    "Support toward an LPG connection; current assistance and refill benefits are published on the official portal.",
                    List.of(
                            "The applicant must be a woman aged 18 or above.",
                            "The household must not already have an LPG connection.",
                            "The household must satisfy the scheme’s current deprivation / eligibility-category rules and document checks."),
                    "PMUY official portal", "https://www.pmuy.gov.in/"),
            new Scheme(
                    "pm-jay", "Ayushman Bharat PM-JAY", "Pradhan Mantri Jan Arogya Yojana", "Health", "heart", "blue",
                    "Cashless hospital cover for families included in the eligible beneficiary database.",
                    "Up to ₹5 lakh per eligible family per year for covered secondary and tertiary hospital care; state rules can vary.",
                    List.of(
                            "Eligibility is based on the official beneficiary database and applicable state coverage rules.",
                            "Check the beneficiary portal or contact an Ayushman help desk to confirm your family record.",
                            "A result here cannot confirm enrollment or treatment coverage."),
                    "National Health Authority beneficiary portal", "https://beneficiary.nha.gov.in/"),
            new Scheme(
                    "pmay-g", "PMAY-Gramin", "Pradhan Mantri Awaas Yojana – Gramin", "Housing", "home", "violet",
                    "Rural housing assistance for eligible households identified through official processes.",
                    "Housing assistance for eligible rural households; amount and selection rules depend on current guidelines and location.",
                    List.of(
                            "The household must meet rural housing deprivation and exclusion rules.",
                            "Selection is through official beneficiary lists / survey processes; a kutcha or no house alone does not guarantee selection.",
                            "Confirm the household entry with the Gram Panchayat or official PMAY-G portal."),
                    "PMAY-G official portal", "https://pmayg.nic.in/"),
            new Scheme(
                    "surya-ghar", "PM Surya Ghar", "PM Surya Ghar: Muft Bijli Yojana", "Energy & household", "sun", "gold",
                    "Rooftop solar support for eligible residential electricity consumers.",
                    "Central financial assistance for eligible residential rooftop solar systems; support depends on system size and current rules.",
                    List.of(
                            "A residential electricity connection and a suitable rooftop are generally needed.",
                            "Apply through the national portal and follow the DISCOM process for your area.",
                            "Check current subsidy, technical requirements and vendor details on the official portal."),
                    "PM Surya Ghar official portal", "https://pmsuryaghar.gov.in/"));

    public List<Scheme> listSchemes(String category, String query) {
        String normalizedCategory = normalize(category);
        String normalizedQuery = normalize(query);
        return schemes.stream()
                .filter(scheme -> normalizedCategory.isEmpty() || normalizedCategory.equals("all schemes")
                        || scheme.category().toLowerCase(Locale.ROOT).equals(normalizedCategory))
                .filter(scheme -> normalizedQuery.isEmpty()
                        || (scheme.name() + " " + scheme.fullName() + " " + scheme.category() + " " + scheme.summary())
                                .toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }

    public Scheme getScheme(String id) {
        return schemes.stream()
                .filter(scheme -> scheme.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new SchemeNotFoundException(id));
    }

    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        validate(request);
        List<SchemeMatch> matches = schemes.stream()
                .map(scheme -> match(scheme, request))
                .filter(match -> !match.status().equals("none"))
                .toList();
        return new EligibilityResponse(java.time.Instant.now(), DISCLAIMER, matches);
    }

    private SchemeMatch match(Scheme scheme, EligibilityRequest profile) {
        return switch (scheme.id()) {
            case "pm-kisan" -> pmKisan(scheme, profile);
            case "pm-ujjwala" -> ujjwala(scheme, profile);
            case "pm-jay" -> health(scheme, profile);
            case "pmay-g" -> housing(scheme, profile);
            case "surya-ghar" -> solar(scheme, profile);
            default -> new SchemeMatch(scheme, "review", "Confirm your details and the current official scheme criteria.");
        };
    }

    private SchemeMatch pmKisan(Scheme scheme, EligibilityRequest profile) {
        if (!profile.occupation().equals("farmer")) {
            return new SchemeMatch(scheme, "none", "");
        }
        return new SchemeMatch(scheme, "review", "Your work may make this worth checking. Confirm landholding, exclusions, and land records on the official PM-KISAN portal.");
    }

    private SchemeMatch ujjwala(Scheme scheme, EligibilityRequest profile) {
        if (!profile.gender().equals("woman") || profile.age() < 18) {
            return new SchemeMatch(scheme, "none", "");
        }
        return new SchemeMatch(scheme, "review", "As an adult woman, you may wish to check PM Ujjwala Yojana. Household LPG status and current inclusion criteria must be verified on the official portal.");
    }

    private SchemeMatch health(Scheme scheme, EligibilityRequest profile) {
        return new SchemeMatch(scheme, "review", "Coverage depends on official beneficiary records and state rules. Check your family details on the official beneficiary portal.");
    }

    private SchemeMatch housing(Scheme scheme, EligibilityRequest profile) {
        if (!profile.residence().equals("rural")) {
            return new SchemeMatch(scheme, "none", "");
        }
        return new SchemeMatch(scheme, "review", "Rural housing assistance may be worth exploring. Final selection depends on official surveys, housing status, and beneficiary-list rules.");
    }

    private SchemeMatch solar(Scheme scheme, EligibilityRequest profile) {
        return new SchemeMatch(scheme, "review", "Check residential electricity connection, rooftop suitability, and your DISCOM’s requirements on the official PM Surya Ghar portal.");
    }

    private void validate(EligibilityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A profile is required.");
        }
        if (request.age() == null || request.age() < 1 || request.age() > 100) {
            throw new IllegalArgumentException("Age must be between 1 and 100.");
        }
        requireOneOf("gender", request.gender(), GENDERS);
        if (request.annualIncome() == null || request.annualIncome() < 0 || request.annualIncome() > 10_000_000) {
            throw new IllegalArgumentException("Annual family income must be between 0 and 10000000.");
        }
        requireOneOf("socialCategory", request.socialCategory(), SOCIAL_CATEGORIES);
        if (request.state() == null || !STATES.contains(request.state())) {
            throw new IllegalArgumentException("Choose a valid state or union territory.");
        }
        requireOneOf("residence", request.residence(), RESIDENCES);
        requireOneOf("occupation", request.occupation(), OCCUPATIONS);
        requireOneOf("specialStatus", request.specialStatus(), SPECIAL_STATUSES);
    }

    private void requireOneOf(String field, String value, Set<String> allowed) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("Invalid or missing value for " + field + ".");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static class SchemeNotFoundException extends RuntimeException {
        public SchemeNotFoundException(String id) {
            super("No scheme found with id '" + id + "'.");
        }
    }
}
