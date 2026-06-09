package io.nncdevel.sseproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component("jdkHttpStreamingClient")
public class JdkHttpStreamingClient implements StreamingClient {

  private final HttpClient httpClient;
  private final LlmProperties properties;
  private final ResponsePayloadFactory payloadFactory;
  private final ObjectMapper objectMapper;

  public JdkHttpStreamingClient(
      LlmProperties properties,
      ResponsePayloadFactory payloadFactory,
      ObjectMapper objectMapper) {
    this.httpClient = HttpClient.newHttpClient();
    this.properties = properties;
    this.payloadFactory = payloadFactory;
    this.objectMapper = objectMapper;
  }

  @Override
  public void stream(ProxyRequest request, OutputStream outputStream) throws IOException {
    String body = objectMapper.writeValueAsString(payloadFactory.create(request, properties.model()));
    HttpRequest httpRequest = HttpRequest.newBuilder()
        .uri(URI.create(properties.responsesUrl()))
        .header("Authorization", "Bearer " + properties.apiKey())
        .header("Content-Type", "application/json")
        .header("Accept", "text/event-stream")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    try {
      HttpResponse<java.io.InputStream> response = httpClient.send(
          httpRequest, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        SseStreams.writeError(outputStream, new IllegalStateException("LLM returned HTTP " + response.statusCode()));
        return;
      }
      SseStreams.copy(response.body(), outputStream);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while streaming LLM response", e);
    }
  }
}
