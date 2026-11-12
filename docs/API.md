# API contract (NestJS, base `/api/v1/`)

All protected routes: `Authorization: Bearer <Supabase access token>`,
validated by `SupabaseAuthGuard` + provider-role check.

| Method | Path | Purpose |
|---|---|---|
| GET | `/providers/me` | Provider profile |
| GET | `/dashboard` | Earnings, active jobs, recent requirements |
| GET | `/requirements` | List (filter `category`, `city`) |
| GET | `/requirements/:id` | Detail + customer images |
| POST | `/requirements/:id/bids` | Submit bid `{amount, days, notes}` — one per provider |
| GET | `/bids/my` | My bids |
| GET | `/jobs/my` | My jobs |
| POST | `/quotations` | Create `{jobId, title, labour, material, other, timelineDays, terms}` |
| POST | `/quotations/:id/send` | Send to customer |
| POST | `/portfolio` | Add portfolio item |
| POST | `/jobs/:id/completion/request` | Backend creates completion OTP, sends to customer |
| POST | `/jobs/:id/completion/verify` | Verify `{otp}` → job COMPLETED |
| GET | `/notifications` | Notifications |

Auth itself is Supabase Auth SDK/REST, not this API:
signup → OTP email → `verifyEmailOtp(type=email)` → login
`signInWith(Email)` / Google OAuth → deep link `sudharo://auth-callback`.
Forgot password: recovery email → OTP → new password → invalidate sessions.
