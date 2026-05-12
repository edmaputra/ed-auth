package io.github.edmaputra.enhauthserv.service;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

/**
 * Shared client-authentication helper for OAuth2 machine endpoints.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClientAuthenticationService {

  private final RegisteredClientRepository registeredClientRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthenticationResult authenticateBasicClient(HttpServletRequest request) {
    return authenticateBasicClient(request.getHeader("Authorization"));
  }

  public AuthenticationResult authenticateBasicClient(String authorizationHeader) {
    String[] clientCredentials = extractClientCredentials(authorizationHeader);
    if (clientCredentials == null) {
      return AuthenticationResult.failed(null);
    }

    String clientId = clientCredentials[0];
    String clientSecret = clientCredentials[1];

    RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
    if (registeredClient == null || !isClientSecretValid(registeredClient, clientSecret)) {
      return AuthenticationResult.failed(clientId);
    }

    return AuthenticationResult.success(clientId, registeredClient);
  }

  private String[] extractClientCredentials(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Basic ")) {
      return null;
    }

    try {
      String credentials = new String(
          Base64.getDecoder().decode(authHeader.substring(6)),
          StandardCharsets.UTF_8);
      int colonIndex = credentials.indexOf(':');
      if (colonIndex == -1) {
        return null;
      }

      String clientId = credentials.substring(0, colonIndex);
      String clientSecret = credentials.substring(colonIndex + 1);
      return new String[] {clientId, clientSecret};
    } catch (Exception exception) {
      log.debug("Invalid Basic Auth header format", exception);
      return null;
    }
  }

  private boolean isClientSecretValid(RegisteredClient registeredClient, String providedSecret) {
    String registeredSecret = registeredClient.getClientSecret();
    if (registeredSecret == null) {
      return providedSecret == null || providedSecret.isEmpty();
    }

    return passwordEncoder.matches(providedSecret, registeredSecret);
  }

  public record AuthenticationResult(
      String clientId,
      RegisteredClient registeredClient,
      boolean authenticated) {

    public static AuthenticationResult failed(String clientId) {
      return new AuthenticationResult(clientId, null, false);
    }

    public static AuthenticationResult success(String clientId, RegisteredClient registeredClient) {
      return new AuthenticationResult(clientId, registeredClient, true);
    }
  }
}
