package io.github.edmaputra.enhauthserv.adapter.out.security;

import io.github.edmaputra.enhauthserv.application.port.out.ScopeValidationPort;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter for scope validation using RegisteredClientRepository.
 * 
 * Queries the registered client store to determine which scopes
 * are authorized for a client.
 */
@Component
@RequiredArgsConstructor
public class ScopeValidationAdapter implements ScopeValidationPort {

  private final RegisteredClientRepository registeredClientRepository;

  @Override
  public Set<String> getClientScopes(String clientId) {
    RegisteredClient client = registeredClientRepository.findByClientId(clientId);
    if (client == null) {
      return Set.of();
    }

    Set<String> scopes = client.getScopes();
    return scopes == null ? Set.of() : Set.copyOf(scopes);
  }

  @Override
  public boolean clientHasScope(String clientId, String scope) {
    Set<String> clientScopes = getClientScopes(clientId);
    return clientScopes.contains(scope);
  }
}
