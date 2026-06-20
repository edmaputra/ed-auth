package io.github.edmaputra.enhauthserv.application.port.in;

import io.github.edmaputra.enhauthserv.application.usecase.introspection.IntrospectTokenCommand;
import io.github.edmaputra.enhauthserv.application.usecase.introspection.IntrospectTokenResult;

public interface IntrospectTokenInputPort {

  IntrospectTokenResult introspect(IntrospectTokenCommand command);
}
