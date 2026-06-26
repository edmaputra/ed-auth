package io.github.edmaputra.enhauthserv.application.usecase.consent;

import io.github.edmaputra.enhauthserv.consent.ConsentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case for managing OAuth2 authorization consent.
 * 
 * Decides whether explicit user consent is required for a client
 * to access specific scopes on behalf of the user.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationConsentUseCase {

  private final ConsentStore consentStore;

  public ConsentDecisionResult checkConsent(CheckConsentCommand command) {
    // Check if user is missing consent for any of the requested scopes
    if (consentStore.isMissingConsent(
        command.principalName(),
        command.registeredClientId(),
        command.requestedScopes())) {
      // User has not consented to all requested scopes
      return ConsentDecisionResult.consentNeeded();
    }

    // User has already authorized these scopes; retrieve the authorized set
    var authorizedScopes = consentStore.getAuthorizedScopes(
        command.principalName(),
        command.registeredClientId());

    return ConsentDecisionResult.noConsentNeeded(authorizedScopes);
  }

  public void approveConsent(CheckConsentCommand command) {
    consentStore.saveConsent(
        command.principalName(),
        command.registeredClientId(),
        command.requestedScopes());
  }
}
