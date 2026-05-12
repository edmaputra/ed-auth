package io.github.edmaputra.enhauthserv.application.usecase.authorization;

/**
 * Result of authorization policy validation.
 */
public record AuthorizationPolicyResult(
    boolean authorized,
    String error,
    String errorDescription) {

  public static AuthorizationPolicyResult success() {
    return new AuthorizationPolicyResult(true, null, null);
  }

  public static AuthorizationPolicyResult failure(String error, String errorDescription) {
    return new AuthorizationPolicyResult(false, error, errorDescription);
  }

  public static AuthorizationPolicyResult missingScope(String requiredScope) {
    return failure(
        "unauthorized_client",
        "Client is not authorized for scope: " + requiredScope);
  }
}
