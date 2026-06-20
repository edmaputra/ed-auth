package io.github.edmaputra.enhauthserv.application.usecase.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolveTenantUseCaseTests {

  @Test
  void headerTenantOverridesPathTenant() {
    ResolveTenantUseCase useCase = new ResolveTenantUseCase(defaultPolicy(false));

    TenantResolutionResult result = useCase.resolve(
        "/t/demo/oauth2/introspect",
        "tenant-b",
        "127.0.0.1");

    assertThat(result.tenantId()).contains("tenant-b");
    assertThat(result.tenantSource()).isEqualTo(TenantResolutionResult.TenantSource.HEADER);
    assertThat(result.rewrittenPath()).contains("/oauth2/introspect");
    assertThat(result.invalidRequest()).isFalse();
  }

  @Test
  void strictModeWithoutTenantMarksInvalidRequest() {
    ResolveTenantUseCase useCase = new ResolveTenantUseCase(defaultPolicy(true));

    TenantResolutionResult result = useCase.resolve("/oauth2/introspect", null, "127.0.0.1");

    assertThat(result.tenantId()).isEmpty();
    assertThat(result.invalidRequest()).isTrue();
  }

  @Test
  void untrustedHeaderFallsBackToPathWhenEnforced() {
    ResolveTenantUseCase useCase = new ResolveTenantUseCase(
        new TenantResolutionPolicy(true, true, false, true, Set.of("127.0.0.1")));

    TenantResolutionResult result = useCase.resolve(
        "/t/demo/oauth2/introspect",
        "tenant-b",
        "10.10.10.10");

    assertThat(result.tenantId()).contains("demo");
    assertThat(result.tenantSource()).isEqualTo(TenantResolutionResult.TenantSource.PATH);
    assertThat(result.invalidRequest()).isFalse();
  }

  @Test
  void invalidHeaderFallsBackToPath() {
    ResolveTenantUseCase useCase = new ResolveTenantUseCase(defaultPolicy(false));

    TenantResolutionResult result = useCase.resolve(
        "/t/demo/oauth2/introspect",
        "tenant bad",
        "127.0.0.1");

    assertThat(result.tenantId()).contains("demo");
    assertThat(result.tenantSource()).isEqualTo(TenantResolutionResult.TenantSource.PATH);
  }

  @Test
  void machineEndpointPathIsRewritten() {
    ResolveTenantUseCase useCase = new ResolveTenantUseCase(defaultPolicy(false));

    TenantResolutionResult result = useCase.resolve(
        "/t/demo/oauth2/revoke",
        null,
        "127.0.0.1");

    assertThat(result.tenantId()).contains("demo");
    assertThat(result.rewrittenPath()).contains("/oauth2/revoke");
  }

  private TenantResolutionPolicy defaultPolicy(boolean requireExplicitTenant) {
    return new TenantResolutionPolicy(true, true, requireExplicitTenant, false, Set.of("127.0.0.1"));
  }
}
