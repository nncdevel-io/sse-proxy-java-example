package io.nncdevel.sseproxy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(ProxyEndpointTest.FakeClients.class)
class ProxyEndpointTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void openAiJavaEndpointReturnsEventStream() throws Exception {
    assertEventStream("/openai-java/responses");
  }

  @Test
  void httpClientEndpointReturnsEventStream() throws Exception {
    assertEventStream("/httpclient/responses");
  }

  @Test
  void restClientEndpointReturnsEventStream() throws Exception {
    assertEventStream("/restclient/responses");
  }

  private void assertEventStream(String path) throws Exception {
    mockMvc.perform(post(path)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"input\":\"hello\"}"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
  }

  @TestConfiguration
  static class FakeClients {

    @Bean("openAiJavaStreamingClient")
    StreamingClient openAiJavaStreamingClient() {
      return new NoopStreamingClient();
    }

    @Bean("jdkHttpStreamingClient")
    StreamingClient jdkHttpStreamingClient() {
      return new NoopStreamingClient();
    }

    @Bean("restClientStreamingClient")
    StreamingClient restClientStreamingClient() {
      return new NoopStreamingClient();
    }
  }

  private static final class NoopStreamingClient implements StreamingClient {

    @Override
    public void stream(ProxyRequest request, OutputStream outputStream) throws IOException {
      outputStream.flush();
    }
  }
}
