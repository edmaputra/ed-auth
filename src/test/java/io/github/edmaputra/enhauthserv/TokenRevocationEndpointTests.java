package io.github.edmaputra.enhauthserv;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenRevocationEndpointTests extends AuthServerIntegrationTests {

  @Test
  void revokeValidAccessTokenReturnsOkAndMakesTokenInactive() throws Exception {
    String accessToken = getAccessToken("demo-client", "demo-secret");

    ResponseEntity<String> revocationResponse = revokeToken(accessToken, "access_token");
    assertThat(revocationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> introspectionResponse = introspect(accessToken);
    assertThat(introspectionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode introspectionBody = objectMapper.readTree(introspectionResponse.getBody());
    assertThat(introspectionBody.path("active").asBoolean()).isFalse();
  }

  @Test
  void revokeUnknownTokenStillReturnsOk() {
    ResponseEntity<String> revocationResponse = revokeToken("unknown-token-value", "access_token");
    assertThat(revocationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void revokeWithoutTokenParameterReturnsInvalidRequest() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    ResponseEntity<String> response =
        restTemplate
            .withBasicAuth("demo-client", "demo-secret")
            .postForEntity("/oauth2/revoke", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.path("error").asText()).isEqualTo("invalid_request");
  }

  @Test
  void revokeWithInvalidClientSecretReturnsInvalidClient() throws Exception {
    String accessToken = getAccessToken("demo-client", "demo-secret");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", accessToken);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    ResponseEntity<String> response =
        restTemplate
            .withBasicAuth("demo-client", "wrong-secret")
            .postForEntity("/oauth2/revoke", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.path("error").asText()).isEqualTo("invalid_client");
  }

  private ResponseEntity<String> revokeToken(String token, String tokenTypeHint) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", token);
    if (tokenTypeHint != null && !tokenTypeHint.isBlank()) {
      form.add("token_type_hint", tokenTypeHint);
    }

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    return restTemplate
        .withBasicAuth("demo-client", "demo-secret")
        .postForEntity("/oauth2/revoke", request, String.class);
  }

  private ResponseEntity<String> introspect(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", token);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    return restTemplate
        .withBasicAuth("demo-client", "demo-secret")
        .postForEntity("/oauth2/introspect", request, String.class);
  }

  private String getAccessToken(String clientId, String clientSecret) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("scope", "read write");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    ResponseEntity<String> response =
        restTemplate
            .withBasicAuth(clientId, clientSecret)
            .postForEntity("/oauth2/token", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = objectMapper.readTree(response.getBody());
    return body.path("access_token").asText();
  }
}
