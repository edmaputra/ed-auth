package io.github.edmaputra.enhauthserv.service;

import java.util.Set;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;

/**
 * Service for determining if a client is authorized to revoke tokens.
 */
@Service
public class RevocationAuthorizationService {

  private static final String REVOCATION_SCOPE = "revocation";

  /**
   * Checks if a client is authorized to revoke tokens.
   */
  public boolean canRevoke(RegisteredClient registeredClient) {
    if (registeredClient == null) {
      return false;
    }

    Set<String> clientScopes = registeredClient.getScopes();
    if (clientScopes == null || clientScopes.isEmpty()) {
      return false;
    }

    return clientScopes.contains(REVOCATION_SCOPE);
  }

  public String getRevocationScope() {
    return REVOCATION_SCOPE;
  }
}
