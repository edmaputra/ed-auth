package io.github.edmaputra.enhauthserv.application.port.in;

import io.github.edmaputra.enhauthserv.application.usecase.claims.ClaimType;
import io.github.edmaputra.enhauthserv.application.usecase.claims.UserProfileData;
import java.util.Map;

public interface UserClaimsInputPort {

  UserProfileData getOrDefaultProfile(String username);

  Map<String, Object> getClaims(String username, ClaimType claimType);
}
