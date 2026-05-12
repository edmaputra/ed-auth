package io.github.edmaputra.enhauthserv.application.usecase.consent;

import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationConsentInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.ConsentStoragePort;
import lombok.RequiredArgsConstructor;

/**
 * Use case for managing OAuth2 authorization consent.
 * 
 * Decides whether explicit user consent is required for a client
 * to access specific scopes on behalf of the user.
 */
@RequiredArgsConstructor
public class AuthorizationConsentUseCase implements AuthorizationConsentInputPort {

  private final ConsentStoragePort consentStoragePort;

  @Override
  public ConsentDecisionResult checkConsent(CheckConsentCommand command) {
    // Check if user is missing consent for any of the requested scopes
    if (consentStoragePort.isMissingConsent(
        command.principalName(),
        command.registeredClientId(),
        command.requestedScopes())) {
      // User has not consented to all requested scopes
      return ConsentDecisionResult.consentNeeded();
    }

    // User has already authorized these scopes; retrieve the authorized set
    var authorizedScopes = consentStoragePort.getAuthorizedScopes(
        command.principalName(),
        command.registeredClientId());

    return ConsentDecisionResult.noConsentNeeded(authorizedScopes);
  }

  @Override
  public void approveConsent(CheckConsentCommand command) {
    consentStoragePort.saveConsent(
        command.principalName(),
        command.registeredClientId(),
        command.requestedScopes());
  }
}
