package io.github.edmaputra.enhauthserv.application.usecase.revocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyResult;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyUseCase;
import io.github.edmaputra.enhauthserv.clients.ClientAuthenticationResult;
import io.github.edmaputra.enhauthserv.clients.ClientAuthenticationService;
import io.github.edmaputra.enhauthserv.tokens.revocation.TokenRevoker;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokeTokenUseCaseTests {

  @Mock
  private ClientAuthenticationService clientAuthenticationService;

  @Mock
  private TokenRevoker tokenRevoker;

  @Mock
  private AuthorizationPolicyUseCase authorizationPolicyUseCase;

  private RevokeTokenUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new RevokeTokenUseCase(
        clientAuthenticationService,
        tokenRevoker,
        authorizationPolicyUseCase);
  }

  @Test
  void missingTokenReturnsBadRequest() {
    RevokeTokenResult result = useCase.revoke(new RevokeTokenCommand("", "access_token", "Basic abc"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.BAD_REQUEST);
    assertThat(result.body().get("error")).isEqualTo("invalid_request");
  }

  @Test
  void unauthenticatedClientReturnsUnauthorized() {
    when(clientAuthenticationService.authenticateBasic("Basic bad"))
        .thenReturn(ClientAuthenticationResult.failed(null));

    RevokeTokenResult result = useCase.revoke(new RevokeTokenCommand("token-value", "access_token", "Basic bad"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.UNAUTHORIZED);
    assertThat(result.body().get("error")).isEqualTo("invalid_client");
  }

  @Test
  void clientWithoutRevocationScopeReturnsForbidden() {
    when(clientAuthenticationService.authenticateBasic("Basic abc"))
        .thenReturn(ClientAuthenticationResult.success(
            "demo-client",
            "registered-demo-client",
            Set.of("read")));
    when(authorizationPolicyUseCase.validateScope(org.mockito.ArgumentMatchers.any()))
        .thenReturn(AuthorizationPolicyResult.missingScope("revocation"));

    RevokeTokenResult result = useCase.revoke(new RevokeTokenCommand("token-value", "access_token", "Basic abc"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.FORBIDDEN);
    assertThat(result.body().get("error")).isEqualTo("unauthorized_client");
  }

  @Test
  void successfulRevocationDelegatesToTokenRevoker() {
    when(clientAuthenticationService.authenticateBasic("Basic abc"))
        .thenReturn(ClientAuthenticationResult.success(
            "demo-client",
            "registered-demo-client",
            Set.of("revocation", "read")));
    when(authorizationPolicyUseCase.validateScope(org.mockito.ArgumentMatchers.any()))
        .thenReturn(AuthorizationPolicyResult.success());

    RevokeTokenResult result = useCase.revoke(
        new RevokeTokenCommand("token-value", "refresh_token", "Basic abc"));

    assertThat(result.status()).isEqualTo(RevokeTokenResult.Status.OK);
    verify(tokenRevoker).revokeTokenForClient("token-value", "refresh_token", "registered-demo-client");
  }
}
