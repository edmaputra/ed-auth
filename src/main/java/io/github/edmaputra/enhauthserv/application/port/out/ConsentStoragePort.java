package io.github.edmaputra.enhauthserv.application.port.out;

import java.util.Set;

/**
 * Output port for accessing consent data.
 * 
 * Implementations query and persist authorization consent records.
 */
public interface ConsentStoragePort {

  /**
   * Check if a principal has previously authorized a client for any scopes.
   *
   * @param principalName the authenticated user
   * @param registeredClientId the OAuth2 client
   * @return set of previously authorized scopes, empty if no prior consent
   */
  Set<String> getAuthorizedScopes(String principalName, String registeredClientId);

  /**
   * Check if principal is missing consent for any of the requested scopes.
   *
   * @param principalName the authenticated user
   * @param registeredClientId the OAuth2 client
   * @param requestedScopes the scopes being requested
   * @return true if user has NOT authorized all requested scopes
   */
  boolean isMissingConsent(String principalName, String registeredClientId, Set<String> requestedScopes);

  /**
   * Save that a principal has authorized a client for specific scopes.
   *
   * @param principalName the authenticated user
   * @param registeredClientId the OAuth2 client
   * @param authorizedScopes the scopes being approved
   */
  void saveConsent(String principalName, String registeredClientId, Set<String> authorizedScopes);
}
