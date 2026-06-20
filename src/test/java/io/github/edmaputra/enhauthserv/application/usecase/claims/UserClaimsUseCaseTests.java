package io.github.edmaputra.enhauthserv.application.usecase.claims;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.edmaputra.enhauthserv.application.port.out.CurrentTenantPort;
import io.github.edmaputra.enhauthserv.application.port.out.UserClaimsDataPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserClaimsUseCaseTests {

  @Test
  void returnsDefaultProfileWhenProfileIsMissing() {
    CurrentTenantPort tenantPort = (defaultTenant) -> "demo";
    UserClaimsDataPort dataPort = new UserClaimsDataPort() {
      @Override
      public Optional<UserProfileData> findUserProfile(String tenantId, String username) {
        return Optional.empty();
      }

      @Override
      public List<UserAttributeData> findUserAttributes(String tenantId, String username) {
        return List.of();
      }

      @Override
      public Set<String> findIncludedAttributeKeys(
          String tenantId,
          Set<String> attributeKeys,
          ClaimType claimType) {
        return Set.of();
      }
    };

    UserClaimsUseCase useCase = new UserClaimsUseCase(tenantPort, dataPort);

    UserProfileData profile = useCase.getOrDefaultProfile("demo-user");

    assertThat(profile.username()).isEqualTo("demo-user");
    assertThat(profile.tenant()).isEqualTo("demo");
    assertThat(profile.email()).isEqualTo("demo-user@example.com");
  }

  @Test
  void filtersReservedAndNonIncludedClaims() {
    CurrentTenantPort tenantPort = (defaultTenant) -> "demo";
    UserClaimsDataPort dataPort = new UserClaimsDataPort() {
      @Override
      public Optional<UserProfileData> findUserProfile(String tenantId, String username) {
        return Optional.empty();
      }

      @Override
      public List<UserAttributeData> findUserAttributes(String tenantId, String username) {
        return List.of(
            new UserAttributeData("favorite_color", "blue"),
            new UserAttributeData("scope", "read"),
            new UserAttributeData("region", "apac"));
      }

      @Override
      public Set<String> findIncludedAttributeKeys(
          String tenantId,
          Set<String> attributeKeys,
          ClaimType claimType) {
        return Set.of("favorite_color");
      }
    };

    UserClaimsUseCase useCase = new UserClaimsUseCase(tenantPort, dataPort);

    Map<String, Object> claims = useCase.getClaims("demo-user", ClaimType.ACCESS_TOKEN);

    assertThat(claims)
        .containsEntry("favorite_color", "blue")
        .doesNotContainKey("scope")
        .doesNotContainKey("region");
  }
}
