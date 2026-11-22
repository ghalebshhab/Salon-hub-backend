# SalonHub production environment values

This file lists every value that the three SalonHub apps read in production.
It holds **no secrets**. Put the real values only in the host's settings
(Render, Vercel) or in the git-ignored `backend/.env` on your computer.

- `[secret]`: never commit it, never paste it in chat, and change it if it leaks.
- `[set]`: you must set it; the app has no safe default.
- `[default]`: you can leave it out; the default is shown.

## 1. Backend on Render (`salon-hub-backend.onrender.com`)

### Database

| Name | Value | Kind |
|---|---|---|
| `DB_URL` | `jdbc:mysql://<host>:<port>/<database>?useSSL=true&serverTimezone=UTC` | [set] |
| `DB_USERNAME` | the production database user | [set] |
| `DB_PASSWORD` | the production database password | [secret] |
| `JPA_DDL_AUTO` | `validate`. Use `update` only for one start if Flyway reports a missing table; then set it back to `validate`. | [default] |

Flyway runs the files in `src/main/resources/db/migration` (V2 auth sessions,
V3 Arabic content) when the app starts. **Back up the database first.**

### Sign-in security

| Name | Value | Kind |
|---|---|---|
| `JWT_SECRET` | a new random Base64 value, 32 bytes or more (command below) | [secret] |
| `AUTH_CODE_SECRET` | a second, different random value, 32 characters or more | [secret] |
| `AUTH_COOKIE_SECURE` | `true` | [default] |
| `AUTH_COOKIE_SAME_SITE` | `Strict`. This works because Vercel forwards `/api` (same site). | [default] |
| `AUTH_ACCESS_TTL` | `PT15M` | [default] |
| `AUTH_REFRESH_TTL` | `P14D` | [default] |
| `AUTH_ADMIN_TTL` | `PT8H`. Keep it equal to the dashboard cookie age. | [default] |
| `CORS_ALLOWED_ORIGINS` | `https://<your-vercel-domain>` and the admin dashboard address, comma-separated | [set] |
| `FRONTEND_URL` | `https://<your-vercel-domain>`, used for links in emails | [set] |

Make a new secret (run it two times, one value for each secret):

```bash
openssl rand -base64 48
```

A new `JWT_SECRET` signs out every user one time. That is expected.

### Email (Gmail account `salonhub.jo@gmail.com`)

| Name | Value | Kind |
|---|---|---|
| `MAIL_DELIVERY` | `smtp` | [set] |
| `MAIL_HOST` | `smtp.gmail.com` | [default] |
| `MAIL_PORT` | `465` | [set] |
| `MAIL_SSL_ENABLED` | `true` | [set] |
| `MAIL_STARTTLS` | `false` | [set] |
| `MAIL_USERNAME` | `salonhub.jo@gmail.com` | [set] |
| `MAIL_PASSWORD` | the 16-letter Gmail **App Password**, without spaces | [secret] |
| `MAIL_FROM` | `salonhub.jo@gmail.com` | [set] |
| `MAIL_FROM_NAME` | `SalonHub` | [default] |
| `MAIL_LOGIN_ALERTS` | `true` | [default] |

Port 465 (SSL) is used because port 587 was blocked on the development
network; Gmail accepts both. After a deploy, the log shows one of:

- `Email: signed in to smtp.gmail.com:465 as salonhub.jo@gmail.com`: email works.
- `Email: cannot sign in ...`: wrong App Password, or the host blocks SMTP
  ports. Render's free plan may block outgoing SMTP; then move to a paid
  instance or to an email API service.

Gmail sends about 500 emails a day from a normal account.

### Media, maps, AI, payments

| Name | Value | Kind |
|---|---|---|
| `CLOUDINARY_CLOUD_NAME` | from the Cloudinary dashboard | [set] |
| `CLOUDINARY_API_KEY` | from the Cloudinary dashboard | [secret] |
| `CLOUDINARY_API_SECRET` | from the Cloudinary dashboard | [secret] |
| `GOOGLE_MAPS_API_KEY` | server key for travel times (Routes API). Optional: without it the app estimates travel time from the distance. | [secret] |
| `ANTHROPIC_API_KEY` | Claude key for Lamsa and the Arabic translation | [secret] |
| `ASSISTANT_MODEL` | `claude-opus-5-5` | [default] |
| `PAYMENT_COMMISSION_BPS` | `0` | [default] |
| `BILLING_CLIQ_ALIAS` | the CliQ alias that salons pay plans to | [set] |
| `BILLING_CLIQ_NAME` | `SalonHub` | [default] |
| `BILLING_SUPPORT_PHONE` | the support phone shown on plan payments | [set] |

Do not set `spring.profiles.active=local` on Render.

## 2. Frontend on Vercel

| Name | Value | Kind |
|---|---|---|
| `VITE_API_BASE_URL` | **remove it** from the Vercel settings. `.env.production` already sets `/api`. | — |
| `VITE_GOOGLE_MAPS_API_KEY` | browser key for the map picker, limited to your Vercel domain | [set] |

`vercel.json` forwards `/api/*` to `https://salon-hub-backend.onrender.com`.
If the backend address changes, change it there.

## 3. Admin dashboard

| Name | Value | Kind |
|---|---|---|
| `BACKEND_API_URL` | `https://salon-hub-backend.onrender.com` | [set] |
| `NODE_ENV` | `production` (the host sets it) | [default] |

## 4. Check after the deploy

1. Render log: `Successfully applied` for the Flyway migrations, and the
   `Email: signed in` line.
2. Sign up with an email address you can read; the code must arrive.
3. Sign in and sign out on the website and on the admin dashboard.
4. Switch the site to Arabic and open a salon page: the services show in
   Arabic a short time after the start (the translation runs in the background).
