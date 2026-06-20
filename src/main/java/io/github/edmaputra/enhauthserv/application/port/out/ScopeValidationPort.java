package io.github.edmaputra.enhauthserv.application.port.out;

import java.util.Set;

/**
 * Output port for accessing client scope data.
 * 
 * Implementations query the underlying system (e.g., database) for
 * registered client scopes.
 */
public interface ScopeValidationPort {

  /**
   * Get all scopes authorized for a client.
   *
   * @param clientId the client identifier
   * @return set of authorized scopes, empty set if client not found or has no scopes
   */
  Set<String> getClientScopes(String clientId);

  /**
   * Check if a client has a specific scope.
   *
   * @param clientId the client identifier
   * @param scope the scope to check
   * @return true if client is authorized for this scope, false otherwise
   */
  boolean clientHasScope(String clientId, String scope);
}
