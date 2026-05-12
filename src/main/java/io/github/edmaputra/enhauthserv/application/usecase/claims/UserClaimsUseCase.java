package io.github.edmaputra.enhauthserv.application.usecase.claims;

import io.github.edmaputra.enhauthserv.application.port.in.UserClaimsInputPort;
import io.github.edmaputra.enhauthserv.application.port.out.CurrentTenantPort;
import io.github.edmaputra.enhauthserv.application.port.out.UserClaimsDataPort;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserClaimsUseCase implements UserClaimsInputPort {

  private static final String DEFAULT_TENANT = "demo";

  private static final Set<String> RESERVED_CLAIMS = Set.of(
      "sub", "iss", "aud", "exp", "iat", "nbf", "jti", "scope", "client_id", "azp", "token_type",
      "auth_time", "nonce", "at_hash", "c_hash", "sid", "amr", "acr");

  private final CurrentTenantPort currentTenantPort;
  private final UserClaimsDataPort userClaimsDataPort;

  public UserClaimsUseCase(
      CurrentTenantPort currentTenantPort,
      UserClaimsDataPort userClaimsDataPort) {
    this.currentTenantPort = currentTenantPort;
    this.userClaimsDataPort = userClaimsDataPort;
  }

  @Override
  public UserProfileData getOrDefaultProfile(String username) {
    String tenantId = resolveTenantId();
    return userClaimsDataPort.findUserProfile(tenantId, username)
        .orElseGet(() -> defaultProfile(username, tenantId));
  }

  @Override
  public Map<String, Object> getClaims(String username, ClaimType claimType) {
    String tenantId = resolveTenantId();
    List<UserAttributeData> attributes = userClaimsDataPort.findUserAttributes(tenantId, username);
    if (attributes.isEmpty()) {
      return Map.of();
    }

    Set<String> attributeKeys = attributes.stream()
        .map(UserAttributeData::key)
        .filter((key) -> key != null && !key.isBlank())
        .collect(Collectors.toSet());

    Set<String> includedKeys = userClaimsDataPort.findIncludedAttributeKeys(
        tenantId,
        attributeKeys,
        claimType);

    Map<String, Object> claims = new LinkedHashMap<>();
    for (UserAttributeData attribute : attributes) {
      String key = attribute.key();
      if (key == null || key.isBlank() || RESERVED_CLAIMS.contains(key)) {
        continue;
      }

      if (!includedKeys.contains(key)) {
        continue;
      }

      claims.put(key, attribute.value());
    }

    return claims;
  }

  private static UserProfileData defaultProfile(String username, String tenantId) {
    return new UserProfileData(
        username,
        username,
        username + "@example.com",
        false,
        "en-US",
        "UTC",
        "unknown",
        tenantId,
        Instant.now().getEpochSecond());
  }

  private String resolveTenantId() {
    return currentTenantPort.currentTenantOrDefault(DEFAULT_TENANT);
  }
}
