package com.inspoverse.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * VIP 权限拦截器
 * <p>
 * 检查目标方法上的 {@link RequireVip} 注解，若当前用户 VIP 等级不足则拦截并返回 403。
 * 此拦截器必须注册在 JwtAuthInterceptor 之后（确保 userId 已注入 request）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipAuthInterceptor implements HandlerInterceptor {

  private final VipService vipService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final String[] LEVEL_NAMES = {"普通用户", "白银会员", "黄金会员"};

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    RequireVip annotation = handlerMethod.getMethodAnnotation(RequireVip.class);
    if (annotation == null) {
      return true; // 无注解，直接放行
    }

    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      // 未登录用户在 JwtAuthInterceptor 中已被拦截
      // 如果走到这里说明是可选认证路径，直接拒绝
      writeForbidden(response, "请先登录");
      return false;
    }

    int requiredLevel = annotation.level();
    int userLevel = vipService.getUserVipLevel(userId);

    if (userLevel < requiredLevel) {
      String levelName = requiredLevel < LEVEL_NAMES.length ? LEVEL_NAMES[requiredLevel] : "VIP";
      String msg = annotation.message().isEmpty()
          ? String.format("此功能需要 %s 及以上等级", levelName)
          : annotation.message();
      log.info("[VipAuth] 权限不足: userId={} userLevel={} required={}", userId, userLevel, requiredLevel);
      writeForbidden(response, msg);
      return false;
    }

    return true;
  }

  private void writeForbidden(HttpServletResponse response, String message) throws Exception {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(
        ApiResponse.failure(ErrorCode.FORBIDDEN.getCode(), message)
    ));
  }
}
