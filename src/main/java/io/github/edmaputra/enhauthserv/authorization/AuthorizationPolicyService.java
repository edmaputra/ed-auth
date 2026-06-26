package io.github.edmaputra.enhauthserv.authorization;

import io.github.edmaputra.enhauthserv.clients.ClientScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationPolicyService {

  private final ClientScopeService clientScopeService;

  public AuthorizationPolicyResult validateScope(ValidateScopeCommand command) {
    if (!clientScopeService.clientHasScope(command.clientId(), command.requiredScope())) {
      return AuthorizationPolicyResult.missingScope(command.requiredScope());
    }

    return AuthorizationPolicyResult.success();
  }
}
