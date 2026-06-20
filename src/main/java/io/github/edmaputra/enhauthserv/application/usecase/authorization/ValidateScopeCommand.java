package io.github.edmaputra.enhauthserv.application.usecase.authorization;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Command to validate client authorization for a scope/operation.
 */
public record ValidateScopeCommand(
    String clientId,
    AuthorizationGrantType grantType,
    String requiredScope) {

  public ValidateScopeCommand {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("clientId cannot be null or blank");
    }
    if (grantType == null) {
      throw new IllegalArgumentException("grantType cannot be null");
    }
    if (requiredScope == null || requiredScope.isBlank()) {
      throw new IllegalArgumentException("requiredScope cannot be null or blank");
    }
  }
}
