package io.nncdevel.sseproxy;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class ProxyController {

  private final StreamingClient openAiJavaStreamingClient;
  private final StreamingClient jdkHttpStreamingClient;
  private final StreamingClient restClientStreamingClient;

  public ProxyController(
      @Qualifier("openAiJavaStreamingClient") StreamingClient openAiJavaStreamingClient,
      @Qualifier("jdkHttpStreamingClient") StreamingClient jdkHttpStreamingClient,
      @Qualifier("restClientStreamingClient") StreamingClient restClientStreamingClient) {
    this.openAiJavaStreamingClient = openAiJavaStreamingClient;
    this.jdkHttpStreamingClient = jdkHttpStreamingClient;
    this.restClientStreamingClient = restClientStreamingClient;
  }

  @PostMapping(path = "/openai-java/responses", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> openAiJava(@RequestBody ProxyRequest request) {
    return stream(openAiJavaStreamingClient, request);
  }

  @PostMapping(path = "/httpclient/responses", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> httpClient(@RequestBody ProxyRequest request) {
    return stream(jdkHttpStreamingClient, request);
  }

  @PostMapping(path = "/restclient/responses", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> restClient(@RequestBody ProxyRequest request) {
    return stream(restClientStreamingClient, request);
  }

  private ResponseEntity<StreamingResponseBody> stream(StreamingClient client, ProxyRequest request) {
    StreamingResponseBody body = outputStream -> {
      try {
        client.stream(request, outputStream);
      } catch (IOException e) {
        throw e;
      } catch (RuntimeException e) {
        SseStreams.writeError(outputStream, e);
      }
    };
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(body);
  }
}
