package io.github.edmaputra.enhauthserv.application.usecase.revocation;

import io.github.edmaputra.enhauthserv.clients.ClientAuthenticationService;
import io.github.edmaputra.enhauthserv.clients.ClientAuthenticationResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyUseCase;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.ValidateScopeCommand;
import io.github.edmaputra.enhauthserv.tokens.revocation.TokenRevoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Service
@RequiredArgsConstructor
public class RevokeTokenUseCase {

  private static final String REVOCATION_SCOPE = "revocation";

  private final ClientAuthenticationService clientAuthenticationService;
  private final TokenRevoker tokenRevoker;
  private final AuthorizationPolicyUseCase authorizationPolicyUseCase;

  public RevokeTokenResult revoke(RevokeTokenCommand command) {
    if (command.token() == null || command.token().isBlank()) {
      return error(
          RevokeTokenResult.Status.BAD_REQUEST,
          "invalid_request",
          "Missing required parameter: token");
    }

    ClientAuthenticationResult authentication =
        clientAuthenticationService.authenticateBasic(command.authorizationHeader());
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
    AuthorizationPolicyResult policyResult = authorizationPolicyUseCase.validateScope(scopeValidation);

    if (!policyResult.authorized()) {
      return error(
          RevokeTokenResult.Status.FORBIDDEN,
          policyResult.error(),
          policyResult.errorDescription());
    }

    tokenRevoker.revokeTokenForClient(
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
