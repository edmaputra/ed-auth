package io.github.edmaputra.enhauthserv.controller;

import io.github.edmaputra.enhauthserv.service.ClientAuthenticationService;
import io.github.edmaputra.enhauthserv.service.RevocationAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for RFC 7009 OAuth 2.0 Token Revocation.
 *
 * Endpoint: POST /oauth2/revoke
 * Authentication: HTTP Basic Auth (client_id:client_secret)
 * Request Parameters: token (required), token_type_hint (optional)
 */
@RestController
@RequestMapping({"/oauth2/revoke", "/t/{tenant}/oauth2/revoke"})
@Slf4j
@RequiredArgsConstructor
public class OAuth2TokenRevocationController {

  private final ClientAuthenticationService clientAuthenticationService;
  private final OAuth2AuthorizationService oauth2AuthorizationService;
  private final RevocationAuthorizationService revocationAuthorizationService;

  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<?> revoke(
      @PathVariable(value = "tenant", required = false) String tenant,
      @RequestParam(value = "token", required = false) String token,
      @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
      HttpServletRequest request) {

    if (token == null || token.isBlank()) {
      return buildErrorResponse(HttpStatus.BAD_REQUEST, "invalid_request",
          "Missing required parameter: token");
    }

    ClientAuthenticationService.AuthenticationResult clientAuthentication =
      clientAuthenticationService.authenticateBasicClient(request);
    if (!clientAuthentication.authenticated()) {
      return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_client",
          "Client authentication failed");
    }

    RegisteredClient registeredClient = clientAuthentication.registeredClient();

    if (!revocationAuthorizationService.canRevoke(registeredClient)) {
      return buildErrorResponse(HttpStatus.FORBIDDEN, "unauthorized_client",
          "Client is not authorized to revoke tokens (missing scope: "
              + revocationAuthorizationService.getRevocationScope() + ")");
    }

    revokeTokenForClient(token, tokenTypeHint, registeredClient.getId());
    return ResponseEntity.ok().build();
  }

  private void revokeTokenForClient(String token, String tokenTypeHint, String registeredClientId) {
    OAuth2TokenType tokenType = resolveTokenType(tokenTypeHint);
    OAuth2Authorization authorization = oauth2AuthorizationService.findByToken(token, tokenType);

    // RFC 7009 requires idempotent success for unknown tokens.
    if (authorization == null) {
      return;
    }

    // A client may only revoke tokens it owns.
    if (!registeredClientId.equals(authorization.getRegisteredClientId())) {
      return;
    }

    OAuth2Authorization.Builder builder = OAuth2Authorization.from(authorization);
    invalidateMatchingTokens(builder, authorization, token);
    oauth2AuthorizationService.save(builder.build());
  }

  private OAuth2TokenType resolveTokenType(String tokenTypeHint) {
    if ("access_token".equals(tokenTypeHint)) {
      return OAuth2TokenType.ACCESS_TOKEN;
    }
    if ("refresh_token".equals(tokenTypeHint)) {
      return OAuth2TokenType.REFRESH_TOKEN;
    }
    return null;
  }

  private void invalidateMatchingTokens(
      OAuth2Authorization.Builder builder,
      OAuth2Authorization authorization,
      String tokenValue) {
    OAuth2Authorization.Token<?> accessToken = authorization.getAccessToken();
    if (accessToken != null && tokenValue.equals(accessToken.getToken().getTokenValue())) {
      builder.invalidate(accessToken.getToken());
    }

    OAuth2Authorization.Token<?> refreshToken = authorization.getRefreshToken();
    if (refreshToken != null && tokenValue.equals(refreshToken.getToken().getTokenValue())) {
      builder.invalidate(refreshToken.getToken());

      // Revoking refresh tokens also invalidates sibling access tokens.
      if (accessToken != null) {
        builder.invalidate(accessToken.getToken());
      }
    }
  }

  private ResponseEntity<Map<String, Object>> buildErrorResponse(
      HttpStatus status,
      String error,
      String errorDescription) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", error);
    errorResponse.put("error_description", errorDescription);
    return new ResponseEntity<>(errorResponse, status);
  }
}
