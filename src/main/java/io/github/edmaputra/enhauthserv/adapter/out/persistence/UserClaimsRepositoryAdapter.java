package io.github.edmaputra.enhauthserv.adapter.out.persistence;

import io.github.edmaputra.enhauthserv.application.usecase.claims.ClaimType;
import io.github.edmaputra.enhauthserv.application.usecase.claims.UserClaimsDataPort;
import io.github.edmaputra.enhauthserv.application.usecase.claims.UserAttributeData;
import io.github.edmaputra.enhauthserv.application.usecase.claims.UserProfileData;
import io.github.edmaputra.enhauthserv.entity.ClaimInclusionRule;
import io.github.edmaputra.enhauthserv.entity.ClaimTarget;
import io.github.edmaputra.enhauthserv.entity.UserProfile;
import io.github.edmaputra.enhauthserv.repository.ClaimInclusionRuleRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileAttributeRepository;
import io.github.edmaputra.enhauthserv.repository.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserClaimsRepositoryAdapter implements UserClaimsDataPort {

  private final UserProfileRepository userProfileRepository;
  private final UserProfileAttributeRepository userProfileAttributeRepository;
  private final ClaimInclusionRuleRepository claimInclusionRuleRepository;

  @Override
  public Optional<UserProfileData> findUserProfile(String tenantId, String username) {
    return userProfileRepository.findByTenantAndUsername(tenantId, username)
        .map(this::toUserProfileData);
  }

  @Override
  public List<UserAttributeData> findUserAttributes(String tenantId, String username) {
    return userProfileAttributeRepository.findByTenantIdAndUserProfileUsername(tenantId, username)
        .stream()
        .map((attribute) -> new UserAttributeData(
            attribute.getAttributeKey(),
            attribute.getAttributeValue()))
        .toList();
  }

  @Override
  public Set<String> findIncludedAttributeKeys(
      String tenantId,
      Set<String> attributeKeys,
      ClaimType claimType) {
    ClaimTarget target = toClaimTarget(claimType);
    return claimInclusionRuleRepository.findByTenantIdAndAttributeKeyIn(tenantId, attributeKeys)
        .stream()
        .filter((rule) -> rule.includesTarget(target))
        .map(ClaimInclusionRule::getAttributeKey)
        .collect(Collectors.toSet());
  }

  private UserProfileData toUserProfileData(UserProfile profile) {
    return new UserProfileData(
        profile.getUsername(),
        profile.getFullName(),
        profile.getEmail(),
        profile.isEmailVerified(),
        profile.getLocale(),
        profile.getZoneinfo(),
        profile.getDepartment(),
        profile.getTenant(),
        profile.getUpdatedAt());
  }

  private ClaimTarget toClaimTarget(ClaimType claimType) {
    return switch (claimType) {
      case USERINFO -> ClaimTarget.USERINFO;
      case ID_TOKEN -> ClaimTarget.ID_TOKEN;
      case ACCESS_TOKEN -> ClaimTarget.ACCESS_TOKEN;
    };
  }
}
