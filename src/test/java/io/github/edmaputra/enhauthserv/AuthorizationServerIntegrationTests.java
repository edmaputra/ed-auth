package io.github.edmaputra.enhauthserv;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationServerIntegrationTests {

  private static final Pattern CSRF_INPUT_PATTERN = Pattern.compile(
      "name=\"_csrf\"\\s+type=\"hidden\"\\s+value=\"([^\"]+)\"");

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${app.issuer-uri}")
  private String issuerUri;

  @LocalServerPort
  private int port;

  @Test
  void oidcMetadataExposesConfiguredIssuerAndCoreEndpoints() throws Exception {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/.well-known/openid-configuration", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.path("issuer").asText()).isEqualTo(issuerUri);
    assertThat(body.path("token_endpoint").asText()).isNotBlank();
    assertThat(body.path("jwks_uri").asText()).isNotBlank();
  }

  @Test
  void jwkSetEndpointReturnsAtLeastOneKey() throws Exception {
    ResponseEntity<String> response = restTemplate.getForEntity("/oauth2/jwks", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.path("keys").isArray()).isTrue();
    assertThat(body.path("keys").size()).isGreaterThan(0);
  }

  @Test
  void clientCredentialsGrantReturnsAccessToken() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("scope", "read");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    ResponseEntity<String> response =
        restTemplate
            .withBasicAuth("demo-client", "demo-secret")
            .postForEntity("/oauth2/token", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.path("access_token").asText()).isNotBlank();
    assertThat(body.path("token_type").asText()).isEqualToIgnoringCase("Bearer");
    assertThat(body.path("expires_in").asLong()).isGreaterThan(0);
    assertThat(body.path("scope").asText()).contains("read");
  }

  @Test
  void invalidClientSecretIsRejected() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    ResponseEntity<String> response =
        restTemplate
            .withBasicAuth("demo-client", "wrong-secret")
            .postForEntity("/oauth2/token", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    String responseBody = response.getBody();
    if (responseBody != null && !responseBody.isBlank()) {
      assertThat(responseBody).contains("invalid_client");
    }
  }

  @Test
  void authorizationCodeGrantReturnsAccessToken() throws Exception {
    CookieManager cookieManager = new CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

    HttpClient client = HttpClient.newBuilder()
      .cookieHandler(cookieManager)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    String redirectUri = "http://127.0.0.1:9000/login/oauth2/code/demo-client";
    String authorizePath = "/oauth2/authorize"
        + "?response_type=code"
        + "&client_id=demo-client"
        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        + "&scope=" + URLEncoder.encode("read", StandardCharsets.UTF_8)
        + "&state=test-state";

    HttpResponse<String> authorizeResponse = client.send(
        HttpRequest.newBuilder()
            .uri(appUri(authorizePath))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertThat(authorizeResponse.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    String loginLocation = authorizeResponse.headers().firstValue("location").orElseThrow();
    assertThat(loginLocation).contains("/login");

    HttpResponse<String> loginPageResponse = client.send(
        HttpRequest.newBuilder()
            .uri(appUri(loginLocation))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertThat(loginPageResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
    String csrfToken = extractCsrfToken(loginPageResponse.body());

    String loginRequestBody = "username=" + urlEncode("demo-user")
        + "&password=" + urlEncode("demo-password")
        + "&_csrf=" + urlEncode(csrfToken);

    HttpResponse<String> loginSubmitResponse = client.send(
        HttpRequest.newBuilder()
            .uri(appUri("/login"))
            .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(loginRequestBody))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertThat(loginSubmitResponse.statusCode()).isEqualTo(HttpStatus.FOUND.value());

    String callbackLocation = null;
    String nextLocation = loginSubmitResponse.headers().firstValue("location").orElseThrow();
    for (int i = 0; i < 5; i++) {
      HttpResponse<String> authorizationStepResponse = client.send(
        HttpRequest.newBuilder()
          .uri(appUri(nextLocation))
          .GET()
          .build(),
        HttpResponse.BodyHandlers.ofString());

      assertThat(authorizationStepResponse.statusCode()).isEqualTo(HttpStatus.FOUND.value());

      String stepLocation = authorizationStepResponse.headers().firstValue("location").orElseThrow();
      if (stepLocation.startsWith(redirectUri)) {
      callbackLocation = stepLocation;
      break;
      }
      nextLocation = stepLocation;
    }

    assertThat(callbackLocation).isNotNull();

    URI callbackUri = URI.create(callbackLocation);
    assertThat(callbackUri.getHost()).isIn("127.0.0.1", "localhost");
    assertThat(callbackUri.getQuery()).contains("code=");

    String authorizationCode = extractQueryParam(callbackUri, "code");
    assertThat(authorizationCode).isNotBlank();

    HttpHeaders tokenHeaders = new HttpHeaders();
    tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> tokenForm = new LinkedMultiValueMap<>();
    tokenForm.add("grant_type", "authorization_code");
    tokenForm.add("code", authorizationCode);
    tokenForm.add("redirect_uri", redirectUri);

    HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenForm, tokenHeaders);

    ResponseEntity<String> tokenResponse =
        restTemplate
            .withBasicAuth("demo-client", "demo-secret")
            .postForEntity("/oauth2/token", tokenRequest, String.class);

    assertThat(tokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode tokenBody = objectMapper.readTree(tokenResponse.getBody());
    assertThat(tokenBody.path("access_token").asText()).isNotBlank();
    assertThat(tokenBody.path("token_type").asText()).isEqualToIgnoringCase("Bearer");
    assertThat(tokenBody.path("expires_in").asLong()).isGreaterThan(0);
    assertThat(tokenBody.path("scope").asText()).contains("read");
  }

  private URI appUri(String pathOrAbsoluteUrl) {
    if (pathOrAbsoluteUrl.startsWith("http://") || pathOrAbsoluteUrl.startsWith("https://")) {
      return URI.create(pathOrAbsoluteUrl);
    }
    String normalizedPath = pathOrAbsoluteUrl.startsWith("/")
        ? pathOrAbsoluteUrl
        : "/" + pathOrAbsoluteUrl;
    return URI.create("http://localhost:" + port + normalizedPath);
  }

  private String extractCsrfToken(String html) {
    Matcher matcher = CSRF_INPUT_PATTERN.matcher(html);
    if (!matcher.find()) {
      throw new IllegalStateException("Unable to extract CSRF token from login page");
    }
    return matcher.group(1);
  }

  private String extractQueryParam(URI uri, String key) {
    String query = uri.getQuery();
    if (query == null || query.isBlank()) {
      throw new IllegalStateException("Expected query parameters in URI: " + uri);
    }

    for (String pair : query.split("&")) {
      String[] split = pair.split("=", 2);
      String currentKey = java.net.URLDecoder.decode(split[0], StandardCharsets.UTF_8);
      String currentValue = split.length > 1
          ? java.net.URLDecoder.decode(split[1], StandardCharsets.UTF_8)
          : "";
      if (key.equals(currentKey)) {
        return currentValue;
      }
    }

    throw new IllegalStateException("Missing query parameter '" + key + "' in URI: " + uri);
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
