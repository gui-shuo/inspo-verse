package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me() {
    return ApiResponse.success(Map.of(
        "id", 10001,
        "username", "demo_user",
        "nickname", "Demo",
        "vipLevel", "silver"
    ));
  }
}
