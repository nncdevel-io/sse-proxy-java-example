package io.nncdevel.sseproxy;

import java.io.IOException;
import java.io.OutputStream;

public interface StreamingClient {

  void stream(ProxyRequest request, OutputStream outputStream) throws IOException;
}
