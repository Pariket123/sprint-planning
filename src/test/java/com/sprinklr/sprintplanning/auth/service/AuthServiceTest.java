package com.sprinklr.sprintplanning.auth.service;

import com.sprinklr.sprintplanning.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

  private final AuthService authService = new AuthService();

  @Test
  void extractsUserFromJwtClaims() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("oid", "oid-123")
        .claim("email", "user@sprinklr.com")
        .claim("name", "Test User")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();

    var response = authService.getCurrentUser(new JwtAuthenticationToken(jwt));

    assertThat(response.oid()).isEqualTo("oid-123");
    assertThat(response.email()).isEqualTo("user@sprinklr.com");
    assertThat(response.name()).isEqualTo("Test User");
  }

  @Test
  void fallsBackToPreferredUsernameForEmail() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("oid", "oid-456")
        .claim("preferred_username", "user@sprinklr.com")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();

    var response = authService.getCurrentUser(new JwtAuthenticationToken(jwt));

    assertThat(response.email()).isEqualTo("user@sprinklr.com");
    assertThat(response.name()).isEqualTo("user@sprinklr.com");
  }

  @Test
  void rejectsMissingOidClaim() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("email", "user@sprinklr.com")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();

    assertThatThrownBy(() -> authService.getCurrentUser(new JwtAuthenticationToken(jwt)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("oid");
  }

  @Test
  void rejectsNonJwtAuthentication() {
    assertThatThrownBy(() -> authService.getCurrentUser(
        new UsernamePasswordAuthenticationToken("user", "password")))
        .isInstanceOf(ApiException.class)
        .extracting("code")
        .isEqualTo("UNAUTHORIZED");
  }
}
