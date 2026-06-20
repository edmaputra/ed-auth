package io.github.edmaputra.enhauthserv.application.usecase.revocation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationResult;
import io.github.edmaputra.enhauthserv.application.port.out.TokenRevocationPort;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RevokeTokenUseCaseTests {

  @Test
  void missingTokenReturnsBadRequest() {
    RevokeTokenUseCase useCase = new RevokeTokenUseCase(
        authorizationHeader -> ClientAuthenticationResult.success(
            "demo-client",
            "registered-demo-client",
            Set.of("revocation")),
        (token, tokenTypeHint, registeredClientId) -> {},
        command -> AuthorizationPolicyResult.success());

    RevokeTokenResult result = useCase.revoke(new RevokeTokenCommand("", "access_token", "Basic abc"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.BAD_REQUEST);
    assertThat(result.body().get("error")).isEqualTo("invalid_request");
  }

  @Test
  void unauthenticatedClientReturnsUnauthorized() {
    RevokeTokenUseCase useCase = new RevokeTokenUseCase(
        authorizationHeader -> ClientAuthenticationResult.failed(null),
        (token, tokenTypeHint, registeredClientId) -> {},
        command -> AuthorizationPolicyResult.success());

    RevokeTokenResult result = useCase.revoke(new RevokeTokenCommand("token-value", "access_token", "Basic bad"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.UNAUTHORIZED);
    assertThat(result.body().get("error")).isEqualTo("invalid_client");
  }

  @Test
  void clientWithoutRevocationScopeReturnsForbidden() {
    RevokeTokenUseCase useCase = new RevokeTokenUseCase(
        authorizationHeader -> ClientAuthenticationResult.success(
            "demo-client",
            "registered-demo-client",
            Set.of("read")),
        (token, tokenTypeHint, registeredClientId) -> {},
        command -> AuthorizationPolicyResult.missingScope("revocation"));

    RevokeTokenResult result = useCase.revoke(new RevokeTokenCommand("token-value", "access_token", "Basic abc"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.FORBIDDEN);
    assertThat(result.body().get("error")).isEqualTo("unauthorized_client");
  }

  @Test
  void successfulRevocationDelegatesToOutputPort() {
    AtomicReference<String> capturedToken = new AtomicReference<>();
    AtomicReference<String> capturedTokenTypeHint = new AtomicReference<>();
    AtomicReference<String> capturedRegisteredClientId = new AtomicReference<>();

    TokenRevocationPort revocationPort = (token, tokenTypeHint, registeredClientId) -> {
      capturedToken.set(token);
      capturedTokenTypeHint.set(tokenTypeHint);
      capturedRegisteredClientId.set(registeredClientId);
    };

    RevokeTokenUseCase useCase = new RevokeTokenUseCase(
        authorizationHeader -> ClientAuthenticationResult.success(
            "demo-client",
            "registered-demo-client",
            Set.of("revocation", "read")),
        revocationPort,
        command -> AuthorizationPolicyResult.success());

    RevokeTokenResult result = useCase.revoke(
        new RevokeTokenCommand("token-value", "refresh_token", "Basic abc"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.OK);
    assertThat(capturedToken.get()).isEqualTo("token-value");
    assertThat(capturedTokenTypeHint.get()).isEqualTo("refresh_token");
    assertThat(capturedRegisteredClientId.get()).isEqualTo("registered-demo-client");
  }
}
