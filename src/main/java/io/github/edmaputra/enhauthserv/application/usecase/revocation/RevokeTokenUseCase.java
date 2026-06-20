package io.github.edmaputra.enhauthserv.application.usecase.revocation;

import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationPolicyInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.RevokeTokenInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationPort;
import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationResult;
import io.github.edmaputra.enhauthserv.application.port.out.TokenRevocationPort;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.ValidateScopeCommand;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

public class RevokeTokenUseCase implements RevokeTokenInputPort {

  private static final String REVOCATION_SCOPE = "revocation";

  private final ClientAuthenticationPort clientAuthenticationPort;
  private final TokenRevocationPort tokenRevocationPort;
  private final AuthorizationPolicyInputPort authorizationPolicyInputPort;

  public RevokeTokenUseCase(
      ClientAuthenticationPort clientAuthenticationPort,
      TokenRevocationPort tokenRevocationPort,
      AuthorizationPolicyInputPort authorizationPolicyInputPort) {
    this.clientAuthenticationPort = clientAuthenticationPort;
    this.tokenRevocationPort = tokenRevocationPort;
    this.authorizationPolicyInputPort = authorizationPolicyInputPort;
  }

  @Override
  public RevokeTokenResult revoke(RevokeTokenCommand command) {
    if (command.token() == null || command.token().isBlank()) {
      return error(
          RevokeTokenResult.Status.BAD_REQUEST,
          "invalid_request",
          "Missing required parameter: token");
    }

    ClientAuthenticationResult authentication =
        clientAuthenticationPort.authenticateBasic(command.authorizationHeader());
    if (!authentication.authenticated()) {
      return error(
          RevokeTokenResult.Status.UNAUTHORIZED,
          "invalid_client",
          "Client authentication failed");
    }

    // Validate client has revocation scope via authorization policy
    ValidateScopeCommand scopeValidation = new ValidateScopeCommand(
        authentication.clientId(),
        AuthorizationGrantType.CLIENT_CREDENTIALS,
        REVOCATION_SCOPE);
    AuthorizationPolicyResult policyResult = authorizationPolicyInputPort.validateScope(scopeValidation);

    if (!policyResult.authorized()) {
      return error(
          RevokeTokenResult.Status.FORBIDDEN,
          policyResult.error(),
          policyResult.errorDescription());
    }

    tokenRevocationPort.revokeTokenForClient(
        command.token(),
        command.tokenTypeHint(),
        authentication.registeredClientId());

    return new RevokeTokenResult(RevokeTokenResult.Status.OK, Map.of());
  }

  private RevokeTokenResult error(
      RevokeTokenResult.Status status,
      String error,
      String errorDescription) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", error);
    errorResponse.put("error_description", errorDescription);
    return new RevokeTokenResult(status, errorResponse);
  }
}
