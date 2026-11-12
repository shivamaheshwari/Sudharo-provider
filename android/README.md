# Sudharo Provider Android UI

Native Android starter UI built with Kotlin, Jetpack Compose and Material 3.

## Included screens
- Login, signup, email OTP entry, forgot password
- Provider onboarding
- Dashboard
- Requirements list and details
- Bid submission
- My jobs and job details
- Quotations list, create quotation and quotation details
- Provider profile, edit profile and portfolio upload entry
- Notifications

## Open in Android Studio
1. Extract the ZIP.
2. Open the `SudharoProviderAndroid` folder in Android Studio.
3. Use JDK 17.
4. Allow Gradle sync to complete.
5. Run on an Android emulator or device (API 26+).

## Important: demo UI vs production services
This is a UI starter, not a connected production backend. Demo interactions navigate between screens and display explanatory messages. Login, Google sign-in, email delivery, OTP verification, data persistence, image upload, PDF creation, push notifications and server-side job completion are not active until configured and integrated.

## Production integration checklist
- Configure Supabase project and Android client.
- Configure email OTP template using `{{ .Token }}` and verified SMTP sender (e.g. Resend).
- Configure Google OAuth credentials, redirect URI and deep links.
- Add a NestJS API and validate Supabase JWTs server-side.
- Add PostgreSQL schema and authorization policies.
- Implement provider role checks and onboarding status.
- Implement requirements, bids, jobs and quotation APIs.
- Implement completion OTP separately on the backend; never trust client-side job status changes.
- Add Firebase FCM, upload storage, PDF generation and Crashlytics.
- Add validation, loading/error states, accessibility, tests and privacy disclosures before release.

## Sample data
Names, prices, request IDs and dates in the UI are illustrative placeholders. Replace them with API responses.
