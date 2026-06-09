package io.nncdevel.sseproxy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProxyStreamingIntegrationTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static HttpServer downstream;
  private static ExecutorService downstreamExecutor;
  private static volatile String lastRequestBody;

  @LocalServerPort
  private int port;

  @BeforeAll
  static void startDownstream() throws IOException {
    downstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    downstream.createContext("/v1/responses", exchange -> {
      lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      byte[] body = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"hello\"}\n\n"
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    downstreamExecutor = Executors.newSingleThreadExecutor();
    downstream.setExecutor(downstreamExecutor);
    downstream.start();
  }

  @AfterAll
  static void stopDownstream() {
    if (downstream != null) {
      downstream.stop(0);
    }
    if (downstreamExecutor != null) {
      downstreamExecutor.shutdownNow();
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("llm.base-url", () -> "http://127.0.0.1:" + downstream.getAddress().getPort() + "/v1");
    registry.add("llm.api-key", () -> "test-key");
    registry.add("llm.model", () -> "test-model");
  }

  @Test
  void httpClientEndpointProxiesSseFromOpenAiCompatibleServer() throws Exception {
    assertProxies("/httpclient/responses");
  }

  @Test
  void restClientEndpointProxiesSseFromOpenAiCompatibleServer() throws Exception {
    assertProxies("/restclient/responses");
  }

  private void assertProxies(String path) throws Exception {
    lastRequestBody = null;
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("""
            {
              "input": "hello",
              "text": {
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
            }
            """))
        .build();

    HttpResponse<String> response = HttpClient.newHttpClient()
        .send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("content-type").orElse(""))
        .contains("text/event-stream");
    assertThat(response.body()).contains("response.output_text.delta");

    JsonNode downstreamRequest = OBJECT_MAPPER.readTree(lastRequestBody);
    assertThat(downstreamRequest.get("model").asText()).isEqualTo("test-model");
    assertThat(downstreamRequest.get("input").asText()).isEqualTo("hello");
    assertThat(downstreamRequest.get("stream").asBoolean()).isTrue();
    assertThat(downstreamRequest.get("text").get("format").get("type").asText()).isEqualTo("json_schema");
  }
}
