package io.github.edmaputra.enhauthserv.service;

import io.github.edmaputra.enhauthserv.entity.ClaimInclusionRule;
import io.github.edmaputra.enhauthserv.entity.ClaimTarget;
import io.github.edmaputra.enhauthserv.entity.UserProfile;
import io.github.edmaputra.enhauthserv.entity.UserProfileAttribute;
import io.github.edmaputra.enhauthserv.repository.ClaimInclusionRuleRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileAttributeRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileRepository;
import io.github.edmaputra.enhauthserv.tenant.TenantContext;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private static final String DEFAULT_TENANT = "demo";

  private static final Set<String> RESERVED_CLAIMS = Set.of(
      "sub", "iss", "aud", "exp", "iat", "nbf", "jti", "scope", "client_id", "azp", "token_type",
      "auth_time", "nonce", "at_hash", "c_hash", "sid", "amr", "acr");

  private final UserProfileRepository userProfileRepository;
  private final UserProfileAttributeRepository userProfileAttributeRepository;
  private final ClaimInclusionRuleRepository claimInclusionRuleRepository;

  public UserProfile getOrDefault(String username) {
    String tenantId = resolveTenantId();
    return userProfileRepository.findByTenantAndUsername(tenantId, username)
      .orElseGet(() -> defaultProfile(username, tenantId));
  }

  public Map<String, Object> getUserInfoAttributes(String username) {
    return toClaimMap(username, ClaimTarget.USERINFO);
  }

  public Map<String, Object> getIdTokenAttributes(String username) {
    return toClaimMap(username, ClaimTarget.ID_TOKEN);
  }

  public Map<String, Object> getAccessTokenAttributes(String username) {
    return toClaimMap(username, ClaimTarget.ACCESS_TOKEN);
  }

  private static UserProfile defaultProfile(String username, String tenantId) {
    return new UserProfile(
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

  private Map<String, Object> toClaimMap(String username, ClaimTarget target) {
    String tenantId = resolveTenantId();
    List<UserProfileAttribute> attributes =
        userProfileAttributeRepository.findByTenantIdAndUserProfileUsername(tenantId, username);
    if (attributes.isEmpty()) {
      return Map.of();
    }

    Set<String> attributeKeys = attributes.stream()
        .map(UserProfileAttribute::getAttributeKey)
        .filter((key) -> key != null && !key.isBlank())
        .collect(Collectors.toSet());

    Map<String, ClaimInclusionRule> rulesByKey =
      claimInclusionRuleRepository.findByTenantIdAndAttributeKeyIn(tenantId, attributeKeys)
        .stream()
      .collect(Collectors.toMap(ClaimInclusionRule::getAttributeKey, (rule) -> rule));

    Map<String, Object> claims = new LinkedHashMap<>();
    for (UserProfileAttribute attribute : attributes) {
      String key = attribute.getAttributeKey();
      if (key == null || key.isBlank() || RESERVED_CLAIMS.contains(key)) {
        continue;
      }
      ClaimInclusionRule rule = rulesByKey.get(key);
      if (rule == null || !rule.includesTarget(target)) {
        continue;
      }
      claims.put(key, attribute.getAttributeValue());
    }
    return claims;
  }

  private String resolveTenantId() {
    return TenantContext.getCurrentTenantOrDefault(DEFAULT_TENANT);
  }
}