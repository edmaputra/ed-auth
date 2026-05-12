package io.github.edmaputra.enhauthserv.application.usecase.registration;

import io.github.edmaputra.enhauthserv.application.port.in.RegisteredClientBootstrapInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.RegisteredClientManagementPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@RequiredArgsConstructor
public class DefaultRegisteredClientBootstrapUseCase implements RegisteredClientBootstrapInputPort {

  private static final String DEMO_CLIENT_ID = "demo-client";
  private static final String DEMO_CLIENT_SECRET = "demo-secret";
  private static final String DEMO_CLIENT_REDIRECT_URI =
      "http://127.0.0.1:9000/login/oauth2/code/demo-client";
  private static final String DEMO_CLIENT_POST_LOGOUT_REDIRECT_URI =
      "http://127.0.0.1:9000/logged-out";

  private static final String PKCE_PUBLIC_CLIENT_ID = "pkce-public-client";
  private static final String PKCE_PUBLIC_CLIENT_REDIRECT_URI =
      "http://127.0.0.1:9000/login/oauth2/code/pkce-public-client";
  private static final String PKCE_PUBLIC_CLIENT_POST_LOGOUT_REDIRECT_URI =
      "http://127.0.0.1:9000/logged-out";

  private final RegisteredClientManagementPort registeredClientManagementPort;
  private final PasswordEncoder passwordEncoder;
  private final TokenSettings tokenSettings;

  @Override
  public void ensureDefaultClients() {
    ensureDemoClient();
    ensurePkcePublicClient();
  }

  private void ensureDemoClient() {
    if (registeredClientManagementPort.findByClientId(DEMO_CLIENT_ID) != null) {
      return;
    }

    registeredClientManagementPort.save(createDemoClient());
  }

  private void ensurePkcePublicClient() {
    if (registeredClientManagementPort.findByClientId(PKCE_PUBLIC_CLIENT_ID) != null) {
      return;
    }

    registeredClientManagementPort.save(createPkcePublicClient());
  }

  private RegisteredClient createDemoClient() {
    return RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId(DEMO_CLIENT_ID)
        .clientSecret(passwordEncoder.encode(DEMO_CLIENT_SECRET))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(DEMO_CLIENT_REDIRECT_URI)
        .postLogoutRedirectUri(DEMO_CLIENT_POST_LOGOUT_REDIRECT_URI)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .scope("write")
        .scope("introspection")
        .scope("revocation")
        .tokenSettings(tokenSettings)
        .build();
  }

  private RegisteredClient createPkcePublicClient() {
    return RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId(PKCE_PUBLIC_CLIENT_ID)
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(PKCE_PUBLIC_CLIENT_REDIRECT_URI)
        .postLogoutRedirectUri(PKCE_PUBLIC_CLIENT_POST_LOGOUT_REDIRECT_URI)
        .scope("openid")
        .scope("profile")
        .scope("email")
        .scope("read")
        .clientSettings(ClientSettings.builder().requireProofKey(true).build())
        .tokenSettings(tokenSettings)
        .build();
  }
}