package io.github.edmaputra.enhauthserv.service;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service for determining if a client is authorized to introspect tokens.
 *
 * Clients must have the "introspection" scope to be authorized to call the token introspection endpoint.
 */
@Service
public class IntrospectionAuthorizationService {

    private static final String INTROSPECTION_SCOPE = "introspection";

    /**
     * Checks if a client is authorized to perform token introspection.
     *
     * A client is authorized if:
     * 1. It is not null
     * 2. It has the "introspection" scope registered
     * 3. The scope has not been revoked
     *
     * @param registeredClient the registered client to check
     * @return true if the client is authorized to introspect tokens, false otherwise
     */
    public boolean canIntrospect(RegisteredClient registeredClient) {
        if (registeredClient == null) {
            return false;
        }

        Set<String> clientScopes = registeredClient.getScopes();
        if (clientScopes == null || clientScopes.isEmpty()) {
            return false;
        }

        return clientScopes.contains(INTROSPECTION_SCOPE);
    }

    /**
     * Returns the required scope for token introspection.
     *
     * @return the introspection scope
     */
    public String getIntrospectionScope() {
        return INTROSPECTION_SCOPE;
    }
}
