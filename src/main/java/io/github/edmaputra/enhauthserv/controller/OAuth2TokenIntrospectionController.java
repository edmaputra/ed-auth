package io.github.edmaputra.enhauthserv.controller;

import io.github.edmaputra.enhauthserv.service.IntrospectionAuthorizationService;
import io.github.edmaputra.enhauthserv.service.TokenIntrospectionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST Controller for RFC 7662 OAuth 2.0 Token Introspection.
 *
 * Endpoint: POST /oauth2/introspect
 * Authentication: HTTP Basic Auth (client_id:client_secret)
 * Request Parameters: token (required)
 * Response: JSON with RFC 7662 fields (active, scope, client_id, etc.)
 */
@RestController
@RequestMapping("/oauth2/introspect")
@Slf4j
@RequiredArgsConstructor
public class OAuth2TokenIntrospectionController {

    private final TokenIntrospectionValidator tokenValidator;
    private final IntrospectionAuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * RFC 7662 Token Introspection endpoint.
     *
     * @param token the token to introspect (required)
     * @param request the HTTP request (for extracting client credentials and IP address)
     * @return RFC 7662 introspection response
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> introspect(
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request) {

        log.debug("Received token introspection request");

        // Step 0: Verify token parameter is present
        if (token == null || token.isEmpty()) {
            log.warn("Token introspection request without token parameter");
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "invalid_request",
                    "Missing required parameter: token");
        }

        // Step 1: Extract and validate client credentials from Basic Auth
        String[] clientCredentials = extractClientCredentials(request);
        if (clientCredentials == null) {
            log.warn("Token introspection request without valid Basic Auth credentials");
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_client",
                    "Client authentication failed");
        }

        String clientId = clientCredentials[0];
        String clientSecret = clientCredentials[1];

        // Step 2: Authenticate the client
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            log.warn("Token introspection attempt with unknown client: {}", clientId);
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_client",
                    "Client authentication failed");
        }

        if (!isClientSecretValid(registeredClient, clientSecret)) {
            log.warn("Token introspection attempt with invalid secret for client: {}", clientId);
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "invalid_client",
                    "Client authentication failed");
        }

        // Step 3: Check if client is authorized to introspect tokens
        if (!authorizationService.canIntrospect(registeredClient)) {
            log.warn("Client {} attempted introspection without required scope", clientId);
            return buildErrorResponse(HttpStatus.FORBIDDEN, "unauthorized_client",
                    "Client is not authorized to introspect tokens (missing scope: " + 
                    authorizationService.getIntrospectionScope() + ")");
        }

        // Step 4: Validate the token and extract claims
        Map<String, Object> introspectionResponse = tokenValidator.introspect(token);
        boolean isActive = (Boolean) introspectionResponse.getOrDefault("active", false);

        log.info("Token introspection completed for client: {} - Active: {}", clientId, isActive);

        // Step 5: Return RFC 7662 response
        return ResponseEntity.ok(introspectionResponse);
    }

    /**
     * Extracts client credentials from HTTP Basic Authentication header.
     *
     * @param request the HTTP request
     * @return array of [clientId, clientSecret], or null if not present or invalid
     */
    private String[] extractClientCredentials(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return null;
        }

        try {
            String credentials = new String(
                    Base64.getDecoder().decode(authHeader.substring(6)),
                    StandardCharsets.UTF_8);
            int colonIndex = credentials.indexOf(':');
            if (colonIndex == -1) {
                return null;
            }

            String clientId = credentials.substring(0, colonIndex);
            String clientSecret = credentials.substring(colonIndex + 1);
            return new String[]{clientId, clientSecret};
        } catch (Exception e) {
            log.debug("Invalid Basic Auth header format", e);
            return null;
        }
    }

    /**
     * Verifies the client secret matches the registered client's secret.
     *
     * Uses Spring Security's PasswordEncoder for secure comparison.
     *
     * @param registeredClient the registered client
     * @param providedSecret the client secret provided in the request
     * @return true if the secret matches, false otherwise
     */
    private boolean isClientSecretValid(RegisteredClient registeredClient, String providedSecret) {
        String registeredSecret = registeredClient.getClientSecret();
        if (registeredSecret == null) {
            // Public clients don't have secrets
            return providedSecret == null || providedSecret.isEmpty();
        }
        
        // Use PasswordEncoder for secure comparison
        return passwordEncoder.matches(providedSecret, registeredSecret);
    }

    /**
     * Builds an error response according to RFC 7662.
     *
     * @param status the HTTP status code
     * @param error the error code
     * @param errorDescription the error description
     * @return ResponseEntity with error details
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String errorDescription) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("error_description", errorDescription);
        return new ResponseEntity<>(errorResponse, status);
    }
}
