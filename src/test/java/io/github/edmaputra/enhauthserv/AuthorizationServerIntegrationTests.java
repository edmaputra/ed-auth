package io.github.edmaputra.enhauthserv;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationServerIntegrationTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${app.issuer-uri}")
  private String issuerUri;

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
}
