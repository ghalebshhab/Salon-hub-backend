# SalonHub version 1: first production deploy

Follow the steps in order. The values are in `production-environment.md`.

## What changes in the database

Flyway runs when the backend starts:

| Migration | Change | Safe for the old code? |
|---|---|---|
| V1 baseline | Nothing on an existing database. Flyway records it as the starting point (`baseline-on-migrate`). On an empty database it creates all 36 tables. | — |
| V2 auth sessions | Adds the `auth_sessions` table. | Yes, the old code does not use it. |
| V3 Arabic content | Adds `salon_service.name_ar`, `salon_service.description_ar`, `salon.description_ar`. | Yes, extra columns do not break `validate`. |

All changes **add** things. Nothing is deleted. Because of this, you can roll
back the code without restoring the database (see step 6).

## Step 1: Back up the production database

Make the backup **before** you push anything.

**A. Use the database host's backup** (best): open the MySQL host's dashboard
and make a manual backup or snapshot. Write down its name and time.

**B. Or dump it from your computer** with the MySQL client that is installed
with MySQL Server 9.6. The command asks for the password, so the password
does not stay in the shell history:

```bash
"/c/Program Files/MySQL/MySQL Server 9.6/bin/mysqldump.exe" --host=<db-host> --port=<db-port> --user=<db-user> -p --single-transaction --routines --triggers --set-gtid-purged=OFF --ssl-mode=REQUIRED <db-name> > salonhub-prod-backup-2026-11-22.sql
```

Check the file: it must be larger than a few KB and end with `-- Dump completed`.
Keep it outside the git folders, for example in `Documents\SalonHub backups`.

To restore it, if you must:

```bash
"/c/Program Files/MySQL/MySQL Server 9.6/bin/mysql.exe" --host=<db-host> --port=<db-port> --user=<db-user> -p <db-name> < salonhub-prod-backup-2026-11-22.sql
```

## Step 2: Check the production schema

Run these on the production database (MySQL Workbench or the `mysql` client):

```sql
SHOW TABLES LIKE 'flyway_schema_history';
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE();
```

- No `flyway_schema_history` table: normal for the first Flyway run. Flyway
  marks the database as version 1 and then runs V2 and V3.
- Fewer than about 36 tables: production is older than the V1 baseline. Set
  `JPA_DDL_AUTO=update` for this one deploy so Hibernate adds what is missing.

## Step 3: Set the values on Render, Vercel and the dashboard host

Set every value in `production-environment.md` **before** the push, because
Render starts the deploy when the push arrives. The most important ones:

- Render: new `JWT_SECRET`, new `AUTH_CODE_SECRET`, the `MAIL_*` values,
  `ANTHROPIC_API_KEY`, `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`.
- Vercel: delete `VITE_API_BASE_URL`.
- Dashboard host: `BACKEND_API_URL`.

## Step 4: Deploy in this order

The new website and the new backend sign in with cookies; the old ones used a
token in the page. Mixing an old and a new part breaks sign-in, so deploy them
close together:

1. **Backend** (push `master`). Wait until Render shows the deploy as *Live*.
2. **Frontend** (push `main`). Wait until Vercel shows *Ready*.
3. **Admin dashboard** (push its branch).

Everyone is signed out one time because of the new `JWT_SECRET`. That is expected.

## Step 5: Check the deploy

In the Render log, look for:

- `Successfully applied 2 migrations` (or `Schema ... is up to date`).
- `Email: signed in to smtp.gmail.com:465 as salonhub.jo@gmail.com`.
- `Translating N services and M salon descriptions to Arabic` (only with the Claude key).
- No `Schema-validation` error, and no `Application run failed`.

Then, by hand on the live site:

1. Sign up with an email address you can read; the code arrives.
2. Sign in, reload the page (you stay signed in), sign out.
3. Book a visit as a customer; accept it as the salon owner.
4. Switch to Arabic; open a salon page; the services and the emblem show Arabic.
5. Sign in to the admin dashboard and open the salons list.

If you set `JPA_DDL_AUTO=update` in step 2, set it back to `validate` now and
deploy the backend again.

## Step 6: If something goes wrong

| Problem | What to do |
|---|---|
| Backend does not start | Read the first error in the Render log. Fix the value, or in Render choose **Rollback** to the last good deploy. The database can stay as it is. |
| `Schema-validation: missing table/column` | Set `JPA_DDL_AUTO=update`, deploy again, then set it back to `validate`. |
| `Email: cannot sign in` | Check `MAIL_PASSWORD` (App Password, no spaces). If it is a timeout, Render blocks SMTP on this plan. |
| Sign-in fails on the site | Check that Vercel has no `VITE_API_BASE_URL`, and that `CORS_ALLOWED_ORIGINS` has the site address. |
| Data is wrong or lost | Stop and restore the backup from step 1. |
