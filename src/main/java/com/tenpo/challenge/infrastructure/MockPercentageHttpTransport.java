package com.tenpo.challenge.infrastructure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

public class MockPercentageHttpTransport implements ClientHttpRequestFactory {

  private static final byte[] RESPONSE_BODY =
      "{\"percentage\":10}".getBytes(StandardCharsets.UTF_8);

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
    return new MockPercentageHttpRequest(uri, httpMethod);
  }

  private static final class MockPercentageHttpRequest extends AbstractClientHttpRequest {

    private final URI uri;
    private final HttpMethod httpMethod;
    private final ByteArrayOutputStream requestBody = new ByteArrayOutputStream();

    private MockPercentageHttpRequest(URI uri, HttpMethod httpMethod) {
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
      return requestBody;
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers) {
      return new MockPercentageHttpResponse();
    }
  }

  private static final class MockPercentageHttpResponse implements ClientHttpResponse {

    private final HttpHeaders headers = new HttpHeaders();

    private MockPercentageHttpResponse() {
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setContentLength(RESPONSE_BODY.length);
    }

    @Override
    public HttpStatusCode getStatusCode() {
      return HttpStatus.OK;
    }

    @Override
    public String getStatusText() {
      return "OK";
    }

    @Override
    public void close() {
      // Nothing to close: the response is backed by an in-memory byte array.
    }

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
