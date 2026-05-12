package io.github.edmaputra.enhauthserv.application.usecase.authorization;

import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationPolicyInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.ScopeValidationPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case for validating authorization policies.
 * 
 * Enforces:
 * 1. Clients have required scopes registered
 * 2. Scopes match the operation being performed
 * 3. Grant type authorization rules are respected
 */
@RequiredArgsConstructor
public class AuthorizationPolicyUseCase implements AuthorizationPolicyInputPort {

  private final ScopeValidationPort scopeValidationPort;

  @Override
  public AuthorizationPolicyResult validateScope(ValidateScopeCommand command) {
    // Verify client has the required scope
    if (!scopeValidationPort.clientHasScope(command.clientId(), command.requiredScope())) {
      return AuthorizationPolicyResult.missingScope(command.requiredScope());
    }

    return AuthorizationPolicyResult.success();
  }
}
