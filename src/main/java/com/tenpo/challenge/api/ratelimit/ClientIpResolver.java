package com.tenpo.challenge.api.ratelimit;

import org.springframework.http.server.reactive.ServerHttpRequest;

public interface ClientIpResolver {

  String resolve(ServerHttpRequest request);
}
