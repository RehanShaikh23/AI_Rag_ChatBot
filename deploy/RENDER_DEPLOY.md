# 🚀 Deploy Backend to Render

Step-by-step guide to deploy the **AI RAG ChatBot Spring Boot backend** on [Render](https://render.com) using Docker.

---

## Prerequisites

- GitHub repo pushed: `https://github.com/RehanShaikh23/AI_Rag_ChatBot`
- Supabase PostgreSQL database (already configured)
- NVIDIA NIM API key

---

## Step 1 — Create a New Web Service

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **"New +"** → **"Web Service"**
3. Connect your GitHub repo: `RehanShaikh23/AI_Rag_ChatBot`
4. Configure the service:

| Setting | Value |
|---------|-------|
| **Name** | `ragchatbot-backend` |
| **Region** | Choose nearest to your Supabase region |
| **Branch** | `main` |
| **Root Directory** | `backend` |
| **Runtime** | **Docker** |
| **Instance Type** | Free (or Starter for production) |

> ⚠️ **Root Directory is critical** — set it to `backend` so Render finds the Dockerfile inside the backend folder.

---

## Step 2 — Set Environment Variables

In the Render service settings, go to **"Environment"** and add these variables:

### Required Variables

| Key | Value | Notes |
|-----|-------|-------|
| `DB_HOST` | `db.vgoyslpzebeqtvmyuspr.supabase.co` | Your Supabase host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `postgres` | Supabase default DB |
| `DB_USERNAME` | `postgres` | Supabase default user |
| `DB_PASSWORD` | `your-supabase-password` | From Supabase dashboard |
| `NVIDIA_API_KEY` | `nvapi-your-key` | From [NVIDIA NIM](https://build.nvidia.com/) |
| `JWT_SECRET` | `your-256bit-hex-secret` | Min 64 hex chars — generate below |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production profile |

### Optional Variables

| Key | Value | Notes |
|-----|-------|-------|
| `CORS_ORIGINS` | `https://ai-rag-chat-bot.vercel.app` | Your frontend URL (already set in config) |
| `SWAGGER_ENABLED` | `false` | Keep Swagger off in production |
| `PORT` | `8081` | Render uses this; matches your server.port |

### Generate a Secure JWT Secret

Run this in your terminal to generate a 256-bit hex secret:

```bash
# PowerShell
-join ((1..64) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })

# Linux/Mac
openssl rand -hex 32
```

---

## Step 3 — Configure Health Check

In the Render service settings under **"Health & Alerts"**:

| Setting | Value |
|---------|-------|
| **Health Check Path** | `/api/health` |
| **Health Check Period** | `30 seconds` |
| **Healthy Threshold** | `1` |
| **Unhealthy Threshold** | `3` |

---

## Step 4 — Configure PORT (Important!)

Render expects the app to listen on the port specified by the `PORT` environment variable. Your app is hardcoded to port `8081` in `application.yml`.

**Option A** — Set the `PORT` env var to `8081` in Render (simplest).

**Option B** — Make the port dynamic by updating `application.yml` line 2:

```yaml
server:
  port: ${PORT:8081}
```

This way Render's assigned port is used automatically, falling back to 8081 locally.

> **Recommended:** Use Option B for maximum compatibility.

---

## Step 5 — Deploy

1. Click **"Create Web Service"**
2. Render will:
   - Clone the repo
   - `cd backend` (root directory)
   - Build the Docker image (multi-stage: Maven build → JRE runtime)
   - Start the container
3. First deploy takes **5-10 minutes** (Maven downloads all dependencies)
4. Subsequent deploys are faster

---

## Step 6 — Verify Deployment

Once deployed, test these endpoints:

```bash
# Health check
curl https://ragchatbot-backend.onrender.com/api/health

# Expected response:
# {"success":true,"data":{"status":"UP","service":"AI RAG ChatBot Backend"},...}

# Actuator health
curl https://ragchatbot-backend.onrender.com/actuator/health

# Register a test user
curl -X POST https://ragchatbot-backend.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123","displayName":"Test"}'
```

---

## Step 7 — Connect Frontend

After the backend is deployed, update your frontend to point to it:

### If frontend is on Vercel:

1. Go to Vercel → Project Settings → **Environment Variables**
2. Add: `VITE_API_URL` = `https://ragchatbot-backend.onrender.com/api`
3. Redeploy the frontend

### If using `src/api/.env.production`:

```env
VITE_API_URL=https://ragchatbot-backend.onrender.com/api
```

Then rebuild and redeploy the frontend.

---

## Step 8 — Update CORS (If Needed)

The backend already allows `https://ai-rag-chat-bot.vercel.app` in CORS. If your frontend URL is different, either:

**A.** Update `application.yml` line 99 and redeploy, or

**B.** Add a `CORS_ORIGINS` env var in Render (requires making CORS configurable — already done with `${CORS_ORIGINS:...}` pattern if you updated it).

---

## Troubleshooting

### ❌ Build fails — "mvn: not found"
The Dockerfile installs Maven via `apk add --no-cache maven`. If this fails, ensure the `Root Directory` is set to `backend`.

### ❌ App crashes on startup — "DB_HOST required"
All database env vars must be set. Check **Environment** tab in Render.

### ❌ Health check fails
- Verify `PORT` env var matches `server.port` (8081)
- Health check path must be `/api/health` (not `/health`)
- Start period is 40s — allow time for Spring Boot startup

### ❌ CORS errors from frontend
- Ensure your frontend URL is in `allowed-origins` in `application.yml`
- Include the protocol (`https://`) and no trailing slash

### ❌ SSE streaming not working
Render supports SSE natively — no special config needed. If behind Cloudflare, ensure proxy buffering is disabled.

### ⚠️ Free tier cold starts
Render free tier spins down after 15 minutes of inactivity. First request after idle takes ~30-60 seconds. Upgrade to Starter ($7/mo) for always-on.

---

## Render Blueprint (Optional)

You can also create a `render.yaml` in the repo root for one-click deploy:

```yaml
services:
  - type: web
    name: ragchatbot-backend
    runtime: docker
    rootDir: backend
    dockerfilePath: ./Dockerfile
    region: oregon
    plan: free
    healthCheckPath: /api/health
    envVars:
      - key: DB_HOST
        sync: false
      - key: DB_PORT
        value: "5432"
      - key: DB_NAME
        sync: false
      - key: DB_USERNAME
        sync: false
      - key: DB_PASSWORD
        sync: false
      - key: NVIDIA_API_KEY
        sync: false
      - key: JWT_SECRET
        generateValue: true
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: PORT
        value: "8081"
```

> `sync: false` means you enter the value manually in the Render dashboard (for secrets).

---

## Architecture on Render

```
                    HTTPS
 Vercel (React) ──────────────► Render (Docker)
                                  │
                                  │  Spring Boot :8081
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
              ┌─────▼─────┐ ┌────▼────┐ ┌──────▼──────┐
              │  Supabase  │ │ NVIDIA  │ │  PGVector   │
              │ PostgreSQL │ │  NIM    │ │ (Supabase)  │
              └───────────┘ └─────────┘ └─────────────┘
```

---

> **You're ready to deploy!** Follow steps 1-6, then connect your Vercel frontend in step 7.
