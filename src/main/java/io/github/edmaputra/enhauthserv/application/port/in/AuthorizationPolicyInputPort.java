package io.github.edmaputra.enhauthserv.application.port.in;

import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.ValidateScopeCommand;

/**
 * Input port for authorization policy validation.
 * 
 * Clients call this port to validate that:
 * 1. A client has the required scopes for their grant type
 * 2. The requested operation is authorized under the policy
 */
public interface AuthorizationPolicyInputPort {

  /**
   * Validate that a client is authorized to perform an operation with the given scopes.
   *
   * @param command the validation request (clientId, grantType, requiredScope, etc.)
   * @return the validation result (authorized true/false with error details if unauthorized)
   */
  AuthorizationPolicyResult validateScope(ValidateScopeCommand command);
}
