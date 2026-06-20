package io.github.edmaputra.enhauthserv.application.port.out;

public interface TokenRevocationPort {

  void revokeTokenForClient(String token, String tokenTypeHint, String registeredClientId);
}
