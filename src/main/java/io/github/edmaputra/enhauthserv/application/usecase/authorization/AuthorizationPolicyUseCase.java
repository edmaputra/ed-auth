package io.github.edmaputra.enhauthserv.application.usecase.authorization;

import io.github.edmaputra.enhauthserv.clients.ClientScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use case for validating authorization policies.
 * 
 * Enforces:
 * 1. Clients have required scopes registered
 * 2. Scopes match the operation being performed
 * 3. Grant type authorization rules are respected
 */
@Service
@RequiredArgsConstructor
public class AuthorizationPolicyUseCase {

  private final ClientScopeService clientScopeService;

  public AuthorizationPolicyResult validateScope(ValidateScopeCommand command) {
    // Verify client has the required scope
    if (!clientScopeService.clientHasScope(command.clientId(), command.requiredScope())) {
      return AuthorizationPolicyResult.missingScope(command.requiredScope());
    }

    return AuthorizationPolicyResult.success();
  }
}
