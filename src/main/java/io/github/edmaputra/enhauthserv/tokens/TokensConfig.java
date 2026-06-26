package io.github.edmaputra.enhauthserv.tokens;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
@EnableConfigurationProperties(TokenPolicyProperties.class)
public class TokensConfig {

  @Bean
  TokenSettings tokenSettings(TokenPolicyProperties tokenPolicyProperties) {
    return TokenSettings.builder()
        .accessTokenTimeToLive(tokenPolicyProperties.getAccessTokenTimeToLive())
        .refreshTokenTimeToLive(tokenPolicyProperties.getRefreshTokenTimeToLive())
        .reuseRefreshTokens(tokenPolicyProperties.isReuseRefreshTokens())
        .build();
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> clientCredentialsTokenCustomizer(
      TokenPolicyProperties tokenPolicyProperties) {
    return (context) -> {
      if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
          validateClientCredentialsScopes(context.getAuthorizedScopes(), tokenPolicyProperties);
        }
      }
    };
  }

  private static void validateClientCredentialsScopes(
      Set<String> requestedScopes,
      TokenPolicyProperties tokenPolicyProperties) {
    if (requestedScopes == null || requestedScopes.isEmpty()) {
      return;
    }

    Set<String> normalizedAllowedScopes = new HashSet<>();
    for (String allowedScope : tokenPolicyProperties.getClientCredentialsAllowedScopes()) {
      normalizedAllowedScopes.add(allowedScope.toLowerCase(Locale.ROOT));
    }

    Set<String> disallowedScopes = new HashSet<>();
    for (String requestedScope : requestedScopes) {
      if (!normalizedAllowedScopes.contains(requestedScope.toLowerCase(Locale.ROOT))) {
        disallowedScopes.add(requestedScope);
      }
    }

    if (disallowedScopes.isEmpty()) {
      return;
    }

    String description = "Scope not allowed for client_credentials grant: "
        + String.join(", ", disallowedScopes);
    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_scope", description, null));
  }
}
