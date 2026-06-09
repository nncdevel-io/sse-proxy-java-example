package io.nncdevel.sseproxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseStreamEvent;
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.stereotype.Component;

@Component("openAiJavaStreamingClient")
public class OpenAiJavaStreamingClient implements StreamingClient {

  private final OpenAIClient client;
  private final LlmProperties properties;
  private final ObjectMapper objectMapper;

  public OpenAiJavaStreamingClient(LlmProperties properties, ObjectMapper objectMapper) {
    this.client = OpenAIOkHttpClient.builder()
        .baseUrl(properties.baseUrl())
        .apiKey(properties.apiKey())
        .build();
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public void stream(ProxyRequest request, OutputStream outputStream) throws IOException {
    ResponseCreateParams.Builder builder = ResponseCreateParams.builder()
        .model(request.modelOr(properties.model()))
        .input(request.inputAsString(objectMapper))
        .store(false);
    if (request.instructions() != null && !request.instructions().isBlank()) {
      builder.instructions(request.instructions());
    }
    if (request.text() != null && !request.text().isNull()) {
      builder.putAdditionalBodyProperty("text", JsonValue.fromJsonNode(request.text()));
    }
    try (StreamResponse<ResponseStreamEvent> stream = client.responses().createStreaming(builder.build())) {
      stream.stream().forEach(event -> writeEvent(outputStream, event));
    }
  }

  private void writeEvent(OutputStream outputStream, ResponseStreamEvent event) {
    try {
      JsonValue value = event._json().orElseGet(() -> JsonValue.from(event.toString()));
      SseStreams.writeData(outputStream, objectMapper.writeValueAsString(value.convert(Object.class)));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize OpenAI SDK stream event", e);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write OpenAI SDK stream event", e);
    }
  }
}
