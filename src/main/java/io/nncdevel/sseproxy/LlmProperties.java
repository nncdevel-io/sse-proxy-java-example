package io.nncdevel.sseproxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
    String baseUrl,
    String apiKey,
    String model) {

  public LlmProperties {
    baseUrl = normalize(defaultIfBlank(baseUrl, "http://localhost:11434/v1"));
    apiKey = defaultIfBlank(apiKey, "ollama");
    model = defaultIfBlank(model, "llama3.2");
  }

  public String responsesUrl() {
    return baseUrl + "/responses";
  }

  private static String defaultIfBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String normalize(String value) {
    String normalized = value;
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
