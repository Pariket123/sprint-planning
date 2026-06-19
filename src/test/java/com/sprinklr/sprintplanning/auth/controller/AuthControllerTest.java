package com.sprinklr.sprintplanning.auth.controller;

import com.sprinklr.sprintplanning.TestSecurityConfig;
import com.sprinklr.sprintplanning.auth.dto.AuthUserResponse;
import com.sprinklr.sprintplanning.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({TestSecurityConfig.class, com.sprinklr.sprintplanning.common.handler.GlobalExceptionHandler.class})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AuthService authService;

  @Test
  void getCurrentUserReturnsEnvelope() throws Exception {
    when(authService.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new AuthUserResponse("user@sprinklr.com", "Test User", "oid-123"));

    mockMvc.perform(get("/api/v1/auth/me")
            .with(jwt().jwt(builder -> builder
                .claim("oid", "oid-123")
                .claim("email", "user@sprinklr.com")
                .claim("name", "Test User"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value("user@sprinklr.com"))
        .andExpect(jsonPath("$.data.oid").value("oid-123"));
  }

  @Test
  void getCurrentUserWithoutJwtReturnsUnauthorized() throws Exception {
    when(authService.getCurrentUser(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new com.sprinklr.sprintplanning.common.exception.ApiException(
            "UNAUTHORIZED", "Authentication required", org.springframework.http.HttpStatus.UNAUTHORIZED));

    mockMvc.perform(get("/api/v1/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }
}
