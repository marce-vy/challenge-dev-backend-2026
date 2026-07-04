package com.tenpo.challenge.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

public class FailingPercentageHttpTransport implements ClientHttpRequestFactory {

  private static final byte[] RESPONSE_BODY =
      "{\"error\":\"provider unavailable\"}".getBytes(StandardCharsets.UTF_8);

  private final AtomicInteger attempts = new AtomicInteger();

  public int attempts() {
    return attempts.get();
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
    return new FailingPercentageHttpRequest(uri, httpMethod);
  }

  private final class FailingPercentageHttpRequest extends AbstractClientHttpRequest {

    private final URI uri;
    private final HttpMethod httpMethod;

    private FailingPercentageHttpRequest(URI uri, HttpMethod httpMethod) {
      this.uri = uri;
      this.httpMethod = httpMethod;
    }

    @Override
    public HttpMethod getMethod() {
      return httpMethod;
    }

    @Override
    public URI getURI() {
      return uri;
    }

    @Override
    protected OutputStream getBodyInternal(HttpHeaders headers) {
      return OutputStream.nullOutputStream();
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers) {
      attempts.incrementAndGet();
      return new FailingPercentageHttpResponse();
    }
  }

  private static final class FailingPercentageHttpResponse implements ClientHttpResponse {

    private final HttpHeaders headers = new HttpHeaders();

    private FailingPercentageHttpResponse() {
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setContentLength(RESPONSE_BODY.length);
    }

    @Override
    public HttpStatusCode getStatusCode() {
      return HttpStatus.SERVICE_UNAVAILABLE;
    }

    @Override
    public String getStatusText() {
      return "Service Unavailable";
    }

    @Override
    public void close() {}

    @Override
    public InputStream getBody() throws IOException {
      return new ByteArrayInputStream(RESPONSE_BODY);
    }

    @Override
    public HttpHeaders getHeaders() {
      return headers;
    }
  }
}
