package io.github.edmaputra.enhauthserv.application.port.out;

public interface CurrentTenantPort {

  String currentTenantOrDefault(String defaultTenant);
}
