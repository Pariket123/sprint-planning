package com.sprinklr.sprintplanning.auth.service;

import com.sprinklr.sprintplanning.auth.dto.AuthUserResponse;
import com.sprinklr.sprintplanning.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  public AuthUserResponse getCurrentUser(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
      throw new ApiException("UNAUTHORIZED", "Authentication required", HttpStatus.UNAUTHORIZED);
    }

    Jwt jwt = jwtAuthentication.getToken();
    String email = firstNonBlank(
        jwt.getClaimAsString("email"),
        jwt.getClaimAsString("preferred_username"),
        jwt.getClaimAsString("upn"));
    String name = firstNonBlank(jwt.getClaimAsString("name"), email);
    String oid = jwt.getClaimAsString("oid");

    if (oid == null || oid.isBlank()) {
      throw new ApiException("INVALID_TOKEN", "JWT is missing required oid claim", HttpStatus.UNAUTHORIZED);
    }

    return new AuthUserResponse(email, name, oid);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
