package com.tenpo.challenge.application.port.out;

public interface ClientIpResolver {

  String resolve(String remoteAddr, String xForwardedForHeader);
}
