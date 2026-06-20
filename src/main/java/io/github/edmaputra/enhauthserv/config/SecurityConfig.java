package io.github.edmaputra.enhauthserv.config;

import io.github.edmaputra.enhauthserv.entity.ClaimInclusionRule;
import io.github.edmaputra.enhauthserv.entity.ClaimTarget;
import io.github.edmaputra.enhauthserv.entity.UserProfile;
import io.github.edmaputra.enhauthserv.entity.UserProfileAttribute;
import io.github.edmaputra.enhauthserv.application.port.in.RegisteredClientBootstrapInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.UserClaimsInputPort;
import io.github.edmaputra.enhauthserv.application.usecase.claims.ClaimType;
import io.github.edmaputra.enhauthserv.application.usecase.claims.UserProfileData;
import io.github.edmaputra.enhauthserv.repository.ClaimInclusionRuleRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileAttributeRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileRepository;
import io.github.edmaputra.enhauthserv.tenant.TenantAwareOAuth2AuthorizationConsentService;
import io.github.edmaputra.enhauthserv.tenant.TenantAwareOAuth2AuthorizationService;
import io.github.edmaputra.enhauthserv.tenant.TenantAwareRegisteredClientRepository;
import io.github.edmaputra.enhauthserv.tenant.TenantContextFilter;
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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.Ordered;
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
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
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
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(TokenPolicyProperties.class)
public class SecurityConfig {

  private static final String DEMO_TENANT = "demo";

  @Bean
  FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(
      TenantContextFilter tenantContextFilter) {
    FilterRegistrationBean<TenantContextFilter> registrationBean =
        new FilterRegistrationBean<>(tenantContextFilter);
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registrationBean.addUrlPatterns("/*");
    return registrationBean;
  }

  @Bean
  @Order(1)
  SecurityFilterChain tenantMachineEndpointsFilterChain(HttpSecurity http) throws Exception {
    http
      .securityMatcher(new OrRequestMatcher(
          PathPatternRequestMatcher.withDefaults().matcher("/t/{tenant}/oauth2/introspect"),
          PathPatternRequestMatcher.withDefaults().matcher("/t/{tenant}/oauth2/revoke")))
      .authorizeHttpRequests((authorize) -> authorize.anyRequest().permitAll());

    return http.build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain authorizationServerSecurityFilterChain(
      HttpSecurity http,
      Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

    http
        .securityMatcher(
            new OrRequestMatcher(
                authorizationServerConfigurer.getEndpointsMatcher(),
                PathPatternRequestMatcher.withDefaults().matcher("/oauth2/authorize-consent")))
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
  @Order(3)
  SecurityFilterChain introspectionFilterChain(HttpSecurity http) throws Exception {
    // Custom machine-to-machine endpoints handle their own client authentication.
    // This filter chain ensures JSON responses instead of login redirects.
    http
      .securityMatcher(
          "/oauth2/introspect",
          "/oauth2/revoke")
        .authorizeHttpRequests((authorize) -> authorize
            .anyRequest().permitAll()
        );

    return http.build();
  }

  @Bean
  @Order(4)
  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests((authorize) -> authorize
      .requestMatchers("/logged-out", "/t/*/.well-known/openid-configuration", "/t/*/oauth2/jwks")
        .permitAll()
            .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
    return new TenantAwareRegisteredClientRepository(jdbcTemplate);
  }

  @Bean
  OAuth2AuthorizationService authorizationService(
      JdbcTemplate jdbcTemplate,
      RegisteredClientRepository registeredClientRepository) {
    return new TenantAwareOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
  }

  @Bean
  OAuth2AuthorizationConsentService authorizationConsentService(
      JdbcTemplate jdbcTemplate,
      RegisteredClientRepository registeredClientRepository) {
    return new TenantAwareOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
  }

  @Bean
  @Order(1)
  CommandLineRunner demoRegisteredClientSeeder(
      RegisteredClientBootstrapInputPort registeredClientBootstrapInputPort) {
    return args -> registeredClientBootstrapInputPort.ensureDefaultClients();
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
      if (userProfileRepository.findByTenantAndUsername(DEMO_TENANT, "demo-user").isPresent()) {
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
          DEMO_TENANT,
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
      UserProfile userProfile =
          userProfileRepository.findByTenantAndUsername(DEMO_TENANT, "demo-user").orElse(null);
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
        DEMO_TENANT,
        "favorite_color",
        ClaimTarget.USERINFO);
      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        DEMO_TENANT,
        "favorite_color",
        ClaimTarget.ACCESS_TOKEN);

      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        DEMO_TENANT,
        "employee_level",
        ClaimTarget.ID_TOKEN);
      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        DEMO_TENANT,
        "employee_level",
        ClaimTarget.ACCESS_TOKEN);

      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        DEMO_TENANT,
        "region",
        ClaimTarget.USERINFO);
      createRuleTargetIfMissing(
        claimInclusionRuleRepository,
        DEMO_TENANT,
        "region",
        ClaimTarget.ID_TOKEN);
    };
  }

  @Bean
  Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper(
      UserClaimsInputPort userClaimsInputPort) {
    return (context) -> {
      var authorization = context.getAuthorization();
      if (authorization == null) {
        return new OidcUserInfo(Map.of());
      }

      String username = authorization.getPrincipalName();
      UserProfileData userProfile = userClaimsInputPort.getOrDefaultProfile(username);

      Map<String, Object> claims = new LinkedHashMap<>();
      claims.put("sub", username);
      claims.put("preferred_username", username);
      claims.put("name", userProfile.fullName());
      claims.put("email", userProfile.email());
      claims.put("email_verified", userProfile.emailVerified());
      claims.put("locale", userProfile.locale());
      claims.put("zoneinfo", userProfile.zoneinfo());
      claims.put("updated_at", userProfile.updatedAt());
      claims.put("department", userProfile.department());
      claims.put("tenant", userProfile.tenant());
      claims.putAll(userClaimsInputPort.getClaims(username, ClaimType.USERINFO));

      return new OidcUserInfo(claims);
    };
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
      UserClaimsInputPort userClaimsInputPort,
      TokenPolicyProperties tokenPolicyProperties) {
    return (context) -> {
      if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
          validateClientCredentialsScopes(context.getAuthorizedScopes(), tokenPolicyProperties);
        }
        return;
      }

      var authorization = context.getAuthorization();
      if (authorization == null) {
        return;
      }

      String username = authorization.getPrincipalName();
      if (username == null || username.isBlank()) {
        return;
      }

      if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
        context.getClaims().claims((claims) ->
            claims.putAll(userClaimsInputPort.getClaims(username, ClaimType.ID_TOKEN)));
        return;
      }

      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        context.getClaims().claims((claims) ->
            claims.putAll(userClaimsInputPort.getClaims(username, ClaimType.ACCESS_TOKEN)));
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
    if (userProfileAttributeRepository.existsByTenantIdAndUserProfileUsernameAndAttributeKey(
        userProfile.getTenant(), userProfile.getUsername(), key)) {
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
      String tenantId,
      String attributeKey,
      ClaimTarget target) {
    ClaimInclusionRule rule = claimInclusionRuleRepository
        .findByTenantIdAndAttributeKey(tenantId, attributeKey)
        .orElseGet(() -> claimInclusionRuleRepository.save(new ClaimInclusionRule(tenantId, attributeKey)));

    if (rule.includesTarget(target)) {
      return;
    }

    rule.addTarget(target);
    claimInclusionRuleRepository.save(rule);
  }
}