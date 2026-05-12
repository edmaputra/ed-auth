package io.github.edmaputra.enhauthserv.application.usecase.revocation;

public record RevokeTokenCommand(
    String token,
    String tokenTypeHint,
    String authorizationHeader) {
}
