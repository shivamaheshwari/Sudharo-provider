# Setup — go live

## 1. Supabase (Auth + DB)
1. Create a project at supabase.com. Note Project URL + anon key.
2. Auth → Email: enable Email, set OTP expiry 5 min.
3. Auth → Email Templates → Confirm signup: use `{{ .Token }}` (6-digit
   code), NOT a confirmation link. Branded Sudharo HTML.
4. Auth → Providers → Google: add Google Cloud OAuth client ID/secret,
   redirect `https://<project-ref>.supabase.co/auth/v1/callback`.
   Android deep link in the manifest is `sudharo://auth-callback`.
5. SMTP (so OTP emails actually arrive): Auth → SMTP → Custom SMTP,
   Resend (or AWS SES): host `smtp.resend.com`, port 465, user `resend`,
   password = Resend API key, sender = verified domain address.
6. Copy `android/local.properties.example` → `local.properties` and fill
   `SUPABASE_URL` + `SUPABASE_ANON_KEY`. The app then leaves demo mode.

## 2. Backend (NestJS + PostgreSQL)
```bash
cd backend
cp .env.example .env   # DATABASE_URL, SUPABASE_URL, PORT, CORS_ORIGINS
npm install
npx prisma migrate dev
npm run start:dev
```
Deploy to Render / AWS. Set `API_BASE_URL` in `local.properties` to
`https://<your-api>/api/v1/`.

## 3. Firebase (push)
Create a Firebase project, add the Android app, download
`google-services.json` into `android/app/` (do NOT commit it), enable
Cloud Messaging. FCM token sync is wired via WorkManager in the app;
server-side send lives in `backend/src/notifications/`.

## 4. Google Play (when ready)
Play Console account, privacy policy + terms, app icon, screenshots
from the 15 screens, release build in Android Studio
(Build → Generate Signed App Bundle).

## Demo codes (demo mode ONLY)
- Signup/login OTP: `123456`
- Job-completion OTP: `654321`
These are hard-coded fallbacks only when no Supabase keys are set, and
are labelled in-app. They must never be used in production.
