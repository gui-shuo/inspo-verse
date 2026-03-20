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

/**
 * JWT 认证拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final JwtUtil jwtUtil;
  private final StringRedisTemplate redisTemplate;

  private static final String ONLINE_USERS_KEY = "online:users";
  private static final int ONLINE_TTL_SECONDS = 300; // 5分钟内认为在线
  // Redis 不可用时只打一次日志，避免每请求都刷 WARN
  private volatile boolean redisAvailable = true;

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
    // 直接放行 CORS 预检请求，由 WebMvcConfig#addCorsMappings 统一处理跨域
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

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

      // 更新Redis在线用户有序集合（Redis 不可用时降级跳过，不影响主流程）
      try {
        double now = System.currentTimeMillis() / 1000.0;
        redisTemplate.opsForZSet().add(ONLINE_USERS_KEY, String.valueOf(userId), now);
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_USERS_KEY, 0, now - ONLINE_TTL_SECONDS);
        // Redis 恢复可用
        if (!redisAvailable) {
          redisAvailable = true;
          log.info("在线用户计数：Redis 已恢复连接");
        }
      } catch (Exception e) {
        if (redisAvailable) {
          redisAvailable = false;
          log.warn("在线用户计数 Redis 不可用，已降级跳过（Redis 恢复后自动重启）: {}", e.getMessage());
        }
      }

      return true;
    } catch (Exception e) {
      writeUnauthorized(response, "Token解析失败");
      return false;
    }
  }
}
