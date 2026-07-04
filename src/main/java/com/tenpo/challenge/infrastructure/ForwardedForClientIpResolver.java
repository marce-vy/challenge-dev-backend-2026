package com.tenpo.challenge.infrastructure;

import com.tenpo.challenge.application.port.out.ClientIpResolver;

public class ForwardedForClientIpResolver implements ClientIpResolver {

  @Override
  public String resolve(String remoteAddr, String xForwardedForHeader) {
    if (xForwardedForHeader != null && !xForwardedForHeader.isBlank()) {
      return xForwardedForHeader.split(",")[0].trim();
    }
    return remoteAddr;
  }
}
