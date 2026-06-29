# Deploying Sprint Planning

## Architecture

| Component | Platform | Purpose |
|-----------|----------|---------|
| Backend API | [Railway](https://railway.app) | Spring Boot (Java 21) |
| Frontend UI | [Vercel](https://vercel.com) | React + Vite static site |
| Database | [MongoDB Atlas](https://www.mongodb.com/atlas) | Teams, pods, planning, releases |

Local development (`localhost:5173` + `localhost:8080`) continues to work independently.

---

## 1. MongoDB Atlas

1. Create a free **M0** cluster.
2. **Database Access** — create a user and password.
3. **Network Access** — allow `0.0.0.0/0` (required for Railway).
4. Copy the connection string:
   ```
   mongodb+srv://USER:PASSWORD@cluster0.xxxxx.mongodb.net/sprint-planning
   ```
   URL-encode special characters in the password (e.g. `@` → `%40`).

---

## 2. Railway (backend)

1. **New Project** → deploy from GitHub → select this repo.
2. Root directory: `/` (repo root).
3. Railway reads `railway.toml` and `nixpacks.toml` automatically.
4. Add **Variables**:

   | Variable | Example |
   |----------|---------|
   | `SPRING_PROFILES_ACTIVE` | `local` |
   | `MONGODB_URI` | `mongodb+srv://...` |
   | `JIRA_BASE_URL` | `https://your-org.atlassian.net` |
   | `JIRA_EMAIL` | your Atlassian email |
   | `JIRA_API_TOKEN` | Atlassian API token |

5. **Settings → Networking → Generate Domain** and copy the public URL.

### Verify

- `https://YOUR-APP.up.railway.app/actuator/health` → `{"status":"UP"}`
- `https://YOUR-APP.up.railway.app/api/v1/teams` → JSON list of teams

---

## 3. Vercel (frontend)

1. Import this repo on Vercel.
2. **Root Directory:** `frontend`
3. **Framework:** Vite (auto-detected)
4. **Before first deploy:** edit `frontend/vercel.json` and replace `YOUR-RAILWAY-APP` with your real Railway hostname:

   ```json
   "destination": "https://sprint-planning-production-xxxx.up.railway.app/api/:path*"
   ```

5. Commit, push, and deploy.

Share the **Vercel URL** with your team (not the raw Railway URL).

---

## 4. Local development

Unchanged:

```bash
# Terminal 1
./mvnw spring-boot:run

# Terminal 2
cd frontend && npm run dev
```

Open http://localhost:5173. Uses local MongoDB by default (`mongodb://localhost:27017/sprint-planning`).

To point local backend at Atlas, set `MONGODB_URI` in `.env` before starting.

---

## Security note

`SPRING_PROFILES_ACTIVE=local` disables application login. Do not expose the Railway URL publicly without an additional layer (Vercel-only access, VPN, or switch to `prod` + Microsoft SSO).
