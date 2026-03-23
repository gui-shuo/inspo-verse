package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.VipMembership;
import com.inspoverse.api.entity.VipOrder;
import com.inspoverse.api.entity.VipPlan;
import com.inspoverse.api.service.DailyTaskService;
import com.inspoverse.api.service.ExperienceService;
import com.inspoverse.api.service.PaymentService;
import com.inspoverse.api.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vip")
@RequiredArgsConstructor
public class VipController {
  private final VipService vipService;
  private final ExperienceService experienceService;
  private final DailyTaskService dailyTaskService;
  private final PaymentService paymentService;

  // ─── VIP套餐列表 ──────────────────────────────────────────────────────────
  @GetMapping("/plans")
  public ApiResponse<List<Map<String, Object>>> getPlans() {
    List<VipPlan> plans = vipService.getAvailablePlans();
    List<Map<String, Object>> result = plans.stream().map(plan -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", plan.getId());
      map.put("planCode", plan.getPlanCode());
      map.put("planName", plan.getPlanName());
      map.put("price", plan.getPriceCents() / 100.0);
      map.put("priceCents", plan.getPriceCents());
      map.put("durationDays", plan.getDurationDays());
      map.put("level", plan.getLevel());
      map.put("levelName", getLevelName(plan.getLevel()));
      return map;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ─── 创建VIP订单（含支付二维码） ─────────────────────────────────────────
  @PostMapping("/orders")
  public ApiResponse<Map<String, Object>> createOrder(
      HttpServletRequest request,
      @Valid @RequestBody CreateVipOrderRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    Map<String, Object> result = vipService.createOrderWithPayment(userId, req.planId(), req.payMethod());
    return ApiResponse.success(result);
  }

  // ─── 查询VIP订单支付状态 ──────────────────────────────────────────────────
  @GetMapping("/orders/{orderNo}/status")
  public ApiResponse<Map<String, Object>> queryPayStatus(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    return ApiResponse.success(vipService.queryVipPaymentStatus(userId, orderNo));
  }

  // ─── Mock确认VIP支付（开发模式） ───────────────────────────────────────────
  @PostMapping("/orders/{orderNo}/mock-confirm")
  public ApiResponse<String> mockConfirmVipPay(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    // 通过支付订单号找到paymentOrder，确认支付
    paymentService.mockConfirmVipPayment(userId, orderNo);
    // 通知VipService激活会员
    vipService.onPaymentSuccess(
        getVipOrderNoFromPaymentOrder(userId, orderNo),
        "mock"
    );
    return ApiResponse.success("VIP支付成功");
  }

  // ─── 旧版模拟支付（兼容） ─────────────────────────────────────────────────
  @PostMapping("/orders/{id}/pay")
  public ApiResponse<Void> payOrder(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    vipService.mockPay(id, userId);
    return ApiResponse.success(null);
  }

  // ─── 获取我的VIP订单列表 ──────────────────────────────────────────────────
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
      VipPlan plan = vipService.getPlanById(order.getVipPlanId());
      map.put("itemName", plan != null ? plan.getPlanName() : "VIP套餐");
      map.put("planLevel", plan != null ? plan.getLevel() : 0);
      return map;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ─── 获取我的会员信息（含等级经验） ───────────────────────────────────────
  @GetMapping("/membership")
  public ApiResponse<Map<String, Object>> getMyMembership(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    VipMembership membership = vipService.getCurrentMembership(userId);
    Map<String, Object> expDetail = experienceService.getUserExpDetail(userId);

    Map<String, Object> result = new HashMap<>(expDetail);
    if (membership == null) {
      result.put("isVip", false);
      result.put("vipLevel", 0);
      result.put("vipLevelName", "普通用户");
    } else {
      result.put("isVip", true);
      result.put("vipLevel", membership.getVipLevel());
      result.put("vipLevelName", getLevelName(membership.getVipLevel()));
      result.put("startTime", membership.getStartTime().toString());
      result.put("endTime", membership.getEndTime().toString());
      result.put("daysRemaining", java.time.temporal.ChronoUnit.DAYS.between(
          java.time.LocalDateTime.now(), membership.getEndTime()));
    }
    return ApiResponse.success(result);
  }

  // ─── 成长轨迹 ─────────────────────────────────────────────────────────────
  @GetMapping("/growth")
  public ApiResponse<Map<String, Object>> getGrowth(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    return ApiResponse.success(experienceService.getGrowthTrajectory(userId));
  }

  // ─── 每日任务列表 ─────────────────────────────────────────────────────────
  @GetMapping("/tasks")
  public ApiResponse<List<Map<String, Object>>> getDailyTasks(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    return ApiResponse.success(dailyTaskService.getTodayTasks(userId));
  }

  // ─── 签到（完成签到任务并领取奖励） ───────────────────────────────────────
  @PostMapping("/tasks/signin")
  public ApiResponse<Map<String, Object>> signIn(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    Map<String, Object> result = dailyTaskService.completeSignIn(userId);
    return ApiResponse.success(result);
  }

  // ─── 领取任务奖励 ─────────────────────────────────────────────────────────
  @PostMapping("/tasks/{taskCode}/claim")
  public ApiResponse<Map<String, Object>> claimTaskReward(
      HttpServletRequest request,
      @PathVariable String taskCode
  ) {
    Long userId = (Long) request.getAttribute("userId");
    return ApiResponse.success(dailyTaskService.claimReward(userId, taskCode));
  }

  // ─── 会员权益列表 ─────────────────────────────────────────────────────────
  @GetMapping("/privileges")
  public ApiResponse<List<Map<String, Object>>> getPrivileges(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    int vipLevel = vipService.getUserVipLevel(userId);
    return ApiResponse.success(buildPrivileges(vipLevel));
  }

  // ─── 辅助方法 ─────────────────────────────────────────────────────────────

  private String getVipOrderNoFromPaymentOrder(Long userId, String paymentOrderNo) {
    var orders = paymentService.getUserOrders(userId);
    for (var order : orders) {
      if (paymentOrderNo.equals(order.getOrderNo()) && "VIP".equals(order.getBizType())) {
        return order.getBizRefId();
      }
    }
    return paymentOrderNo;
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

  private List<Map<String, Object>> buildPrivileges(int vipLevel) {
    List<Map<String, Object>> all = new java.util.ArrayList<>();
    all.add(Map.of("icon", "zap", "title", "AI 极速响应", "desc", "优先调用高性能计算节点", "unlocked", vipLevel >= 1));
    all.add(Map.of("icon", "crown", "title", "专属模型", "desc", "解锁 GPT-4 及 Claude 3 Opus", "unlocked", vipLevel >= 2));
    all.add(Map.of("icon", "gift", "title", "月度礼包", "desc", vipLevel >= 2 ? "每月领取 1000 灵感点数" : "每月领取 300 灵感点数", "unlocked", vipLevel >= 1));
    all.add(Map.of("icon", "shield", "title", "数据保险箱", "desc", "企业级数据加密存储", "unlocked", vipLevel >= 1));
    all.add(Map.of("icon", "activity", "title", "无限上下文", "desc", "支持 100k+ Token 对话", "unlocked", vipLevel >= 2));
    all.add(Map.of("icon", "arrow-up-right", "title", "多模态输入", "desc", "支持图片/音频/视频识别", "unlocked", vipLevel >= 2));
    return all;
  }

  public record CreateVipOrderRequest(
      @NotNull(message = "套餐ID不能为空") Long planId,
      @NotBlank(message = "支付方式不能为空")
      @Pattern(regexp = "ALIPAY|WECHAT", message = "支付方式只能是 ALIPAY 或 WECHAT")
      String payMethod
  ) {}
}
