package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  @PostMapping("/login")
  public ApiResponse<Map<String, String>> login(@RequestBody LoginRequest req) {
    // Mock token generation for MVP scaffold. Replace with real JWT issuing service in production.
    String token = "mock-jwt-" + UUID.randomUUID();
    return ApiResponse.success(Map.of(
        "accessToken", token,
        "refreshToken", "mock-refresh-" + UUID.randomUUID(),
        "username", req.username()
    ));
  }

  public record LoginRequest(
      @NotBlank String username,
      @NotBlank String password
  ) {}
}
