package io.github.edmaputra.enhauthserv.application.port.out;

import java.util.Map;

public interface TokenIntrospectionPort {

  Map<String, Object> introspect(String token);
}
