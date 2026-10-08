# SkillGap AI Backend

## Purpose

Spring Boot REST API for SkillGap AI, the career-readiness platform. This repository is the backend half of the unified SkillGap AI application and serves the separately hosted React frontend.

## Technology

- Java 21
- Spring Boot
- Spring Data JPA
- Spring Security
- MySQL
- Optional Gemini API summary with deterministic fallback

## Setup

Requirements: Java 21+, Maven, and MySQL. Create a database named `skillgap_ai`, then set the environment variables listed below in your shell or secret manager. The `.env.example` file is a reference only; Spring Boot does not load it automatically.

```powershell
mvn spring-boot:run
```

The health endpoint is `/api/health` on the configured server port.

## Environment Variables

- `PORT`: HTTP port; defaults to `SERVER_PORT` or `8080`.
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`: MySQL connection settings. `DB_PASSWORD` must be supplied.
- `DDL_AUTO`: Hibernate schema policy; defaults to `update`.
- `CORS_ALLOWED_ORIGINS`: comma-separated frontend origins; local default is `http://localhost:5173`.
- `GEMINI_ENABLED`: optional AI summary switch; defaults to `false`.
- `GEMINI_API_KEY`: backend-only Gemini credential. Never commit a real key or send it to the frontend.

Gemini provides only the analysis summary. Deterministic analysis remains available when Gemini is disabled or unavailable. Do not set a real key in this repository's `.env.example`.

## Build and Test

```powershell
mvn test -q
mvn clean package -DskipTests
```

The generated JAR is written under `target/`, which is excluded from Git.

## Relationship to SkillGap AI

The API implements authentication, profile/target-role selection, skill assessment and analysis, roadmap generation, progress, and interview preparation. The sibling `skillgap-ai-frontend` repository contains the React/Vite client. Both repositories are split from the SkillGap AI project; neither includes generated artifacts or real environment files.
