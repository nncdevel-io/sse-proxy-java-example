# sse-proxy-java-example

Java example of an SSE proxy that relays OpenAI streaming responses to clients.

## Overview

This project runs one Spring Boot application with three SSE proxy endpoints.
All endpoints accept the same request shape and return `text/event-stream`.
They differ only in the client used to call an OpenAI-compatible Responses API.

| Endpoint | Downstream client |
| --- | --- |
| `/openai-java/responses` | Official `openai-java` SDK |
| `/httpclient/responses` | Java `java.net.http.HttpClient` |
| `/restclient/responses` | Spring `RestClient` |

The default downstream target is local Ollama's OpenAI-compatible API:
`http://localhost:11434/v1`.

## Requirements

- Java 21 or later
- Maven 3.9 or later
- Optional: Docker
- Optional: Ollama or another OpenAI-compatible Responses API server

## Configuration

The application reads these environment variables.

| Variable | Default | Description |
| --- | --- | --- |
| `LLM_BASE_URL` | `http://localhost:11434/v1` | OpenAI-compatible API base URL |
| `LLM_API_KEY` | `ollama` | Bearer token sent to the downstream API |
| `LLM_MODEL` | `llama3.2` | Default model when the request omits `model` |

Spring Boot virtual threads are enabled in `application.yml`.

## Run Locally

```bash
mvn spring-boot:run
```

To use the OpenAI API instead of Ollama:

```bash
LLM_BASE_URL=https://api.openai.com/v1 \
LLM_API_KEY="$OPENAI_API_KEY" \
LLM_MODEL=gpt-5.2 \
mvn spring-boot:run
```

## Run With Docker

```bash
mvn package
docker build -t sse-proxy-java-example .
docker run --rm -p 8080:8080 \
  -e LLM_BASE_URL=http://host.docker.internal:11434/v1 \
  -e LLM_API_KEY=ollama \
  -e LLM_MODEL=llama3.2 \
  sse-proxy-java-example
```

## Verify Proxy Behavior

Start Ollama and pull a model:

```bash
ollama pull llama3.2
```

Run the same request against each endpoint. Use `curl -N` so curl does not
buffer the SSE stream.

```bash
curl -N http://localhost:8080/openai-java/responses \
  -H 'Content-Type: application/json' \
  -d '{"input":"Write one short sentence about SSE."}'
```

```bash
curl -N http://localhost:8080/httpclient/responses \
  -H 'Content-Type: application/json' \
  -d '{"input":"Write one short sentence about SSE."}'
```

```bash
curl -N http://localhost:8080/restclient/responses \
  -H 'Content-Type: application/json' \
  -d '{"input":"Write one short sentence about SSE."}'
```

Each command should print incremental SSE events. For the `HttpClient` and
`RestClient` endpoints, downstream events are passed through as received. The
`openai-java` endpoint receives typed SDK stream events and emits them as SSE
`data:` events.

## Structured Outputs

Pass a `text` object to forward a Responses API Structured Outputs request.

```bash
curl -N http://localhost:8080/restclient/responses \
  -H 'Content-Type: application/json' \
  -d '{
    "input": "Return a greeting.",
    "text": {
      "format": {
        "type": "json_schema",
        "name": "greeting",
        "schema": {
          "type": "object",
          "properties": {
            "message": { "type": "string" }
          },
          "required": ["message"],
          "additionalProperties": false
        },
        "strict": true
      }
    }
  }'
```

## Test

```bash
mvn test
```
