package io.github.edmaputra.enhauthserv.application.usecase.claims;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserClaimsDataPort {

  Optional<UserProfileData> findUserProfile(String tenantId, String username);

  List<UserAttributeData> findUserAttributes(String tenantId, String username);

  Set<String> findIncludedAttributeKeys(
      String tenantId,
      Set<String> attributeKeys,
      ClaimType claimType);
}

