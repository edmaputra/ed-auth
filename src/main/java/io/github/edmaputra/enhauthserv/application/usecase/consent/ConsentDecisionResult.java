package io.github.edmaputra.enhauthserv.application.usecase.consent;

import java.util.Set;

/**
 * Result of a consent decision check.
 * 
 * Indicates whether consent is required before authorization can proceed,
 * and optionally includes any previously authorized scopes.
 */
public record ConsentDecisionResult(
    boolean consentRequired,
    Set<String> previouslyAuthorizedScopes,
    String error) {

  public static ConsentDecisionResult noConsentNeeded(Set<String> authorizedScopes) {
    return new ConsentDecisionResult(false, authorizedScopes == null ? Set.of() : Set.copyOf(authorizedScopes), null);
  }

  public static ConsentDecisionResult consentNeeded() {
    return new ConsentDecisionResult(true, Set.of(), null);
  }

  public static ConsentDecisionResult withError(String errorMessage) {
    return new ConsentDecisionResult(false, Set.of(), errorMessage);
  }
}
