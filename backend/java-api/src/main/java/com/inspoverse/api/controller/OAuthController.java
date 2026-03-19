package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.UserOAuthBinding;
import com.inspoverse.api.service.OAuthBindingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 第三方OAuth绑定控制器
 */
@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class OAuthController {

  private final OAuthBindingService oAuthBindingService;

  /**
   * 获取我的第三方绑定状态
   */
  @GetMapping("/bindings")
  public ApiResponse<List<Map<String, Object>>> getBindings(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<UserOAuthBinding> bindings = oAuthBindingService.getUserBindings(userId);
    List<Map<String, Object>> result = bindings.stream().map(b -> {
      Map<String, Object> map = new HashMap<>();
      map.put("provider", b.getProvider());
      map.put("providerUsername", b.getProviderUsername());
      map.put("createdAt", b.getCreatedAt().toString());
      return map;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  /**
   * 获取OAuth授权跳转URL（前端重定向至此）
   */
  @GetMapping("/authorize/{provider}")
  public ApiResponse<Map<String, String>> getAuthUrl(
      HttpServletRequest request,
      @PathVariable String provider
  ) {
    Long userId = (Long) request.getAttribute("userId");
    String state = userId + "-" + UUID.randomUUID();
    String authUrl = oAuthBindingService.getAuthorizationUrl(provider, state);
    return ApiResponse.success(Map.of("authUrl", authUrl, "state", state));
  }

  /**
   * OAuth回调绑定（前端拿到 code 后调用此接口）
   */
  @PostMapping("/callback/{provider}")
  public ApiResponse<Map<String, Object>> oauthCallback(
      HttpServletRequest request,
      @PathVariable String provider,
      @RequestBody Map<String, String> body
  ) {
    Long userId = (Long) request.getAttribute("userId");
    String code = body.get("code");
    if (code == null || code.isBlank()) {
      return ApiResponse.failure(400, "code不能为空");
    }
    UserOAuthBinding binding = oAuthBindingService.bindOAuth(userId, provider, code);
    return ApiResponse.success(Map.of(
        "provider", binding.getProvider(),
        "providerUsername", binding.getProviderUsername() != null ? binding.getProviderUsername() : ""
    ));
  }

  /**
   * 解绑第三方账号
   */
  @DeleteMapping("/bindings/{provider}")
  public ApiResponse<Void> unbind(
      HttpServletRequest request,
      @PathVariable String provider
  ) {
    Long userId = (Long) request.getAttribute("userId");
    oAuthBindingService.unbind(userId, provider);
    return ApiResponse.success(null);
  }
}
