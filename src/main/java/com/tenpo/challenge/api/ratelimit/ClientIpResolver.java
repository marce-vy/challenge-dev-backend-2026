package com.tenpo.challenge.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientIpResolver {

  String resolve(HttpServletRequest request);
}
