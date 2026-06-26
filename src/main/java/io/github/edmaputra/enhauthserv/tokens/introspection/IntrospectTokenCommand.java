package io.github.edmaputra.enhauthserv.tokens.introspection;

public record IntrospectTokenCommand(String token, String authorizationHeader) {
}
