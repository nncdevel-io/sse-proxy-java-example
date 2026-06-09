package io.nncdevel.sseproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("restClientStreamingClient")
public class RestClientStreamingClient implements StreamingClient {

  private final RestClient restClient;
  private final LlmProperties properties;
  private final ResponsePayloadFactory payloadFactory;
  private final ObjectMapper objectMapper;

  public RestClientStreamingClient(
      RestClient.Builder restClientBuilder,
      LlmProperties properties,
      ResponsePayloadFactory payloadFactory,
      ObjectMapper objectMapper) {
    this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    this.properties = properties;
    this.payloadFactory = payloadFactory;
    this.objectMapper = objectMapper;
  }

  @Override
  public void stream(ProxyRequest request, OutputStream outputStream) throws IOException {
    String body = objectMapper.writeValueAsString(payloadFactory.create(request, properties.model()));
    restClient.post()
        .uri("/responses")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .header("Authorization", "Bearer " + properties.apiKey())
        .body(body)
        .exchange((httpRequest, response) -> {
          if (!response.getStatusCode().is2xxSuccessful()) {
            SseStreams.writeError(outputStream,
                new IllegalStateException("LLM returned HTTP " + response.getStatusCode().value()));
            return null;
          }
          SseStreams.copy(response.getBody(), outputStream);
          return null;
        });
  }
}
