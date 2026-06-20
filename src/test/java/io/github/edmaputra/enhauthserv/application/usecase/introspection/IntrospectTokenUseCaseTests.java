package io.github.edmaputra.enhauthserv.application.usecase.introspection;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationPolicyInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationPort;
import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationResult;
import io.github.edmaputra.enhauthserv.application.port.out.TokenIntrospectionPort;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.ValidateScopeCommand;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class IntrospectTokenUseCaseTests {

  @Test
  void missingTokenReturnsBadRequest() {
    IntrospectTokenUseCase useCase = new IntrospectTokenUseCase(
      authorizationHeader -> ClientAuthenticationResult.success(
        "demo-client",
        "registered-demo-client",
        Set.of("introspection")),
        token -> Map.of("active", true),
        command -> AuthorizationPolicyResult.success());

    IntrospectTokenResult result = useCase.introspect(new IntrospectTokenCommand("", "Basic abc"));

    assertThat(result.status()).isEqualTo(IntrospectTokenResult.Status.BAD_REQUEST);
    assertThat(result.body().get("error")).isEqualTo("invalid_request");
  }

  @Test
  void unauthenticatedClientReturnsUnauthorized() {
    IntrospectTokenUseCase useCase = new IntrospectTokenUseCase(
        authorizationHeader -> ClientAuthenticationResult.failed(null),
        token -> Map.of("active", true),
        command -> AuthorizationPolicyResult.success());

    IntrospectTokenResult result = useCase.introspect(new IntrospectTokenCommand("token-value", "Basic bad"));

    assertThat(result.status()).isEqualTo(IntrospectTokenResult.Status.UNAUTHORIZED);
    assertThat(result.body().get("error")).isEqualTo("invalid_client");
  }

  @Test
  void clientWithoutIntrospectionScopeReturnsForbidden() {
    IntrospectTokenUseCase useCase = new IntrospectTokenUseCase(
      authorizationHeader -> ClientAuthenticationResult.success(
        "demo-client",
        "registered-demo-client",
        Set.of("read")),
        token -> Map.of("active", true),
        command -> AuthorizationPolicyResult.missingScope("introspection"));

    IntrospectTokenResult result = useCase.introspect(new IntrospectTokenCommand("token-value", "Basic abc"));

    assertThat(result.status()).isEqualTo(IntrospectTokenResult.Status.FORBIDDEN);
    assertThat(result.body().get("error")).isEqualTo("unauthorized_client");
  }

  @Test
  void successfulIntrospectionReturnsOk() {
    Map<String, Object> payload = Map.of("active", true, "client_id", "demo-client");
    IntrospectTokenUseCase useCase = new IntrospectTokenUseCase(
      authorizationHeader -> ClientAuthenticationResult.success(
        "demo-client",
        "registered-demo-client",
        Set.of("introspection", "read")),
        token -> payload,
        command -> AuthorizationPolicyResult.success());

    IntrospectTokenResult result = useCase.introspect(new IntrospectTokenCommand("token-value", "Basic abc"));

    assertThat(result.status()).isEqualTo(IntrospectTokenResult.Status.OK);
    assertThat(result.body()).isEqualTo(payload);
  }
}
