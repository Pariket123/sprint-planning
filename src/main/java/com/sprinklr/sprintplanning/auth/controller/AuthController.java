package com.sprinklr.sprintplanning.auth.controller;

import com.sprinklr.sprintplanning.auth.dto.AuthUserResponse;
import com.sprinklr.sprintplanning.auth.service.AuthService;
import com.sprinklr.sprintplanning.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Microsoft Entra ID authentication")
@SecurityRequirement(name = "bearer-jwt")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/me")
  @Operation(summary = "Get the authenticated user profile from the JWT")
  public ResponseEntity<ApiResponse<AuthUserResponse>> getCurrentUser(Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.ok(authService.getCurrentUser(authentication)));
  }
}
