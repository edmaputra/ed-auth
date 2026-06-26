package io.github.edmaputra.enhauthserv.application.usecase.introspection;

import io.github.edmaputra.enhauthserv.clients.ClientAuthenticationService;
import io.github.edmaputra.enhauthserv.clients.ClientAuthenticationResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyUseCase;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.ValidateScopeCommand;
import io.github.edmaputra.enhauthserv.service.TokenIntrospectionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Service
@RequiredArgsConstructor
public class IntrospectTokenUseCase {

  private static final String INTROSPECTION_SCOPE = "introspection";

  private final ClientAuthenticationService clientAuthenticationService;
  private final TokenIntrospectionValidator tokenIntrospectionValidator;
  private final AuthorizationPolicyUseCase authorizationPolicyUseCase;

  public IntrospectTokenResult introspect(IntrospectTokenCommand command) {
    if (command.token() == null || command.token().isEmpty()) {
      return error(
          IntrospectTokenResult.Status.BAD_REQUEST,
          "invalid_request",
          "Missing required parameter: token");
    }

    ClientAuthenticationResult authentication =
        clientAuthenticationService.authenticateBasic(command.authorizationHeader());
    if (!authentication.authenticated()) {
      return error(
          IntrospectTokenResult.Status.UNAUTHORIZED,
          "invalid_client",
          "Client authentication failed");
    }

    // Validate client has introspection scope via authorization policy
    ValidateScopeCommand scopeValidation = new ValidateScopeCommand(
        authentication.clientId(),
        AuthorizationGrantType.CLIENT_CREDENTIALS,
        INTROSPECTION_SCOPE);
    AuthorizationPolicyResult policyResult = authorizationPolicyUseCase.validateScope(scopeValidation);

    if (!policyResult.authorized()) {
      return error(
          IntrospectTokenResult.Status.FORBIDDEN,
          policyResult.error(),
          policyResult.errorDescription());
    }

    Map<String, Object> introspectionResponse = tokenIntrospectionValidator.introspect(command.token());
    return new IntrospectTokenResult(IntrospectTokenResult.Status.OK, introspectionResponse);
  }

  private IntrospectTokenResult error(
      IntrospectTokenResult.Status status,
      String error,
      String errorDescription) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", error);
    errorResponse.put("error_description", errorDescription);
    return new IntrospectTokenResult(status, errorResponse);
  }
}
