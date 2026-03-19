package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.User;
import com.inspoverse.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  /**
   * 获取当前用户信息
   */
  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    User user = userService.getUserById(userId);

    return ApiResponse.success(Map.of(
        "id", user.getId(),
        "username", user.getUsername(),
        "nickname", user.getNickname(),
        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
        "phone", user.getPhone() != null ? user.getPhone() : "",
        "email", user.getEmail() != null ? user.getEmail() : "",
        "status", user.getStatus(),
        "createdAt", user.getCreatedAt().toString()
    ));
  }

  /**
   * 更新当前用户信息
   */
  @PutMapping("/me")
  public ApiResponse<Void> updateMe(
      HttpServletRequest request,
      @Valid @RequestBody UpdateUserRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    userService.updateUser(userId, req.nickname(), req.avatarUrl());
    return ApiResponse.success(null);
  }

  public record UpdateUserRequest(
      String nickname,
      String avatarUrl
  ) {}
}
