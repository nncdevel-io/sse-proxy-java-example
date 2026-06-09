package io.nncdevel.sseproxy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ResponsePayloadFactoryTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ResponsePayloadFactory factory = new ResponsePayloadFactory(objectMapper);

  @Test
  void createsOpenAiCompatibleStreamingPayload() throws Exception {
    JsonNode input = objectMapper.readTree("\"hello\"");
    JsonNode text = objectMapper.readTree("""
        {
          "format": {
            "type": "json_schema",
            "name": "answer",
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
        """);
    ProxyRequest request = new ProxyRequest(null, input, "reply in JSON", text);

    JsonNode payload = factory.create(request, "llama3.2");

    assertThat(payload.get("model").asText()).isEqualTo("llama3.2");
    assertThat(payload.get("input").asText()).isEqualTo("hello");
    assertThat(payload.get("instructions").asText()).isEqualTo("reply in JSON");
    assertThat(payload.get("stream").asBoolean()).isTrue();
    assertThat(payload.get("text").get("format").get("type").asText()).isEqualTo("json_schema");
  }
}
