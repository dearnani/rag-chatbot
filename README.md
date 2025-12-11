# RAG Chatbot

This is a RAG (Retrieval-Augmented Generation) Chatbot application using Spring Boot, Qdrant (Vector DB), and Neo4j (Graph DB).

## Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven (optional, for local builds)

## Running with Docker Compose (Recommended)

The easiest way to run the application and its dependencies (Qdrant, Neo4j) is using Docker Compose.

1.  Build and start the services:
    ```bash
    docker-compose up --build -d
    ```

2.  The application will be available at `http://localhost:8080`.
    - **Qdrant**: 
        - API/Dashboard: `http://localhost:6333/dashboard`
        - REST API: `http://localhost:6333`
        - GRPC API: `localhost:6334` (if enabled/mapped)
    - **Neo4j**: `http://localhost:7474` (User: `neo4j`, Password: `password`)
    - **API Documentation**: `http://localhost:8080/swagger-ui/index.html`

3.  To stop the services:
    ```bash
    docker-compose down
    ```


## API Documentation (Swagger UI)

You can explore and test the API endpoints using the integrated Swagger UI.

1.  Navigate to `http://localhost:8080/swagger-ui/index.html` in your browser.
2.  You will see the **RAG Bot API**.
3.  **To test the Query endpoint**:
    - Expand the `POST /api/rag/query` section.
    - Click **Try it out**.
    - In the Request body, enter your query:
      ```json
      {
        "query": "What are the latest financial trends?"
      }
      ```
    - Click **Execute** to send the request and view the response.
4.  **To index documents**:
    - Expand the `POST /api/rag/index` section.
    - Click **Try it out**.
    - Enter the list of texts to index:
      ```json
      {
        "texts": ["Financial market data snippet 1...", "Another snippet..."]
      }
      ```
    - Click **Execute**.

## Local Development

If you want to run the application locally without Docker for the app itself:

1.  Start the dependencies (Qdrant & Neo4j):
    ```bash
    docker-compose up -d qdrant neo4j
    ```

2.  Build the application:
    ```bash
    mvn clean install
    ```

3.  Run the application:
    ```bash
    java -jar target/*.jar
    ```
    *Note: You may need to configure environment variables in `application.yml` or pass them as VM arguments if the defaults don't match your local setup.*

## Building the Docker Image Manually

To build the Docker image using the multi-stage build:

```bash
docker build -t rag-chatbot .
```

To run the container standalone (ensure dependencies are reachable, e.g., via Docker network or host IP):

```bash
docker run -p 8080:8080 rag-chatbot
```

## Langfuse Observability

This project includes a self-hosted Langfuse stack for LLM observability and tracing.

### Setup & Access

1.  **Start the Stack**:
    When you run `docker-compose up --build -d`, the Langfuse services (Server, Worker, Postgres, ClickHouse, Redis) are automatically started.

2.  **Access Dashboard**:
    - URL: `http://localhost:3000`
    - Email: `admin@langfuse.com`
    - Password: `password`

    *Note: The project, organization, and API keys are auto-provisioned on startup via `LANGFUSE_INIT_*` environment variables.*

### Verification

To verify that tracing is working:

1.  **Send a Chat Query**:
    Use Swagger UI (`http://localhost:8080/swagger-ui/index.html`) or `curl` to send a request:
    ```bash
    curl -X POST "http://localhost:8080/api/rag/query" \
         -H "Content-Type: application/json" \
         -d '{"query": "Test query for Langfuse"}'
    ```

2.  **Check Traces**:
    - Log in to the Langfuse Dashboard (`http://localhost:3000`).
    - Navigate to the **"MyProject"** project.
    - Go to **Traces**. You should see a new trace for `deepseek-generation` corresponding to your request.
