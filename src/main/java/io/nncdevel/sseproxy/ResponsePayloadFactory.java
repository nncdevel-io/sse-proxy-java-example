package io.nncdevel.sseproxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class ResponsePayloadFactory {

  private final ObjectMapper objectMapper;

  public ResponsePayloadFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ObjectNode create(ProxyRequest request, String defaultModel) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("model", request.modelOr(defaultModel));
    payload.set("input", request.inputOrTextNode(objectMapper));
    payload.put("stream", true);
    if (request.instructions() != null && !request.instructions().isBlank()) {
      payload.put("instructions", request.instructions());
    }
    if (request.text() != null && !request.text().isNull()) {
      payload.set("text", request.text());
    }
    return payload;
  }
}
