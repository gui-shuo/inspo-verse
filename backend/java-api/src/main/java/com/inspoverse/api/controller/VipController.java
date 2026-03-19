package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.VipMembership;
import com.inspoverse.api.entity.VipOrder;
import com.inspoverse.api.entity.VipPlan;
import com.inspoverse.api.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VIP控制器
 */
@RestController
@RequestMapping("/api/v1/vip")
@RequiredArgsConstructor
public class VipController {
  private final VipService vipService;

  /**
   * 获取VIP套餐列表
   */
  @GetMapping("/plans")
  public ApiResponse<List<Map<String, Object>>> getPlans() {
    List<VipPlan> plans = vipService.getAvailablePlans();

    List<Map<String, Object>> result = plans.stream().map(plan -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", plan.getId());
      map.put("planCode", plan.getPlanCode());
      map.put("planName", plan.getPlanName());
      map.put("price", plan.getPriceCents() / 100.0); // 转换为元
      map.put("priceCents", plan.getPriceCents());
      map.put("durationDays", plan.getDurationDays());
      map.put("level", plan.getLevel());
      map.put("levelName", getLevelName(plan.getLevel()));
      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(result);
  }

  /**
   * 创建VIP订单
   */
  @PostMapping("/orders")
  public ApiResponse<Map<String, Object>> createOrder(
      HttpServletRequest request,
      @Valid @RequestBody CreateOrderRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    VipOrder order = vipService.createOrder(userId, req.planId());

    return ApiResponse.success(Map.of(
        "orderId", order.getId(),
        "orderNo", order.getOrderNo(),
        "amount", order.getAmountCents() / 100.0,
        "payStatus", order.getPayStatus()
    ));
  }

  /**
   * 获取我的订单列表
   */
  @GetMapping("/orders")
  public ApiResponse<List<Map<String, Object>>> getMyOrders(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<VipOrder> orders = vipService.getUserOrders(userId);

    List<Map<String, Object>> result = orders.stream().map(order -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", order.getId());
      map.put("orderNo", order.getOrderNo());
      map.put("amount", order.getAmountCents() / 100.0);
      map.put("payStatus", order.getPayStatus());
      map.put("payStatusText", getPayStatusText(order.getPayStatus()));
      map.put("payChannel", order.getPayChannel());
      map.put("paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : null);
      map.put("createdAt", order.getCreatedAt().toString());
      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(result);
  }

  /**
   * 模拟支付
   */
  @PostMapping("/orders/{id}/pay")
  public ApiResponse<Void> payOrder(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    vipService.mockPay(id, userId);
    return ApiResponse.success(null);
  }

  /**
   * 获取我的会员信息
   */
  @GetMapping("/membership")
  public ApiResponse<Map<String, Object>> getMyMembership(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    VipMembership membership = vipService.getCurrentMembership(userId);

    if (membership == null) {
      return ApiResponse.success(Map.of(
          "isVip", false,
          "level", 0,
          "levelName", "普通用户"
      ));
    }

    return ApiResponse.success(Map.of(
        "isVip", true,
        "level", membership.getVipLevel(),
        "levelName", getLevelName(membership.getVipLevel()),
        "startTime", membership.getStartTime().toString(),
        "endTime", membership.getEndTime().toString(),
        "daysRemaining", java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDateTime.now(),
            membership.getEndTime()
        )
    ));
  }

  private String getLevelName(int level) {
    return switch (level) {
      case 1 -> "白银会员";
      case 2 -> "黄金会员";
      default -> "普通用户";
    };
  }

  private String getPayStatusText(int status) {
    return switch (status) {
      case 0 -> "待支付";
      case 1 -> "已支付";
      case 2 -> "支付失败";
      case 3 -> "已退款";
      default -> "未知";
    };
  }

  public record CreateOrderRequest(
      @NotNull(message = "套餐ID不能为空") Long planId
  ) {}
}
