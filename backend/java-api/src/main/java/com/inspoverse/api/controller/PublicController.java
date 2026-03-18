package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

  @GetMapping("/rules")
  public ApiResponse<Map<String, String>> rules() {
    return ApiResponse.success(Map.of("content", "请遵守社区规范，文明发言。"));
  }

  @GetMapping("/contact")
  public ApiResponse<Map<String, String>> contact() {
    return ApiResponse.success(Map.of("email", "support@inspo-verse.com"));
  }
}
