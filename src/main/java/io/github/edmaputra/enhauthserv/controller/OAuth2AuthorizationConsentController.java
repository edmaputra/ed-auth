package io.github.edmaputra.enhauthserv.controller;

import io.github.edmaputra.enhauthserv.application.port.in.AuthorizationConsentInputPort;
import io.github.edmaputra.enhauthserv.application.usecase.consent.CheckConsentCommand;
import io.github.edmaputra.enhauthserv.application.usecase.consent.ConsentDecisionResult;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for handling user consent in the OAuth2 authorization flow.
 *
 * Implements the consent form display and processing for the RFC 6749 authorization code grant.
 * When a user authorizes a client application for specific scopes, they must explicitly grant
 * consent for those scopes (unless they have previously done so).
 *
 * Endpoints:
 * - GET /oauth2/authorize-consent: Display consent form for requested scopes
 * - POST /oauth2/authorize-consent: Process consent approval and redirect back to authorization
 */
@Controller
@RequestMapping("/oauth2")
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthorizationConsentController {

  private final AuthorizationConsentInputPort authorizationConsentInputPort;
  private final RegisteredClientRepository registeredClientRepository;

  /**
   * Displays the user consent form for the requested scopes.
   *
   * This endpoint is called when a user is attempting to authorize a client application
   * for specific scopes but has not previously granted consent.
   *
   * @param clientId the client ID requesting authorization
   * @param requestedScopes the scopes being requested (space-separated)
   * @param redirectUri the redirect URI the client provided
   * @param state the state parameter from the authorization request
   * @param authentication the currently authenticated user
   * @param model the view model for rendering
   * @return the consent form view name
   */
  @GetMapping("/authorize-consent")
  public String consentForm(
      @RequestParam("client_id") String clientId,
      @RequestParam("requested_scopes") String requestedScopes,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("state") String state,
      Authentication authentication,
      Model model) {

    String principalName = authentication.getName();

    // Parse requested scopes from space-separated string
    Set<String> requestedScopesSet = Set.of(requestedScopes.split("\\s+"));

    // Check consent status through use case
    CheckConsentCommand command = new CheckConsentCommand(principalName, clientId, requestedScopesSet);
    ConsentDecisionResult consentResult = authorizationConsentInputPort.checkConsent(command);

    // If consent is not required, redirect back to authorization endpoint
    if (!consentResult.consentRequired()) {
      log.info(
          "User {} has previously authorized client {} for scopes",
          principalName,
          clientId);
      return "redirect:/oauth2/authorize?client_id=" + clientId
          + "&response_type=code"
          + "&redirect_uri=" + redirectUri
          + "&scope=" + requestedScopes.replace(" ", "+")
          + "&state=" + state;
    }

    // Populate model for consent form
    RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
    if (registeredClient == null) {
      log.warn("Consent request for unknown client: {}", clientId);
      model.addAttribute("error", "Unknown client");
      return "consent-error";
    }

    model.addAttribute("clientId", clientId);
    model.addAttribute("clientName", registeredClient.getClientName() != null
        ? registeredClient.getClientName()
        : clientId);
    model.addAttribute("requestedScopes", requestedScopesSet);
    model.addAttribute("redirectUri", redirectUri);
    model.addAttribute("state", state);
    model.addAttribute("principalName", principalName);

    log.info(
        "Displaying consent form for user {} to authorize client {} for scopes: {}",
        principalName,
        clientId,
        requestedScopesSet);

    return "authorize-consent";
  }

  /**
   * Processes user consent approval and redirects back to authorization endpoint.
   *
   * This endpoint is called when the user submits the consent form with approved scopes.
   *
   * @param clientId the client ID requesting authorization
   * @param redirectUri the redirect URI the client provided
   * @param requestedScopes the scopes being requested (space-separated)
   * @param state the state parameter from the authorization request
   * @param approvedScopes the scopes approved by the user (form parameter names)
   * @param authentication the currently authenticated user
   * @return redirect to authorization endpoint with consent approved
   */
  @PostMapping("/authorize-consent")
  public String approveConsent(
      @RequestParam("client_id") String clientId,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("requested_scopes") String requestedScopes,
      @RequestParam("state") String state,
      @RequestParam(name = "scope", required = false) String[] approvedScopes,
      Authentication authentication) {

    String principalName = authentication.getName();
    Set<String> requestedScopesSet = Set.of(requestedScopes.split("\\s+"));

    // Save consent through use case
    CheckConsentCommand command = new CheckConsentCommand(principalName, clientId, requestedScopesSet);
    authorizationConsentInputPort.approveConsent(command);

    log.info(
        "User {} approved client {} for scopes: {}",
        principalName,
        clientId,
        requestedScopesSet);

    // Redirect back to authorization endpoint - the authorization endpoint will now
    // proceed with the authorization code flow since consent has been granted
    return "redirect:/oauth2/authorize?client_id=" + clientId
        + "&response_type=code"
        + "&redirect_uri=" + redirectUri
        + "&scope=" + requestedScopes.replace(" ", "+")
        + "&state=" + state
        + "&consent_approved=true";
  }
}
