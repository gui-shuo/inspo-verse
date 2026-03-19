package com.inspoverse.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JwtUtil jwtUtil;

  private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(
        ApiResponse.failure(ErrorCode.UNAUTHORIZED.getCode(), message)
    ));
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String authorization = request.getHeader("Authorization");

    if (!StringUtils.hasText(authorization)) {
      writeUnauthorized(response, "缺少Authorization头");
      return false;
    }

    if (!authorization.startsWith("Bearer ")) {
      writeUnauthorized(response, "Authorization格式错误");
      return false;
    }

    String token = authorization.substring(7);

    try {
      if (!jwtUtil.validateToken(token)) {
        writeUnauthorized(response, "Token已过期或无效");
        return false;
      }

      // 将用户信息存入请求属性，供后续使用
      Long userId = jwtUtil.getUserIdFromToken(token);
      String username = jwtUtil.getUsernameFromToken(token);
      request.setAttribute("userId", userId);
      request.setAttribute("username", username);

      return true;
    } catch (Exception e) {
      writeUnauthorized(response, "Token解析失败");
      return false;
    }
  }
}
