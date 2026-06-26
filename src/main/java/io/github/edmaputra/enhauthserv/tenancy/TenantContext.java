package io.github.edmaputra.enhauthserv.tenancy;

import java.util.Optional;

/**
 * Request-thread tenant context holder.
 */
public final class TenantContext {

  private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

  private TenantContext() {
  }

  public static void setCurrentTenant(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      clear();
      return;
    }
    CURRENT_TENANT.set(tenantId);
  }

  public static Optional<String> getCurrentTenant() {
    return Optional.ofNullable(CURRENT_TENANT.get());
  }

  public static String getCurrentTenantOrDefault(String fallbackTenant) {
    return getCurrentTenant().orElse(fallbackTenant);
  }

  public static void clear() {
    CURRENT_TENANT.remove();
  }
}