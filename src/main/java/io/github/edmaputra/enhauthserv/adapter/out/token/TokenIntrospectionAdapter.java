package io.github.edmaputra.enhauthserv.adapter.out.token;

import io.github.edmaputra.enhauthserv.application.port.out.TokenIntrospectionPort;
import io.github.edmaputra.enhauthserv.service.TokenIntrospectionValidator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenIntrospectionAdapter implements TokenIntrospectionPort {

  private final TokenIntrospectionValidator tokenIntrospectionValidator;

  @Override
  public Map<String, Object> introspect(String token) {
    return tokenIntrospectionValidator.introspect(token);
  }
}
