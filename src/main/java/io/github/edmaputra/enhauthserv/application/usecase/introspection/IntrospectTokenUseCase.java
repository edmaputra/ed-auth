package io.github.edmaputra.enhauthserv.application.usecase.introspection;

import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationPolicyInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.IntrospectTokenInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationPort;
import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationResult;
import io.github.edmaputra.enhauthserv.application.port.out.TokenIntrospectionPort;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.ValidateScopeCommand;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

public class IntrospectTokenUseCase implements IntrospectTokenInputPort {

  private static final String INTROSPECTION_SCOPE = "introspection";

  private final ClientAuthenticationPort clientAuthenticationPort;
  private final TokenIntrospectionPort tokenIntrospectionPort;
  private final AuthorizationPolicyInputPort authorizationPolicyInputPort;

  public IntrospectTokenUseCase(
      ClientAuthenticationPort clientAuthenticationPort,
      TokenIntrospectionPort tokenIntrospectionPort,
      AuthorizationPolicyInputPort authorizationPolicyInputPort) {
    this.clientAuthenticationPort = clientAuthenticationPort;
    this.tokenIntrospectionPort = tokenIntrospectionPort;
    this.authorizationPolicyInputPort = authorizationPolicyInputPort;
  }

  @Override
  public IntrospectTokenResult introspect(IntrospectTokenCommand command) {
    if (command.token() == null || command.token().isEmpty()) {
      return error(
          IntrospectTokenResult.Status.BAD_REQUEST,
          "invalid_request",
          "Missing required parameter: token");
    }

    ClientAuthenticationResult authentication =
        clientAuthenticationPort.authenticateBasic(command.authorizationHeader());
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
    AuthorizationPolicyResult policyResult = authorizationPolicyInputPort.validateScope(scopeValidation);

    if (!policyResult.authorized()) {
      return error(
          IntrospectTokenResult.Status.FORBIDDEN,
          policyResult.error(),
          policyResult.errorDescription());
    }

    Map<String, Object> introspectionResponse = tokenIntrospectionPort.introspect(command.token());
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
