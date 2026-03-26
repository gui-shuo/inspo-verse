package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.PaymentOrder;
import com.inspoverse.api.service.PaymentService;
import com.inspoverse.api.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付控制器
 * <p>
 * 主要接口（需 JWT）：
 *   POST /api/v1/payment/create-order   创建支付订单
 *   GET  /api/v1/payment/status/{orderNo}  查询支付状态
 *   POST /api/v1/payment/mock-confirm/{orderNo}  Mock 确认支付（仅 dev）
 * <p>
 * 回调接口（无需 JWT，由 Epay 支付平台调用）：
 *   POST /api/v1/payment/notify/epay
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final VipService vipService;

  @Value("${app.payment.mock-mode:true}")
  private boolean mockMode;

  // ── 创建支付订单 ──────────────────────────────────────────────────────────────
  @PostMapping("/create-order")
  public ApiResponse<Map<String, Object>> createOrder(
      HttpServletRequest request,
      @Valid @RequestBody CreateOrderRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    PaymentOrder order = paymentService.createOrder(userId, req.packageId(), req.payMethod());

    Map<String, Object> result = new HashMap<>();
    result.put("orderNo",    order.getOrderNo());
    result.put("points",     order.getPoints());
    result.put("amount",     order.getAmount());
    result.put("payMethod",  order.getPayMethod());
    result.put("payUrl",     order.getPayUrl());
    result.put("qrCode",     order.getQrCodeDataUrl());   // Base64 data URI
    result.put("expiredAt",  order.getExpiredAt().toString());
    result.put("mockMode",   mockMode);
    return ApiResponse.success(result);
  }

  // ── 查询支付状态 ──────────────────────────────────────────────────────────────
  @GetMapping("/status/{orderNo}")
  public ApiResponse<Map<String, Object>> queryStatus(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    PaymentOrder order = paymentService.queryStatus(userId, orderNo);

    Map<String, Object> result = new HashMap<>();
    result.put("orderNo", order.getOrderNo());
    result.put("status",  order.getStatus());
    result.put("points",  order.getPoints());
    result.put("amount",  order.getAmount());
    result.put("paidAt",  order.getPaidAt() != null ? order.getPaidAt().toString() : null);
    return ApiResponse.success(result);
  }

  // ── Mock 模式：模拟扫码支付成功（仅开发/演示） ──────────────────────────────
  @PostMapping("/mock-confirm/{orderNo}")
  public ApiResponse<String> mockConfirm(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    paymentService.mockConfirm(userId, orderNo);
    return ApiResponse.success("支付成功");
  }

  // ── Epay 异步通知回调（生产使用，无需 JWT，由 Epay 平台调用） ──────────────
  @PostMapping("/notify/epay")
  public String epayNotify(@RequestParam Map<String, String> params) {
    log.info("[PaymentController] 收到 Epay 异步通知");
    String result = paymentService.handleEpayNotify(params);

    // 如果支付成功且是VIP订单，触发VIP激活
    if ("success".equals(result)) {
      String outTradeNo = params.get("out_trade_no");
      if (outTradeNo != null) {
        try {
          PaymentOrder order = paymentService.findByOrderNo(outTradeNo);
          if (order != null && "VIP".equals(order.getBizType()) && order.getBizRefId() != null) {
            vipService.onPaymentSuccess(order.getBizRefId(), params.getOrDefault("type", "epay"));
          }
        } catch (Exception e) {
          log.error("[PaymentController] VIP 激活处理异常: orderNo={}", outTradeNo, e);
        }
      }
    }
    return result;
  }

  // ── 支付宝异步通知回调（兼容保留） ─────────────────────────────────────────
  @PostMapping("/notify/alipay")
  public String alipayNotify(@RequestParam Map<String, String> params) {
    return paymentService.handleAlipayNotify(params);
  }

  // ── 微信支付异步通知回调（兼容保留） ───────────────────────────────────────
  @PostMapping("/notify/wechat")
  public String wechatNotify(@RequestBody String body) {
    return paymentService.handleWechatNotify(body);
  }

  // ── 获取当前用户的支付订单列表（含充值和VIP） ──────────────────────────────
  @GetMapping("/orders")
  public ApiResponse<List<Map<String, Object>>> getMyOrders(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<PaymentOrder> list = paymentService.getUserOrders(userId);
    List<Map<String, Object>> result = list.stream().map(o -> {
      Map<String, Object> m = new HashMap<>();
      m.put("id",        o.getId());
      m.put("orderNo",   o.getOrderNo());
      m.put("orderType", o.getBizType() != null ? o.getBizType() : "RECHARGE");
      m.put("points",    o.getPoints());
      m.put("amount",    o.getAmount());
      m.put("payMethod", o.getPayMethod());
      m.put("status",    o.getStatus());
      m.put("bizRefId",  o.getBizRefId());
      m.put("createdAt", o.getCreatedAt().toString());
      m.put("paidAt",    o.getPaidAt() != null ? o.getPaidAt().toString() : null);
      return m;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ── 获取充值套餐列表（无需 JWT） ─────────────────────────────────────────────
  @GetMapping("/packages")
  public ApiResponse<Object> getPackages() {
    var list = PaymentService.PACKAGES.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> Map.of(
            "id",     e.getKey(),
            "points", (int) e.getValue()[0],
            "amount", e.getValue()[1] / 100.0
        ))
        .toList();
    return ApiResponse.success(list);
  }

  // ── 请求体定义 ────────────────────────────────────────────────────────────────
  public record CreateOrderRequest(
      @NotBlank String packageId,
      @NotBlank @Pattern(regexp = "ALIPAY|WECHAT", message = "payMethod 只能是 ALIPAY 或 WECHAT") String payMethod
  ) {}
}
