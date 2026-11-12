# Architecture

## Decision
Kotlin + Jetpack Compose (native Android), MVVM + Clean Architecture.
Supabase Auth owns identity. NestJS owns business logic. The Android app
never decides bidding rules, quotation approval, job completion or prices —
it calls the API and the server validates. Room is the offline-first cache.

## Layers (Android)
- Presentation: Compose screens in `MainActivity.kt`, `ProviderViewModel`
  (StateFlow, lifecycle-aware collection).
- Domain: models in `domain/model/`, validation in repository/use cases.
- Data: `SudharoRepository` → Room (`data/local/`) + Retrofit
  (`data/remote/ApiClient.kt`) + Supabase Auth REST
  (`data/remote/SupabaseAuthService.kt`) + DataStore session.
- Core: theme tokens, navigation (5-tab bottom nav: Home, Requirements,
  Jobs, Quotations, Profile).

## Two OTP systems — do not mix them
| | Auth OTP | Job-completion OTP |
|---|---|---|
| Owner | Supabase Auth | NestJS backend |
| Code | 6 digits, `{{ .Token }}` email template | 6 digits, hash stored only |
| Expiry | 5 min | 5 min |
| Attempts | 5 | 5 |
| Resend | 60s cooldown | new request, old code invalidated |

## Backend
NestJS modules: auth, providers, categories, requirements, bids, jobs,
quotations, completion-verification, portfolio, payments, notifications,
reviews, admin. Prisma schema in `backend/prisma/schema.prisma`
(Provider, Requirement, Bid unique per provider+requirement, Job,
Quotation with version, CompletionVerification with otpHash, AuditLog).
`SupabaseAuthGuard` validates RS256 JWTs via JWKS. Bid acceptance and
job completion must run in Prisma transactions + audit logs.

## Security
- No DB password / service-role key in the APK. Only anon key + URL in
  BuildConfig from `local.properties`.
- HTTPS only (`usesCleartextTraffic=false`), Keystore-backed session,
  signed upload URLs for images (to be added with Storage).
- Validate all prices/statuses server-side; client totals are display only.
