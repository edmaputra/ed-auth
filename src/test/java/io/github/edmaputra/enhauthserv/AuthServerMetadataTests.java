package io.github.edmaputra.enhauthserv;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServerMetadataTests extends AuthServerIntegrationTests {

  @Test
  void oidcMetadataExposesConfiguredIssuerAndCoreEndpoints() throws Exception {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/.well-known/openid-configuration", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.path("issuer").asText()).isEqualTo(issuerUri);
    assertThat(body.path("token_endpoint").asText()).isNotBlank();
    assertThat(body.path("userinfo_endpoint").asText()).isNotBlank();
    assertThat(body.path("end_session_endpoint").asText()).isNotBlank();
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
}
