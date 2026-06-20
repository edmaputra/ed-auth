package io.github.edmaputra.enhauthserv.application.usecase.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import io.github.edmaputra.enhauthserv.application.port.out.RegisteredClientManagementPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@ExtendWith(MockitoExtension.class)
class DefaultRegisteredClientBootstrapUseCaseTests {

  @Mock
  private RegisteredClientManagementPort registeredClientManagementPort;

  @Mock
  private PasswordEncoder passwordEncoder;

  private DefaultRegisteredClientBootstrapUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new DefaultRegisteredClientBootstrapUseCase(
        registeredClientManagementPort,
        passwordEncoder,
        TokenSettings.builder().build());
  }

  @Test
  void ensureDefaultClientsSeedsBothClientsWhenTheyDoNotExist() {
    when(registeredClientManagementPort.findByClientId("demo-client")).thenReturn(null);
    when(registeredClientManagementPort.findByClientId("pkce-public-client")).thenReturn(null);
    when(passwordEncoder.encode("demo-secret")).thenReturn("encoded-demo-secret");

    useCase.ensureDefaultClients();

    ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
    verify(registeredClientManagementPort, times(2)).save(captor.capture());

    List<RegisteredClient> savedClients = captor.getAllValues();
    assertThat(savedClients).hasSize(2);
    assertThat(savedClients.get(0).getClientId()).isEqualTo("demo-client");
    assertThat(savedClients.get(0).getClientSecret()).isEqualTo("encoded-demo-secret");
    assertThat(savedClients.get(1).getClientId()).isEqualTo("pkce-public-client");
  }

  @Test
  void ensureDefaultClientsSkipsAlreadyProvisionedClients() {
    RegisteredClient existingClient = RegisteredClient.withId("existing-id")
        .clientId("demo-client")
        .clientSecret("encoded-demo-secret")
        .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
        .build();

    when(registeredClientManagementPort.findByClientId("demo-client")).thenReturn(existingClient);
    when(registeredClientManagementPort.findByClientId("pkce-public-client")).thenReturn(existingClient);

    useCase.ensureDefaultClients();

    verifyNoInteractions(passwordEncoder);
    verify(registeredClientManagementPort).findByClientId("demo-client");
    verify(registeredClientManagementPort).findByClientId("pkce-public-client");
  }
}