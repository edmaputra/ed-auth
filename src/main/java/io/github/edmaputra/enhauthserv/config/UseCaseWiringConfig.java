package io.github.edmaputra.enhauthserv.config;

import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationConsentInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationPolicyInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.IntrospectTokenInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.RegisteredClientBootstrapInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.RevokeTokenInputPort;
import io.github.edmaputra.enhauthserv.application.port.in.UserClaimsInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.ClientAuthenticationPort;
import io.github.edmaputra.enhauthserv.application.port.out.ConsentStoragePort;
import io.github.edmaputra.enhauthserv.application.port.out.CurrentTenantPort;
import io.github.edmaputra.enhauthserv.application.port.out.RegisteredClientManagementPort;
import io.github.edmaputra.enhauthserv.application.port.out.ScopeValidationPort;
import io.github.edmaputra.enhauthserv.application.port.out.TokenIntrospectionPort;
import io.github.edmaputra.enhauthserv.application.port.out.TokenRevocationPort;
import io.github.edmaputra.enhauthserv.application.port.out.UserClaimsDataPort;
import io.github.edmaputra.enhauthserv.application.usecase.authorization.AuthorizationPolicyUseCase;
import io.github.edmaputra.enhauthserv.application.usecase.claims.UserClaimsUseCase;
import io.github.edmaputra.enhauthserv.application.usecase.consent.AuthorizationConsentUseCase;
import io.github.edmaputra.enhauthserv.application.usecase.introspection.IntrospectTokenUseCase;
import io.github.edmaputra.enhauthserv.application.usecase.registration.DefaultRegisteredClientBootstrapUseCase;
import io.github.edmaputra.enhauthserv.application.usecase.revocation.RevokeTokenUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Configuration
public class UseCaseWiringConfig {

  @Bean
  RegisteredClientBootstrapInputPort registeredClientBootstrapInputPort(
      RegisteredClientManagementPort registeredClientManagementPort,
      PasswordEncoder passwordEncoder,
      TokenSettings tokenSettings) {
    return new DefaultRegisteredClientBootstrapUseCase(
        registeredClientManagementPort,
        passwordEncoder,
        tokenSettings);
  }

  @Bean
  AuthorizationConsentInputPort authorizationConsentInputPort(
      ConsentStoragePort consentStoragePort) {
    return new AuthorizationConsentUseCase(consentStoragePort);
  }

  @Bean
  AuthorizationPolicyInputPort authorizationPolicyInputPort(
      ScopeValidationPort scopeValidationPort) {
    return new AuthorizationPolicyUseCase(scopeValidationPort);
  }

  @Bean
  IntrospectTokenInputPort introspectTokenInputPort(
      ClientAuthenticationPort clientAuthenticationPort,
      TokenIntrospectionPort tokenIntrospectionPort,
      AuthorizationPolicyInputPort authorizationPolicyInputPort) {
    return new IntrospectTokenUseCase(
        clientAuthenticationPort,
        tokenIntrospectionPort,
        authorizationPolicyInputPort);
  }

  @Bean
  RevokeTokenInputPort revokeTokenInputPort(
      ClientAuthenticationPort clientAuthenticationPort,
      TokenRevocationPort tokenRevocationPort,
      AuthorizationPolicyInputPort authorizationPolicyInputPort) {
    return new RevokeTokenUseCase(
        clientAuthenticationPort,
        tokenRevocationPort,
        authorizationPolicyInputPort);
  }

  @Bean
  UserClaimsInputPort userClaimsInputPort(
      CurrentTenantPort currentTenantPort,
      UserClaimsDataPort userClaimsDataPort) {
    return new UserClaimsUseCase(currentTenantPort, userClaimsDataPort);
  }
}
