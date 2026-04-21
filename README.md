# AI Agent Application

A Spring Boot agent platform that uses Ollama for local LLM inference, PostgreSQL + pgvector for durable memory, and a read-only MCP server for database access.

## Overview

This project runs an agentic loop:
- the LLM decides whether to call a tool or answer
- tools execute work such as database lookup, email, or logging
- observations are saved as task history and semantic memory
- the agent iterates until it can return a final response

The database path is intentionally constrained:
- the LLM produces structured database intent, not raw SQL
- the application converts that intent into precise SQL
- the PostgreSQL MCP server executes read-only queries only
- sensitive fields such as `password` are blocked and redacted

## Current Architecture

### Main application
- Spring Boot API and UI host
- agent execution loop
- task persistence
- pgvector-backed semantic memory
- MCP client that launches the database server jar over STDIO

### Embedded MCP database integration
- standalone Maven build under [`postgres-readonly-mcp-server`](/Users/amarupadhyaya/Downloads/research_agent-main/postgres-readonly-mcp-server)
- exposes read-only PostgreSQL tools:
  - `postgres_health_check`
  - `list_tables`
  - `describe_table`
  - `query_readonly`

### Vector memory
- task state is stored durably in PostgreSQL
- step content is embedded and stored in pgvector
- relevant prior memories are retrieved before the agent decides
- if the embedding model is unavailable, the app degrades gracefully instead of crashing

## Repository Structure

```text
research_agent-main/
├── pom.xml
├── docker-compose.yml
├── docker/
│   └── postgres/
│       └── init/
│           └── 01-enable-pgvector.sql
├── src/
│   └── main/
│       ├── java/com/researchagent/
│       └── resources/application.yml
├── postgres-readonly-mcp-server/
│   ├── pom.xml
│   └── src/main/java/com/researchagent/mcp/postgres/
└── ai-chatbot-landing.html
```

## Requirements

- Java 25 for the main application build
- Maven 3.9+
- Docker
- Ollama running locally

Recommended Ollama models:
- chat model: `gemma3:4b`
- embedding model: `nomic-embed-text`

Pull them if needed:

```bash
ollama pull gemma3:4b
ollama pull nomic-embed-text
```

## Run pgvector with Docker Compose

The repository already includes a ready-to-run pgvector setup in [`docker-compose.yml`](/Users/amarupadhyaya/Downloads/research_agent-main/docker-compose.yml).

Start it with:

```bash
docker compose up -d
```

What it does:
- starts PostgreSQL with pgvector image `pgvector/pgvector:pg18-trixie`
- exposes the database on `localhost:5433`
- creates database `mydb`
- uses default credentials `postgres/postgres`
- auto-runs [`01-enable-pgvector.sql`](/Users/amarupadhyaya/Downloads/research_agent-main/docker/postgres/init/01-enable-pgvector.sql) to enable the `vector` extension

Default connection:
- JDBC URL: `jdbc:postgresql://localhost:5433/mydb`
- Username: `postgres`
- Password: `postgres`

Stop it with:

```bash
docker compose down
```

Remove volume data too:

```bash
docker compose down -v
```

## Configuration

Current defaults live in [`application.yml`](/Users/amarupadhyaya/Downloads/research_agent-main/src/main/resources/application.yml).

### Database
- `DB_URL=jdbc:postgresql://localhost:5433/mydb`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=postgres`

### Ollama
- `OLLAMA_BASE_URL=http://127.0.0.1:11434`
- `OLLAMA_MODEL=gemma3:4b`
- `OLLAMA_EMBEDDING_MODEL=nomic-embed-text`

### Vector memory
- `AGENT_MEMORY_EMBEDDING_DIMENSIONS=768`

### MCP database server
- `AGENT_DB_MCP_JAR_PATH=postgres-readonly-mcp-server/target/postgres-readonly-mcp-server-1.0.0.jar`
- `AGENT_DB_MCP_DB_URL=jdbc:postgresql://localhost:5433/mydb`
- `AGENT_DB_MCP_DB_USERNAME=postgres`
- `AGENT_DB_MCP_DB_PASSWORD=postgres`
- `AGENT_DB_MCP_ALLOWED_SCHEMAS=public`
- `AGENT_DB_MCP_MAX_ROWS=20`

## Build

Run:

```bash
mvn clean install
```

This now builds both:
- the main Spring Boot application
- the standalone PostgreSQL MCP server jar

Artifacts:
- main app: [`target/ai-agent-app-1.0-SNAPSHOT.jar`](/Users/amarupadhyaya/Downloads/research_agent-main/target/ai-agent-app-1.0-SNAPSHOT.jar)
- MCP server: [`postgres-readonly-mcp-server/target/postgres-readonly-mcp-server-1.0.0.jar`](/Users/amarupadhyaya/Downloads/research_agent-main/postgres-readonly-mcp-server/target/postgres-readonly-mcp-server-1.0.0.jar)

## Run the Application

1. Start PostgreSQL + pgvector:

```bash
docker compose up -d
```

2. Make sure Ollama is running and the required models are pulled.

3. Start the Spring app:

```bash
mvn spring-boot:run
```

If port `8080` is already in use:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## How Database Access Works

The database tool no longer asks the LLM to generate SQL directly.

Flow:
1. The LLM returns structured intent such as entity, operation, columns, filters, and limit.
2. The app validates that intent against the discovered schema.
3. The app removes or blocks sensitive fields.
4. The app builds SQL from validated intent.
5. The MCP server executes the query in read-only mode.
6. Only relevant, non-sensitive rows are returned to the agent.

### Important safety rules
- no write queries
- no multi-statement SQL
- no session-changing SQL
- no privileged file helpers
- no password/token/secret/api key fields returned
- list queries prefer explicit minimal columns
- result size is capped

## pgvector Memory Implementation

Agent memory is implemented with PostgreSQL plus embeddings.

### What is stored
- task metadata
- task steps
- vectorized memory entries derived from step content

### How retrieval works
- before each decision, the executor searches semantically similar prior memories
- the top matches are added to the prompt as context
- this gives the agent continuity across tasks beyond just the current request

### Failure handling
- if pgvector is unavailable, memory initialization fails
- if the embedding model is unavailable in Ollama, semantic retrieval is skipped safely and the app continues running

## API Endpoints

Base URL:

```text
http://localhost:8080/api/agent
```

Available endpoints:
- `GET /api/agent/health`
- `POST /api/agent/tasks`
- `GET /api/agent/tasks/{taskId}`
- `GET /api/agent/tasks/{taskId}/steps`

Example task request:

```json
{
  "task": "List all users"
}
```

## Authentication

JWT auth is configured in the application and used by the frontend experience.

Relevant properties:
- `JWT_SECRET`
- `JWT_EXPIRATION_MILLIS`

## Frontend

[`ai-chatbot-landing.html`](/Users/amarupadhyaya/Downloads/research_agent-main/ai-chatbot-landing.html) is the current chat-style landing page and operator UI. It includes:
- login/signup/logout flows
- task composer
- task trace rendering
- animated progress UI
- theme switching

## Troubleshooting

### `extension "vector" is not available`
Use the included Docker Compose setup instead of a non-pgvector local PostgreSQL installation.

### `model "nomic-embed-text" not found`
Pull the embedding model:

```bash
ollama pull nomic-embed-text
```

### Port `8080` already in use
Start on another port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### MCP jar not found
Build the project first:

```bash
mvn clean install
```

## Notes

- The root Maven build triggers the MCP server build automatically.
- The database tool is tuned to retrieve only relevant data.
- Sensitive fields are blocked at both intent-validation and MCP-response layers.
