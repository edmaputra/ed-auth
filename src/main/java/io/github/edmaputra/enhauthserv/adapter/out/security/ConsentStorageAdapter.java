package io.github.edmaputra.enhauthserv.adapter.out.security;

import io.github.edmaputra.enhauthserv.application.port.out.ConsentStoragePort;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Component;

/**
 * Adapter for consent storage using OAuth2AuthorizationConsentService.
 * 
 * Queries and persists user consent records for OAuth2 authorization grants.
 */
@Component
@RequiredArgsConstructor
public class ConsentStorageAdapter implements ConsentStoragePort {

  private final OAuth2AuthorizationConsentService consentService;

  @Override
  public Set<String> getAuthorizedScopes(String principalName, String registeredClientId) {
    OAuth2AuthorizationConsent consent = consentService.findById(registeredClientId, principalName);
    if (consent == null) {
      return Set.of();
    }

    Set<String> scopes = consent.getAuthorities().stream()
        .map(authority -> authority.getAuthority())
        .collect(java.util.stream.Collectors.toSet());

    return scopes;
  }

  @Override
  public boolean isMissingConsent(String principalName, String registeredClientId, Set<String> requestedScopes) {
    Set<String> authorizedScopes = getAuthorizedScopes(principalName, registeredClientId);

    // Check if any requested scope is not in the set of authorized scopes
    for (String requestedScope : requestedScopes) {
      if (!authorizedScopes.contains(requestedScope)) {
        return true;  // Missing consent for at least one scope
      }
    }

    return false;  // Has consent for all requested scopes
  }

  @Override
  public void saveConsent(String principalName, String registeredClientId, Set<String> authorizedScopes) {
    // Create a new consent record with the authorized scopes
    OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent.withId(
        registeredClientId,
        principalName);

    // Add authorities for each scope (OAuth2 uses "SCOPE_" prefix convention)
    for (String scope : authorizedScopes) {
      builder.authority(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_" + scope));
    }

    consentService.save(builder.build());
  }
}
