package io.nncdevel.sseproxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record ProxyRequest(
    String model,
    JsonNode input,
    String instructions,
    JsonNode text) {

  public String modelOr(String defaultModel) {
    return model == null || model.isBlank() ? defaultModel : model;
  }

  public JsonNode inputOrTextNode(ObjectMapper objectMapper) {
    return input == null || input.isNull() ? objectMapper.getNodeFactory().textNode("") : input;
  }

  public String inputAsString(ObjectMapper objectMapper) {
    JsonNode value = inputOrTextNode(objectMapper);
    if (value.isTextual()) {
      return value.asText();
    }
    return value.toString();
  }
}
