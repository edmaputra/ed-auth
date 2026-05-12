package io.github.edmaputra.enhauthserv.application.port.out;

import java.util.Set;

public record ClientAuthenticationResult(
    String clientId,
    String registeredClientId,
    Set<String> scopes,
    boolean authenticated) {

  public static ClientAuthenticationResult failed(String clientId) {
    return new ClientAuthenticationResult(clientId, null, Set.of(), false);
  }

  public static ClientAuthenticationResult success(
      String clientId,
      String registeredClientId,
      Set<String> scopes) {
    return new ClientAuthenticationResult(
        clientId,
        registeredClientId,
        scopes == null ? Set.of() : Set.copyOf(scopes),
        true);
  }
}
