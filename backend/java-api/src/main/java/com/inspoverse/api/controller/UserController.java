package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.User;
import com.inspoverse.api.service.AdminService;
import com.inspoverse.api.service.FileStorageService;
import com.inspoverse.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final FileStorageService fileStorageService;
  private final AdminService adminService;

  /**
   * 获取当前用户信息
   */
  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    User user = userService.getUserById(userId);

    Map<String, Object> result = new HashMap<>();
    result.put("id", user.getId());
    result.put("username", user.getUsername());
    result.put("nickname", user.getNickname());
    result.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
    result.put("phone", user.getPhone() != null ? UserService.maskPhone(user.getPhone()) : "");
    result.put("email", user.getEmail() != null ? user.getEmail() : "");
    result.put("bio", user.getBio() != null ? user.getBio() : "");
    result.put("status", user.getStatus());
    result.put("createdAt", user.getCreatedAt().toString());

    // 返回角色信息
    List<String> roles = adminService.getUserRoles(userId);
    result.put("roles", roles);
    result.put("isAdmin", roles.contains("ROLE_ADMIN"));

    return ApiResponse.success(result);
  }

  /**
   * 更新个人资料（昵称、简介、手机号）
   */
  @PutMapping("/me/profile")
  public ApiResponse<Void> updateProfile(
      HttpServletRequest request,
      @Valid @RequestBody UpdateProfileRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    userService.updateProfile(userId, req.nickname(), req.bio(), req.phone());
    return ApiResponse.success(null);
  }

  /**
   * 上传头像（multipart/form-data）
   */
  @PostMapping("/me/avatar")
  public ApiResponse<Map<String, String>> uploadAvatar(
      HttpServletRequest request,
      @RequestParam("file") MultipartFile file
  ) {
    Long userId = (Long) request.getAttribute("userId");
    String avatarUrl = fileStorageService.uploadAvatar(userId, file);
    userService.updateAvatar(userId, avatarUrl);
    return ApiResponse.success(Map.of("avatarUrl", avatarUrl));
  }

  /**
   * 修改密码
   */
  @PostMapping("/me/change-password")
  public ApiResponse<Void> changePassword(
      HttpServletRequest request,
      @Valid @RequestBody ChangePasswordRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    userService.changePassword(userId, req.currentPassword(), req.newPassword());
    return ApiResponse.success(null);
  }

  // ========================= Request Records =========================

  public record UpdateProfileRequest(
      @Size(max = 32, message = "昵称最多32个字符") String nickname,
      @Size(max = 500, message = "简介最多500个字符") String bio,
      String phone
  ) {}

  public record ChangePasswordRequest(
      @NotBlank(message = "当前密码不能为空") String currentPassword,
      @NotBlank(message = "新密码不能为空") String newPassword,
      @NotBlank(message = "确认密码不能为空") String confirmPassword
  ) {}
}
