# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot SSE proxy example. Application code lives in
`src/main/java/io/nncdevel/sseproxy`, with `SseProxyApplication` as the entry
point and proxy/client classes grouped in the same package. Runtime configuration
is in `src/main/resources/application.yml`. Tests live in
`src/test/java/io/nncdevel/sseproxy`. The root also contains `pom.xml`,
`Dockerfile`, `README.md`, and `tasks.md`.

## Build, Test, and Development Commands

- `mvn test`: runs the Spring Boot/JUnit test suite.
- `mvn spring-boot:run`: starts the proxy locally on the default Spring Boot
  port.
- `mvn package`: builds the application JAR before Docker packaging.
- `docker build -t sse-proxy-java-example .`: builds the container image.

Use environment variables to target a downstream OpenAI-compatible API:
`LLM_BASE_URL`, `LLM_API_KEY`, and `LLM_MODEL`.

## Coding Style & Naming Conventions

Use Java 21 and Spring Boot conventions already present in the codebase. Keep
Java indentation at two spaces, put classes in the `io.nncdevel.sseproxy`
package, and use descriptive class names such as `ProxyController` or
`JdkHttpStreamingClient`. Test classes should mirror the subject under test and
end with `Test`. Keep Markdown compatible with `.markdownlint-cli2.jsonc`.

## Testing Guidelines

Tests use JUnit 5 with Spring Boot test support. Prefer focused unit tests for
pure behavior, `@WebMvcTest` for controller endpoints, and integration tests
when streaming behavior crosses component boundaries. Name test methods after
the behavior being verified, for example `restClientEndpointReturnsEventStream`.
Run `mvn test` before submitting changes.

## Commit & Pull Request Guidelines

Recent commits use short, imperative summaries such as
`Add initial implementation of SSE proxy with OpenAI integration and Docker support`.
Keep commits focused and mention the affected area when useful. Pull requests
should describe the change, list verification performed, link related issues
when applicable, and include request/response examples if endpoint behavior
changes.

## Security & Configuration Tips

Do not commit real API keys. Use environment variables for credentials and local
model settings. When changing defaults in `application.yml`, update `README.md`
so local and Docker instructions stay accurate.
