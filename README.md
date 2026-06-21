# 🤖 AI RAG ChatBot

A production-ready full-stack **Retrieval-Augmented Generation (RAG)** chatbot powered by **Spring AI**, **NVIDIA NIM**, **PGVector**, and **React**.

Upload documents and chat with an AI that uses your content as context — with real-time **Server-Sent Events (SSE)** streaming, **JWT authentication**, and a modern responsive UI.

---

## ✨ Features

- **RAG Pipeline** — Upload PDFs, DOCX, TXT files → automatic chunking → vector embeddings → similarity search during chat
- **SSE Streaming** — Token-by-token AI responses with real-time streaming
- **JWT Authentication** — Secure registration/login with BCrypt password hashing
- **Conversation Management** — Multi-conversation support with auto-generated titles
- **Per-User Rate Limiting** — Bucket4j token-bucket algorithm on chat and upload endpoints
- **Async Document Processing** — Non-blocking document ingestion with status polling
- **NVIDIA NIM Integration** — Uses `meta/llama-3.1-8b-instruct` for chat and `nvidia/nv-embedqa-e5-v5` for embeddings
- **Swagger/OpenAPI** — Full API documentation with JWT bearer auth support

---

## 🏗️ Architecture

```
┌─────────────────┐    SSE/REST     ┌──────────────────┐
│  React Frontend │ ◄────────────► │  Spring Boot API  │
│  (Vite + React) │                 │  (Port 8081)      │
└─────────────────┘                 └────────┬─────────┘
                                             │
                              ┌──────────────┼──────────────┐
                              │              │              │
                         ┌────▼────┐   ┌─────▼─────┐  ┌────▼────┐
                         │ PGVector │   │ NVIDIA NIM│  │  JWT    │
                         │ (Vectors)│   │ (AI/LLM)  │  │ (Auth)  │
                         └────┬────┘   └───────────┘  └─────────┘
                              │
                    ┌─────────▼─────────┐
                    │   PostgreSQL DB    │
                    │   (Supabase)       │
                    └───────────────────┘
```

---

## 🛠️ Tech Stack

| Layer     | Technology                                      |
|-----------|------------------------------------------------|
| Frontend  | React 19, Vite, CSS3, Material Symbols          |
| Backend   | Spring Boot 3.4, Spring AI 1.0, Java 21         |
| AI/LLM    | NVIDIA NIM (OpenAI-compatible), Llama 3.1 8B    |
| Embeddings| nvidia/nv-embedqa-e5-v5 (1024-dim, asymmetric)  |
| Vector DB | PGVector (PostgreSQL extension)                  |
| Database  | PostgreSQL 16 (Supabase)                         |
| Auth      | JWT (JJWT 0.12), BCrypt                          |
| Docs      | SpringDoc OpenAPI 2.7                            |

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Node.js 20+
- PostgreSQL 16 with PGVector extension (or Docker)
- NVIDIA NIM API key ([get one here](https://build.nvidia.com/))

### 1. Clone & Setup Environment

```bash
git clone https://github.com/RehanShaikh23/AI_Rag_ChatBot.git
cd AI_Rag_ChatBot
```

Create `backend/.env`:

```env
DB_HOST=your-db-host
DB_PORT=5432
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=your-db-password
NVIDIA_API_KEY=nvapi-your-key-here
JWT_SECRET=your-256bit-hex-secret
```

### 2. Start the Database (Docker)

```bash
cd backend
docker compose up -d
```

### 3. Run the Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts at `http://localhost:8081`.

### 4. Run the Frontend

```bash
npm install
npm run dev
```

The UI starts at `http://localhost:5173`.

---

## 📁 Project Structure

```
AI_Rag_ChatBot/
├── backend/                     # Spring Boot API
│   ├── src/main/java/com/ragchatbot/
│   │   ├── ai/                  # RAG pipeline, embeddings, AI services
│   │   ├── config/              # Security, CORS, async, rate limiting
│   │   ├── controller/          # REST endpoints (chat, auth, documents)
│   │   ├── dto/                 # Request/response DTOs
│   │   ├── entity/              # JPA entities (User, Conversation, etc.)
│   │   ├── exception/           # Global exception handler
│   │   ├── repository/          # Spring Data JPA repositories
│   │   ├── security/            # JWT filter, token provider
│   │   └── service/             # Business logic layer
│   ├── src/main/resources/
│   │   ├── application.yml      # Main config
│   │   ├── application-dev.yml  # H2 dev profile
│   │   └── application-prod.yml # Production profile
│   ├── Dockerfile               # Multi-stage Docker build
│   └── docker-compose.yml       # Local PGVector setup
├── src/                         # React frontend
│   ├── api/                     # API client, chat, auth modules
│   ├── components/              # UI components (Sidebar, ChatInput, etc.)
│   └── contexts/                # React context (AuthContext)
├── deploy/
│   └── nginx.conf               # Production reverse proxy config
└── .github/workflows/
    └── ci.yml                   # GitHub Actions CI/CD
```

---

## 🔌 API Endpoints

| Method | Endpoint                  | Auth | Description                    |
|--------|--------------------------|------|--------------------------------|
| POST   | `/api/auth/register`      | ❌   | Register a new user            |
| POST   | `/api/auth/login`         | ❌   | Login and get JWT token        |
| GET    | `/api/auth/me`            | ✅   | Get current user profile       |
| POST   | `/api/chat`               | ✅   | Send message (sync response)   |
| POST   | `/api/chat/stream`        | ✅   | Send message (SSE streaming)   |
| GET    | `/api/chat/conversations` | ✅   | List user's conversations      |
| GET    | `/api/chat/{id}/messages` | ✅   | Get messages in a conversation |
| DELETE | `/api/chat/{id}`          | ✅   | Delete a conversation          |
| POST   | `/api/documents/upload`   | ✅   | Upload document for RAG        |
| GET    | `/api/documents`          | ✅   | List uploaded documents        |
| DELETE | `/api/documents/{id}`     | ✅   | Delete a document              |
| GET    | `/api/health`             | ❌   | Health check                   |

---

## 🌐 Deployment

### Production Profile

```bash
java -jar app.jar --spring.profiles.active=prod
```

### Docker

```bash
cd backend
docker build -t ragchatbot-backend .
docker run -p 8081:8081 \
  -e DB_HOST=your-host \
  -e DB_PASSWORD=your-pass \
  -e NVIDIA_API_KEY=your-key \
  -e JWT_SECRET=your-secret \
  -e SPRING_PROFILES_ACTIVE=prod \
  ragchatbot-backend
```

### Environment Variables

| Variable         | Required | Description                        |
|-----------------|----------|------------------------------------|
| `DB_HOST`       | ✅       | PostgreSQL host                    |
| `DB_PORT`       | ❌       | PostgreSQL port (default: 5432)    |
| `DB_NAME`       | ✅       | Database name                      |
| `DB_USERNAME`   | ✅       | Database user                      |
| `DB_PASSWORD`   | ✅       | Database password                  |
| `NVIDIA_API_KEY`| ✅       | NVIDIA NIM API key                 |
| `JWT_SECRET`    | ✅       | 256-bit hex secret for JWT signing |
| `CORS_ORIGINS`  | ❌       | Comma-separated allowed origins    |
| `SWAGGER_ENABLED`| ❌      | Enable Swagger UI (default: false) |
| `VITE_API_URL`  | ❌       | Frontend API base URL              |

---

## 🧪 Running Tests

```bash
cd backend
mvn test
```

---

## 📄 License

This project is for educational and portfolio purposes.
