package io.github.edmaputra.enhauthserv.application.port.in;

import io.github.edmaputra.enhauthserv.application.usecase.revocation.RevokeTokenCommand;
import io.github.edmaputra.enhauthserv.application.usecase.revocation.RevokeTokenResult;

public interface RevokeTokenInputPort {

  RevokeTokenResult revoke(RevokeTokenCommand command);
}
