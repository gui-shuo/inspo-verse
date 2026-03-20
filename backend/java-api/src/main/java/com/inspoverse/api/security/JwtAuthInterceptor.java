package com.inspoverse.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * JWT 认证拦截器
 * 支持两种模式：
 *  - 强制认证：token 必须存在且有效（默认）
 *  - 可选认证： GET 请求访问论坛公开访问的路径，有 token 则解析，无 token 则匿名放行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JwtUtil jwtUtil;
  private final StringRedisTemplate redisTemplate;

  private static final String ONLINE_USERS_KEY = "online:users";
  private static final int ONLINE_TTL_SECONDS = 300;
  private volatile boolean redisAvailable = true;

  /**
   * GET 请求未登录也可访问的公开路径（无 token 放行，有 token 则解析并注入 userId）
   */
  private static final Pattern OPTIONAL_AUTH_GET_PATTERN = Pattern.compile(
      "^/api/v1/(forum/(posts|comments|posts/\\d+)|content/(explore|explore/\\d+|explore/\\d+/comments)|workshop/(projects|projects/\\d+))$"
  );

  private boolean isOptionalAuth(HttpServletRequest request) {
    return "GET".equalsIgnoreCase(request.getMethod())
        && OPTIONAL_AUTH_GET_PATTERN.matcher(request.getServletPath()).matches();
  }

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
    // 直接放行 CORS 预检请求
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String authorization = request.getHeader("Authorization");
    boolean hasToken = StringUtils.hasText(authorization) && authorization.startsWith("Bearer ");

    // 无 token 时：对可选认证路径 GET 请求匿名放行，其它情况返回 401
    if (!hasToken) {
      if (isOptionalAuth(request)) {
        return true;
      }
      writeUnauthorized(response, "缺少Authorization头");
      return false;
    }

    String token = authorization.substring(7);

    try {
      if (!jwtUtil.validateToken(token)) {
        // 可选认证路径上 token 失效时也放行（匿名模式）
        if (isOptionalAuth(request)) {
          return true;
        }
        writeUnauthorized(response, "Token已过期或无效");
        return false;
      }

      Long userId = jwtUtil.getUserIdFromToken(token);
      String username = jwtUtil.getUsernameFromToken(token);
      request.setAttribute("userId", userId);
      request.setAttribute("username", username);

      // 更新Redis在线用户（Redis 不可用时降级跳过）
      try {
        double now = System.currentTimeMillis() / 1000.0;
        redisTemplate.opsForZSet().add(ONLINE_USERS_KEY, String.valueOf(userId), now);
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_USERS_KEY, 0, now - ONLINE_TTL_SECONDS);
        if (!redisAvailable) {
          redisAvailable = true;
          log.info("在线用户计数：Redis 已恢复连接");
        }
      } catch (Exception e) {
        if (redisAvailable) {
          redisAvailable = false;
          log.warn("在线用户计数 Redis 不可用，已降级跳过: {}", e.getMessage());
        }
      }

      return true;
    } catch (Exception e) {
      if (isOptionalAuth(request)) {
        return true; // token 格式异常也不阻断公开读取
      }
      writeUnauthorized(response, "Token解析失败");
      return false;
    }
  }
}
