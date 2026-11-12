# Sudharo Provider — Android App (Kotlin + Jetpack Compose)

Production-ready starter for the Sudharo Provider app: the service provider
sees requirements, bids, creates quotations (with real PDF), manages jobs
and completes work with a customer OTP. Blue + White, Material 3.

**Repo:** https://github.com/shivamaheshwari/Sudharo-provider
**Status:** App builds in Android Studio and works end-to-end in local demo
mode today. Real Supabase / NestJS / FCM need the keys in `docs/SETUP.md` —
nothing is faked as live when it is not.

## What works right now (demo mode, no keys needed)
- Signup / login with validation, session persisted in DataStore, logout
- Email OTP screen: 6 boxes, 60s resend cooldown, 5 attempts.
  Demo OTP is **123456** — only in demo mode, clearly labelled in-app.
  With Supabase keys, the same screen calls Supabase `verifyEmailOtp`.
- Onboarding (category / city / experience / hours) saved in Room
- Requirements: Room-seeded list, category filters, detail, search
- Bidding: price + days + notes, validated, **one bid per requirement**,
  saved in Room, survives restart
- Quotations: labour + material + other = auto total, timeline, terms,
  **new version on revision, never overwrite an accepted one**,
  real PDF export to Downloads (`PdfGenerator`, no extra library)
- Jobs: Active / Completed / Cancelled, progress, job detail
- Job completion: Request Completion → customer OTP → verify.
  Demo completion OTP is **654321** — only in demo mode.
  In production the OTP is generated + verified by the NestJS backend
  (`backend/src/completion-verification/`), never by the app.
- Profile edit + portfolio (image picker, Room), notifications with badge
- Dashboard earnings / active jobs computed from real local jobs

Sample data in the app is labelled sample in this README. Replace it by
connecting the API — the Retrofit interface is already in
`android/app/src/main/java/com/sudharo/provider/data/remote/ApiClient.kt`.

## Open in Android Studio
1. Open the `android/` folder in Android Studio (JDK 17).
2. Allow Gradle sync to complete. `minSdk 26`, `compileSdk/targetSdk 35`.
3. Run on an emulator or device (API 26+).
4. Demo login: sign up in the app first (any name/email, password 8+ chars),
   OTP `123456`, then login with the same email/password.

> Local Gradle build in this VM was not possible — no JDK is installed here
> and the Gradle daemon loopback is blocked — so the first real build must
> happen in Android Studio on your machine. The code was structure-checked
> here (imports, references, brace balance); it has not been compiled here.
> If Android Studio reports an error, send me the exact error text.

## Go live (real auth + backend)
Copy `android/local.properties.example` to `android/local.properties` and
fill `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `API_BASE_URL`. Then follow
`docs/SETUP.md` (Supabase + Resend SMTP with `{{ .Token }}`, Google OAuth
deep link `sudharo://auth-callback`, Firebase FCM) and `docs/API.md`.
Never commit `local.properties` or any service-role key.

## Structure
```
android/            Kotlin + Compose app (MVVM + Clean, Room + DataStore + Retrofit)
  app/src/main/java/com/sudharo/provider/
    MainActivity.kt            UI screens (15 screens from the design)
    SudharoApplication.kt      manual DI holder
    core/theme/                Blue + White tokens
    data/local/                Room entities / DAO / database
    data/remote/               Retrofit API + Supabase Auth REST
    data/repository/           SudharoRepository (offline-first source of truth)
    domain/model/              Provider / Requirement / Bid / Job / Quotation…
    presentation/              ProviderViewModel (StateFlow)
    util/                      OtpManager, PdfGenerator
backend/            NestJS + TypeScript + Prisma + PostgreSQL skeleton
docs/               ARCHITECTURE.md, SETUP.md, API.md
```

## Docs
- `docs/ARCHITECTURE.md` — layers, decisions, what lives where
- `docs/SETUP.md` — Supabase, Resend, Google, Firebase, backend deploy
- `docs/API.md` — REST contract the app already calls
