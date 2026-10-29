# Smart Government Scheme Eligibility Checker — API

Spring Boot REST API used by the Sahaaya React frontend. It serves the scheme catalogue and screens a submitted profile against a small, clearly documented sample of central schemes. Screening is informational, not an official eligibility determination.

## Run

From this directory, run `./mvnw spring-boot:run` (or `mvn spring-boot:run` if Maven is installed). The default API address is `http://localhost:8080`.

Run tests with `./mvnw test`.

Configuration:

- `PORT` — HTTP port; defaults to `8080`.
- `FRONTEND_ORIGIN` — allowed browser origin for CORS; defaults to `http://localhost:5173`.

No database is required. The API does not persist submitted eligibility profiles.

## Endpoints

- `GET /api/health` — service status.
- `GET /api/schemes` — all schemes. Optional `category` and `q` query parameters filter by category and text.
- `GET /api/schemes/{id}` — scheme details; returns `404` for an unknown ID.
- `POST /api/eligibility/check` — validate a submitted profile and return potential matches / schemes to verify.
Eligibility request JSON:

```json
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
```

Allowed values: `gender` is `woman`, `man`, `other`, or `prefer-not`; `socialCategory` is `general`, `sc`, `st`, `obc`, or `prefer-not`; `residence` is `rural` or `urban`; `occupation` is `farmer`, `student`, `street-vendor`, `self-employed`, `employed`, or `other`; `specialStatus` is `none`, `disability`, `senior-citizen`, `minority`, or `ex-serviceman`. `annualIncome` is an integer from 0 to 10,000,000. State must be one of the Indian states / union territories shown by the frontend.

Example response shape:

```json
{
  "checkedAt": "2026-10-08T00:00:00Z",
  "disclaimer": "This is an informational screening only...",
  "matches": [
    {
      "scheme": { "id": "pm-kisan", "name": "PM-KISAN" },
      "status": "potential",
      "reason": "Confirm official exclusions and records."
    }
  ]
}
```

Match statuses are `potential` (answers resemble selected basic criteria) and `review` (official lists, local rules, or further checks are needed). Schemes that do not match the simple screen are omitted. A `potential` status is not proof of eligibility.

## Scheme catalogue and limitations

The API currently includes PM-KISAN, PM Ujjwala Yojana, Ayushman Bharat PM-JAY, PMAY-Gramin, and PM Surya Ghar. Scheme descriptions link to official government sources. Current rules, household income/deprivation criteria, official beneficiary records, exclusions, document checks, state variations, and application availability are not integrated or independently verified by this sample checker. Do not use it to approve or deny benefits. Do not add Aadhaar, bank, or other sensitive identifiers to this screening form.
