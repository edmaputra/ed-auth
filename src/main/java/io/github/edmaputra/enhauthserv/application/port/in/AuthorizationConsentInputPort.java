package io.github.edmaputra.enhauthserv.application.port.in;

import io.github.edmaputra.enhauthserv.application.usecase.consent.CheckConsentCommand;
import io.github.edmaputra.enhauthserv.application.usecase.consent.ConsentDecisionResult;

/**
 * Input port for managing OAuth2 authorization consent.
 * 
 * Clients call this port to:
 * 1. Check if a user has already given consent for a client/scope combination
 * 2. Determine if explicit consent is required before granting authorization
 */
public interface AuthorizationConsentInputPort {

  /**
   * Check if a user has already consented to authorize a client for the requested scopes.
   *
   * @param command the consent check request (principalName, registeredClientId, requestedScopes)
   * @return the consent decision (consentRequired true/false, existingScopes if already consented)
   */
  ConsentDecisionResult checkConsent(CheckConsentCommand command);

  /**
   * Record that a user has given consent for a client to access specific scopes.
   *
   * @param command the consent approval request
   */
  void approveConsent(CheckConsentCommand command);
}
