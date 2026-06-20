package io.github.edmaputra.enhauthserv.adapter.out.security;

import io.github.edmaputra.enhauthserv.application.port.out.RegisteredClientManagementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisteredClientRepositoryAdapter implements RegisteredClientManagementPort {

  private final RegisteredClientRepository registeredClientRepository;

  @Override
  public RegisteredClient findByClientId(String clientId) {
    return registeredClientRepository.findByClientId(clientId);
  }

  @Override
  public void save(RegisteredClient registeredClient) {
    registeredClientRepository.save(registeredClient);
  }
}