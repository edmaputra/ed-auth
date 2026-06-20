package io.github.edmaputra.enhauthserv.application.usecase.introspection;

public record IntrospectTokenCommand(String token, String authorizationHeader) {
}
