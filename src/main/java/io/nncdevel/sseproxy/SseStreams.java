package io.nncdevel.sseproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class SseStreams {

  private SseStreams() {
  }

  static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    inputStream.transferTo(outputStream);
    outputStream.flush();
  }

  static void writeData(OutputStream outputStream, String json) throws IOException {
    outputStream.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
    outputStream.flush();
  }

  static void writeError(OutputStream outputStream, Throwable throwable) throws IOException {
    String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
    outputStream.write(("event: error\ndata: {\"error\":\"" + escaped + "\"}\n\n")
        .getBytes(StandardCharsets.UTF_8));
    outputStream.flush();
  }
}
