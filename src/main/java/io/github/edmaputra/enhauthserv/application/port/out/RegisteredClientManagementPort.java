package io.github.edmaputra.enhauthserv.application.port.out;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

public interface RegisteredClientManagementPort {

  RegisteredClient findByClientId(String clientId);

  void save(RegisteredClient registeredClient);
}