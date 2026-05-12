package io.github.edmaputra.enhauthserv.application.port.out;

public interface ClientAuthenticationPort {

  ClientAuthenticationResult authenticateBasic(String authorizationHeader);
}
