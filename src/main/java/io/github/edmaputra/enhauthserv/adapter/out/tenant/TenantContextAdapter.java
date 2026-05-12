package io.github.edmaputra.enhauthserv.adapter.out.tenant;

import io.github.edmaputra.enhauthserv.application.port.out.CurrentTenantPort;
import io.github.edmaputra.enhauthserv.tenant.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class TenantContextAdapter implements CurrentTenantPort {

  @Override
  public String currentTenantOrDefault(String defaultTenant) {
    return TenantContext.getCurrentTenantOrDefault(defaultTenant);
  }
}
