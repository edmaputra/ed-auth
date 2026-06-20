package io.github.edmaputra.enhauthserv.application.usecase.consent;

import java.util.Set;

/**
 * Command to check or approve user consent for OAuth2 authorization.
 */
public record CheckConsentCommand(
    String principalName,
    String registeredClientId,
    Set<String> requestedScopes) {

  public CheckConsentCommand {
    if (principalName == null || principalName.isBlank()) {
      throw new IllegalArgumentException("principalName cannot be null or blank");
    }
    if (registeredClientId == null || registeredClientId.isBlank()) {
      throw new IllegalArgumentException("registeredClientId cannot be null or blank");
    }
    if (requestedScopes == null || requestedScopes.isEmpty()) {
      throw new IllegalArgumentException("requestedScopes cannot be null or empty");
    }
  }
}
