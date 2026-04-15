package io.github.edmaputra.enhauthserv.config;

import io.github.edmaputra.enhauthserv.entity.ClaimInclusionRule;
import io.github.edmaputra.enhauthserv.entity.ClaimTarget;
import io.github.edmaputra.enhauthserv.entity.UserProfile;
import io.github.edmaputra.enhauthserv.entity.UserProfileAttribute;
import io.github.edmaputra.enhauthserv.repository.ClaimInclusionRuleRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileAttributeRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileRepository;
import io.github.edmaputra.enhauthserv.service.UserProfileService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(TokenPolicyProperties.class)
public class SecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain authorizationServerSecurityFilterChain(
      HttpSecurity http,
      Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

    http
        .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(authorizationServerConfigurer, (authorizationServer) ->
            authorizationServer
            .oidc((oidc) ->
              oidc.userInfoEndpoint((userInfoEndpoint) ->
                userInfoEndpoint.userInfoMapper(userInfoMapper)))
        )
        .authorizeHttpRequests((authorize) ->
            authorize.anyRequest().authenticated()
        )
        // Redirect to the login page when not authenticated from the
        // authorization endpoint
        .exceptionHandling((exceptions) -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
            )
        );

    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain introspectionFilterChain(HttpSecurity http) throws Exception {
    // Custom machine-to-machine endpoints handle their own client authentication.
    // This filter chain ensures JSON responses instead of login redirects.
    http
      .securityMatcher("/oauth2/introspect", "/oauth2/revoke")
        .authorizeHttpRequests((authorize) -> authorize
            .anyRequest().permitAll()
        );

    return http.build();
  }

  @Bean
  @Order(3)
  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests((authorize) -> authorize
        .requestMatchers("/logged-out").permitAll()
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcRegisteredClientRepository(jdbcTemplate);
  }

  @Bean
  OAuth2AuthorizationService authorizationService(
      JdbcTemplate jdbcTemplate,
      RegisteredClientRepository registeredClientRepository) {
    return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
  }

  @Bean
  OAuth2AuthorizationConsentService authorizationConsentService(
      JdbcTemplate jdbcTemplate,
      RegisteredClientRepository registeredClientRepository) {
    return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
  }

  @Bean
  @Order(1)
  CommandLineRunner demoRegisteredClientSeeder(
      RegisteredClientRepository registeredClientRepository,
      PasswordEncoder passwordEncoder,
      TokenSettings tokenSettings) {
    return args -> {
      if (registeredClientRepository.findByClientId("demo-client") == null) {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("demo-client")
            .clientSecret(passwordEncoder.encode("demo-secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://127.0.0.1:9000/login/oauth2/code/demo-client")
            .postLogoutRedirectUri("http://127.0.0.1:9000/logged-out")
            .scope("openid")
            .scope("profile")
            .scope("email")
            .scope("read")
            .scope("write")
            .scope("introspection")
            .scope("revocation")
            .tokenSettings(tokenSettings)
            .build();

        registeredClientRepository.save(registeredClient);
      }

      if (registeredClientRepository.findByClientId("pkce-public-client") == null) {
        RegisteredClient pkcePublicClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("pkce-public-client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://127.0.0.1:9000/login/oauth2/code/pkce-public-client")
            .postLogoutRedirectUri("http://127.0.0.1:9000/logged-out")
            .scope("openid")
            .scope("profile")
            .scope("email")
            .scope("read")
            .clientSettings(ClientSettings.builder().requireProofKey(true).build())
            .tokenSettings(tokenSettings)
            .build();

        registeredClientRepository.save(pkcePublicClient);
      }
    };
  }

  @Bean
  JdbcUserDetailsManager userDetailsService(DataSource dataSource) {
    return new JdbcUserDetailsManager(dataSource);
  }

  @Bean
  SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  @Bean
  @Order(2)
  CommandLineRunner demoUserSeeder(
      JdbcUserDetailsManager userDetailsManager,
      PasswordEncoder passwordEncoder) {
    return args -> {
      if (userDetailsManager.userExists("demo-user")) {
        return;
      }

      UserDetails user = User.withUsername("demo-user")
          .password(passwordEncoder.encode("demo-password"))
          .roles("USER")
          .build();

      userDetailsManager.createUser(user);
    };
  }

  @Bean
  @Order(3)
  CommandLineRunner demoUserProfileSeeder(UserProfileRepository userProfileRepository) {
    return args -> {
      if (userProfileRepository.findByUsername("demo-user").isPresent()) {
        return;
      }

      UserProfile userProfile = new UserProfile(
          "demo-user",
          "Demo User",
          "demo-user@example.com",
          true,
          "en-US",
          "Asia/Jakarta",
          "engineering",
          "demo",
          Instant.now().getEpochSecond());

      userProfileRepository.save(userProfile);
    };
  }

  @Bean
  @Order(4)
  CommandLineRunner demoUserProfileAttributeSeeder(
      UserProfileRepository userProfileRepository,
      UserProfileAttributeRepository userProfileAttributeRepository) {
    return args -> {
      UserProfile userProfile = userProfileRepository.findByUsername("demo-user").orElse(null);
      if (userProfile == null) {
        return;
      }

      createAttributeIfMissing(
          userProfile,
          userProfileAttributeRepository,
          "favorite_color",
          "blue");
      createAttributeIfMissing(
          userProfile,
          userProfileAttributeRepository,
          "employee_level",
          "senior");
      createAttributeIfMissing(
          userProfile,
          userProfileAttributeRepository,
          "region",
          "apac");
    };
  }

  @Bean
  @Order(5)
    CommandLineRunner demoClaimInclusionRuleSeeder(
      ClaimInclusionRuleRepository claimInclusionRuleRepository) {
    return args -> {
      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        "favorite_color",
        ClaimTarget.USERINFO);
      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        "favorite_color",
        ClaimTarget.ACCESS_TOKEN);

      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        "employee_level",
        ClaimTarget.ID_TOKEN);
      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        "employee_level",
        ClaimTarget.ACCESS_TOKEN);

      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        "region",
        ClaimTarget.USERINFO);
      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        "region",
        ClaimTarget.ID_TOKEN);
    };
  }

  @Bean
  Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper(
      UserProfileService userProfileService) {
    return (context) -> {
      String username = context.getAuthorization().getPrincipalName();
      UserProfile userProfile = userProfileService.getOrDefault(username);

      Map<String, Object> claims = new LinkedHashMap<>();
      claims.put("sub", username);
      claims.put("preferred_username", username);
      claims.put("name", userProfile.getFullName());
      claims.put("email", userProfile.getEmail());
      claims.put("email_verified", userProfile.isEmailVerified());
      claims.put("locale", userProfile.getLocale());
      claims.put("zoneinfo", userProfile.getZoneinfo());
      claims.put("updated_at", userProfile.getUpdatedAt());
      claims.put("department", userProfile.getDepartment());
      claims.put("tenant", userProfile.getTenant());
      claims.putAll(userProfileService.getUserInfoAttributes(username));

      return new OidcUserInfo(claims);
    };
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
      UserProfileService userProfileService,
      TokenPolicyProperties tokenPolicyProperties) {
    return (context) -> {
      if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
          validateClientCredentialsScopes(context.getAuthorizedScopes(), tokenPolicyProperties);
        }
        return;
      }

      if (context.getAuthorization() == null) {
        return;
      }

      String username = context.getAuthorization().getPrincipalName();
      if (username == null || username.isBlank()) {
        return;
      }

      if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
        context.getClaims().claims((claims) ->
            claims.putAll(userProfileService.getIdTokenAttributes(username)));
        return;
      }

      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        context.getClaims().claims((claims) ->
            claims.putAll(userProfileService.getAccessTokenAttributes(username)));
      }
    };
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = generateRsaKey();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  @Bean
  AuthorizationServerSettings authorizationServerSettings(
      @Value("${app.issuer-uri}") String issuerUri) {
    return AuthorizationServerSettings.builder()
        .issuer(issuerUri)
        .build();
  }

  @Bean
  TokenSettings tokenSettings(TokenPolicyProperties tokenPolicyProperties) {
    return TokenSettings.builder()
        .accessTokenTimeToLive(tokenPolicyProperties.getAccessTokenTimeToLive())
        .refreshTokenTimeToLive(tokenPolicyProperties.getRefreshTokenTimeToLive())
        .reuseRefreshTokens(tokenPolicyProperties.isReuseRefreshTokens())
        .build();
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

  private static RSAKey generateRsaKey() {
    KeyPair keyPair = generateRsaKeyPair();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

    return new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  private static KeyPair generateRsaKeyPair() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      return keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("RSA algorithm is not available", exception);
    }
  }

  private static void createAttributeIfMissing(
      UserProfile userProfile,
      UserProfileAttributeRepository userProfileAttributeRepository,
      String key,
      String value) {
    if (userProfileAttributeRepository.existsByUserProfileUsernameAndAttributeKey(
        userProfile.getUsername(), key)) {
      return;
    }

    userProfileAttributeRepository.save(
        new UserProfileAttribute(
            userProfile,
            key,
            value));
  }

  private static void createRuleTargetIfMissing(
      ClaimInclusionRuleRepository claimInclusionRuleRepository,
      String attributeKey,
      ClaimTarget target) {
    ClaimInclusionRule rule = claimInclusionRuleRepository.findById(attributeKey)
        .orElseGet(() -> claimInclusionRuleRepository.save(new ClaimInclusionRule(attributeKey)));

    if (rule.includesTarget(target)) {
      return;
    }

    rule.addTarget(target);
    claimInclusionRuleRepository.save(rule);
  }
}