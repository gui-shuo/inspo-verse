package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.User;
import com.inspoverse.api.service.UserService;
import com.inspoverse.api.util.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器
 */
@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final UserService userService;
  private final JwtUtil jwtUtil;

  /**
   * 用户注册
   */
  @PostMapping("/register")
  public ApiResponse<Map<String, String>> register(@Valid @RequestBody RegisterRequest req) {
    User user = userService.register(req.username(), req.phone(), req.password());

    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
    String refreshToken = jwtUtil.generateRefreshToken(user.getId());

    return ApiResponse.success(Map.of(
        "accessToken", accessToken,
        "refreshToken", refreshToken,
        "username", user.getUsername()
    ));
  }

  /**
   * 用户登录
   */
  @PostMapping("/login")
  public ApiResponse<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
    User user = userService.getUserByUsername(req.username());
    if (user == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }

    if (!userService.verifyPassword(user, req.password())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }

    if (user.getStatus() != 1) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
    }

    // 更新最后登录时间
    userService.updateLastLogin(user.getId());

    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
    String refreshToken = jwtUtil.generateRefreshToken(user.getId());

    return ApiResponse.success(Map.of(
        "accessToken", accessToken,
        "refreshToken", refreshToken,
        "username", user.getUsername()
    ));
  }

  public record RegisterRequest(
      @NotBlank(message = "用户名不能为空")
      @Pattern(regexp = "^[a-zA-Z0-9]{4,16}$", message = "用户名需4-16位字母数字组合")
      String username,

      @NotBlank(message = "手机号不能为空")
      @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号")
      String phone,

      @NotBlank(message = "密码不能为空")
      @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{8,}$",
          message = "密码需8位以上含大小写字母、数字及符号")
      String password
  ) {}

  public record LoginRequest(
      @NotBlank(message = "用户名不能为空")
      String username,

      @NotBlank(message = "密码不能为空")
      String password
  ) {}
}
