package io.github.edmaputra.enhauthserv.tokens.revocation;

public record RevokeTokenCommand(
    String token,
    String tokenTypeHint,
    String authorizationHeader) {
}
