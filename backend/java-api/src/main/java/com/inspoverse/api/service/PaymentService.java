package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.PaymentOrder;
import com.inspoverse.api.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务
 * <p>
 * 支持两种运行模式：
 * - mock 模式（app.payment.mock-mode=true）：适用于开发和演示环境
 * - 生产模式（app.payment.mock-mode=false）：接入第三方易支付（Epay）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentOrderMapper paymentOrderMapper;
  private final WalletService walletService;
  private final EpayService epayService;

  @Value("${app.payment.mock-mode:true}")
  private boolean mockMode;

  @Value("${app.payment.notify-base-url:http://localhost:8080}")
  private String notifyBaseUrl;

  @Value("${cors.allowed-origins:http://localhost:5173}")
  private String frontendOrigin;

  /**
   * 充值套餐定义：packageId -> [points, amountCents]
   * amountCents: 分 (1元=100分)
   */
  public static final Map<String, long[]> PACKAGES = Map.of(
      "p100",  new long[]{100,  600},    // 100 pts = ¥6
      "p500",  new long[]{500,  2800},   // 500 pts = ¥28
      "p1500", new long[]{1500, 6800},   // 1500 pts = ¥68  (HOT)
      "p5000", new long[]{5000, 18800}   // 5000 pts = ¥188
  );

  // ── 支付宝蓝 / 微信绿 ──
  private static final int COLOR_ALIPAY  = 0xFF1677FF;
  private static final int COLOR_WECHAT  = 0xFF07C160;

  /**
   * 将前端支付方式映射到 Epay 支付类型
   */
  private String toEpayType(String payMethod) {
    return switch (payMethod) {
      case "ALIPAY" -> "alipay";
      case "WECHAT" -> "wxpay";
      default -> "alipay";
    };
  }

  /**
   * 创建支付订单
   *
   * @param userId    用户ID
   * @param packageId 套餐ID（p100 / p500 / p1500 / p5000）
   * @param payMethod ALIPAY / WECHAT
   * @return 已保存的支付订单（含 qrCodeDataUrl 字段）
   */
  @Transactional
  public PaymentOrder createOrder(Long userId, String packageId, String payMethod) {
    long[] pkg = PACKAGES.get(packageId);
    if (pkg == null) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的充值套餐：" + packageId);
    }
    if (!"ALIPAY".equals(payMethod) && !"WECHAT".equals(payMethod)) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的支付方式：" + payMethod);
    }

    // 检查有无未完成的相同套餐订单（防重复创建）
    long pending = paymentOrderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>()
        .eq(PaymentOrder::getUserId, userId)
        .eq(PaymentOrder::getStatus, "PENDING")
        .gt(PaymentOrder::getExpiredAt, LocalDateTime.now()));
    if (pending >= 3) {
      throw new BusinessException(ErrorCode.CONFLICT, "存在未完成的支付订单，请先完成或等待订单过期");
    }

    int points     = (int) pkg[0];
    BigDecimal amt = BigDecimal.valueOf(pkg[1], 2); // 分 → 元，scale=2

    String orderNo = "EP" + System.currentTimeMillis()
        + ThreadLocalRandom.current().nextInt(1000, 9999);

    PaymentOrder order = new PaymentOrder();
    order.setUserId(userId);
    order.setOrderNo(orderNo);
    order.setPackageId(packageId);
    order.setAmount(amt);
    order.setPoints(points);
    order.setPayMethod(payMethod);
    order.setStatus("PENDING");
    order.setExpiredAt(LocalDateTime.now().plusMinutes(5));

    if (mockMode) {
      order.setPayUrl("MOCK:" + orderNo);
    } else {
      // 调用 Epay 生成支付链接
      String notifyUrl = notifyBaseUrl.replaceAll("/+$", "") + "/api/v1/payment/notify/epay";
      String returnUrl = frontendOrigin.replaceAll("/+$", "") + "/user?tab=wallet";

      String payUrl = epayService.createPayUrl(
          orderNo,
          "灵感点数充值 " + points + "pts",
          amt,
          notifyUrl,
          returnUrl,
          toEpayType(payMethod)
      );
      order.setPayUrl(payUrl);
    }

    paymentOrderMapper.insert(order);
    log.info("[Payment] 创建订单 orderNo={} userId={} method={} points={} amount={} mock={}",
        orderNo, userId, payMethod, points, amt, mockMode);

    // 生成二维码图片（payUrl 渲染为 QR 码）
    order.setQrCodeDataUrl(generateQrCode(order.getPayUrl(), payMethod));
    return order;
  }

  /**
   * 查询支付状态
   *
   * @param userId  当前登录用户（防止越权）
   * @param orderNo 商户订单号
   */
  public PaymentOrder queryStatus(Long userId, String orderNo) {
    PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
        .eq(PaymentOrder::getUserId, userId)
        .eq(PaymentOrder::getOrderNo, orderNo));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    // 自动过期
    if ("PENDING".equals(order.getStatus()) && LocalDateTime.now().isAfter(order.getExpiredAt())) {
      order.setStatus("EXPIRED");
      paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
          .eq(PaymentOrder::getId, order.getId())
          .set(PaymentOrder::getStatus, "EXPIRED"));
    }
    return order;
  }

  /**
   * Mock 模式：模拟支付成功（开发/演示专用）
   * 生产环境请通过支付回调通知接口触发充值。
   *
   * @param userId  当前登录用户
   * @param orderNo 商户订单号
   */
  @Transactional
  public void mockConfirm(Long userId, String orderNo) {
    if (!mockMode) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "非 mock 模式不支持此接口");
    }
    PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
        .eq(PaymentOrder::getUserId, userId)
        .eq(PaymentOrder::getOrderNo, orderNo));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if ("PAID".equals(order.getStatus())) return; // 幂等
    if (!"PENDING".equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "订单状态异常：" + order.getStatus());
    }

    // 更新订单状态
    paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
        .eq(PaymentOrder::getId, order.getId())
        .set(PaymentOrder::getStatus, "PAID")
        .set(PaymentOrder::getPaidAt, LocalDateTime.now()));

    // 充值灵感点数
    String desc = String.format("充值 %d 灵感点数（%s）", order.getPoints(),
        "WECHAT".equals(order.getPayMethod()) ? "微信支付" : "支付宝");
    walletService.earn(userId, order.getPoints(), "EARN_RECHARGE", desc, orderNo);

    log.info("[Payment] Mock 支付成功：orderNo={} userId={} points={}", orderNo, userId, order.getPoints());
  }

  /**
   * 获取用户的支付订单列表（最近50条，按创建时间倒序）
   */
  public java.util.List<PaymentOrder> getUserOrders(Long userId) {
    return paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
        .eq(PaymentOrder::getUserId, userId)
        .orderByDesc(PaymentOrder::getCreatedAt)
        .last("LIMIT 50"));
  }

  public boolean isMockMode() {
    return mockMode;
  }

  /**
   * 通过订单号查找支付订单（内部使用，无需校验用户）
   */
  public PaymentOrder findByOrderNo(String orderNo) {
    return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
        .eq(PaymentOrder::getOrderNo, orderNo));
  }

  /**
   * 为VIP订单创建支付单（复用支付能力）
   */
  @Transactional
  public PaymentOrder createVipPaymentOrder(Long userId, String vipOrderNo,
      String planName, int priceCents, String payMethod) {

    BigDecimal amt = BigDecimal.valueOf(priceCents, 2);
    String orderNo = "EPVIP" + System.currentTimeMillis()
        + java.util.concurrent.ThreadLocalRandom.current().nextInt(1000, 9999);

    PaymentOrder order = new PaymentOrder();
    order.setUserId(userId);
    order.setOrderNo(orderNo);
    order.setPackageId("VIP");
    order.setAmount(amt);
    order.setPoints(0);
    order.setPayMethod(payMethod);
    order.setStatus("PENDING");
    order.setBizType("VIP");
    order.setBizRefId(vipOrderNo);
    order.setExpiredAt(LocalDateTime.now().plusMinutes(5));

    if (mockMode) {
      order.setPayUrl("MOCK:" + orderNo);
    } else {
      // 调用 Epay 生成支付链接
      String notifyUrl = notifyBaseUrl.replaceAll("/+$", "") + "/api/v1/payment/notify/epay";
      String returnUrl = frontendOrigin.replaceAll("/+$", "") + "/vip";

      String payUrl = epayService.createPayUrl(
          orderNo,
          "INSPO VIP - " + planName,
          amt,
          notifyUrl,
          returnUrl,
          toEpayType(payMethod)
      );
      order.setPayUrl(payUrl);
    }

    paymentOrderMapper.insert(order);
    order.setQrCodeDataUrl(generateQrCode(order.getPayUrl(), payMethod));
    log.info("[Payment] 创建VIP支付订单 orderNo={} vipOrderNo={} amount={} mock={}",
        orderNo, vipOrderNo, amt, mockMode);
    return order;
  }

  /**
   * Mock VIP支付确认（开发环境）
   */
  @Transactional
  public void mockConfirmVipPayment(Long userId, String paymentOrderNo) {
    if (!mockMode) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "非 mock 模式不支持此接口");
    }
    PaymentOrder order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
        .eq(PaymentOrder::getUserId, userId)
        .eq(PaymentOrder::getOrderNo, paymentOrderNo));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "支付订单不存在");
    }
    if ("PAID".equals(order.getStatus())) return;
    if (!"PENDING".equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "订单状态异常：" + order.getStatus());
    }
    if (!"VIP".equals(order.getBizType())) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "非VIP支付订单");
    }

    paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
        .eq(PaymentOrder::getId, order.getId())
        .set(PaymentOrder::getStatus, "PAID")
        .set(PaymentOrder::getPaidAt, LocalDateTime.now()));

    log.info("[Payment] Mock VIP支付成功：orderNo={} vipRef={}", paymentOrderNo, order.getBizRefId());
  }

  /**
   * 通用支付确认处理（回调统一入口）— 根据 bizType 分发
   */
  @Transactional
  public void confirmPayment(String orderNo) {
    PaymentOrder order = paymentOrderMapper.selectOne(
        new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getOrderNo, orderNo));
    if (order == null || "PAID".equals(order.getStatus())) return;

    paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrder>()
        .eq(PaymentOrder::getId, order.getId())
        .set(PaymentOrder::getStatus, "PAID")
        .set(PaymentOrder::getPaidAt, LocalDateTime.now()));

    if ("RECHARGE".equals(order.getBizType()) || "VIP".equals(order.getPackageId()) == false) {
      // 充值订单 → 直接充值灵感点数
      String desc = String.format("充值 %d 灵感点数（%s）", order.getPoints(),
          "WECHAT".equals(order.getPayMethod()) ? "微信支付" : "支付宝");
      walletService.earn(order.getUserId(), order.getPoints(), "EARN_RECHARGE", desc, orderNo);
    }
    // VIP订单的激活由VipService.onPaymentSuccess处理，此处不重复
  }

  /**
   * Epay 异步通知回调处理
   * 验证签名 → 更新订单状态 → 触发充值/VIP激活
   *
   * @param params 通知参数
   * @return "success" 表示处理成功
   */
  @Transactional
  public String handleEpayNotify(Map<String, String> params) {
    log.info("[Payment] 收到 Epay 异步通知: {}", params);

    // 1. 验证签名
    String sign = params.get("sign");
    if (sign == null || !epayService.verifyNotifySign(params, sign)) {
      log.warn("[Payment] Epay 通知签名验证失败: {}", params);
      return "fail";
    }

    // 2. 检查交易状态
    String tradeStatus = params.get("trade_status");
    if (!"TRADE_SUCCESS".equals(tradeStatus)) {
      log.info("[Payment] Epay 通知交易未成功: status={}", tradeStatus);
      return "success"; // 返回 success 防止重复通知
    }

    // 3. 查找订单
    String outTradeNo = params.get("out_trade_no");
    if (outTradeNo == null) {
      log.warn("[Payment] Epay 通知缺少 out_trade_no");
      return "fail";
    }

    PaymentOrder order = paymentOrderMapper.selectOne(
        new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getOrderNo, outTradeNo));
    if (order == null) {
      log.warn("[Payment] Epay 通知订单不存在: {}", outTradeNo);
      return "fail";
    }
    if ("PAID".equals(order.getStatus())) {
      return "success"; // 幂等
    }
    if (!"PENDING".equals(order.getStatus())) {
      log.warn("[Payment] Epay 通知订单状态异常: orderNo={} status={}", outTradeNo, order.getStatus());
      return "success";
    }

    // 4. 验证金额
    String moneyStr = params.get("money");
    if (moneyStr != null) {
      BigDecimal notifyAmount = new BigDecimal(moneyStr);
      if (order.getAmount().compareTo(notifyAmount) != 0) {
        log.error("[Payment] Epay 通知金额不匹配: orderNo={} expected={} actual={}",
            outTradeNo, order.getAmount(), notifyAmount);
        return "fail";
      }
    }

    // 5. 确认支付
    confirmPayment(outTradeNo);

    // 6. 如果是 VIP 订单，通知 VIP 服务（通过 ApplicationContext 延迟获取避免循环依赖）
    if ("VIP".equals(order.getBizType()) && order.getBizRefId() != null) {
      log.info("[Payment] Epay VIP支付成功，bizRef={}", order.getBizRefId());
      // VIP 激活由 PaymentController 层面通过事件或直接调用 VipService 处理
    }

    log.info("[Payment] Epay 支付确认成功: orderNo={} tradeNo={}", outTradeNo, params.get("trade_no"));
    return "success";
  }

  /**
   * 支付宝异步通知回调处理（保留兼容，但标记已迁移至 Epay）
   */
  @Transactional
  public String handleAlipayNotify(Map<String, String> params) {
    log.warn("[Payment] 收到支付宝直接通知（已迁移至 Epay），转发处理: {}", params);
    return handleEpayNotify(params);
  }

  /**
   * 微信支付异步通知回调处理（保留兼容，但标记已迁移至 Epay）
   */
  @Transactional
  public String handleWechatNotify(String body) {
    log.warn("[Payment] 收到微信直接通知（已迁移至 Epay），忽略: {}", body);
    return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
  }

  // ─────────────────────────── 私有工具方法 ───────────────────────────────────

  /**
   * 生成 QR 码图像，返回 Base64 Data URL
   */
  private String generateQrCode(String content, String payMethod) {
    int color = "WECHAT".equals(payMethod) ? COLOR_WECHAT : COLOR_ALIPAY;
    try {
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 250, 250);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageConfig config = new MatrixToImageConfig(color, 0xFFFFFFFF);
      MatrixToImageWriter.writeToStream(matrix, "PNG", out, config);
      String b64 = Base64.getEncoder().encodeToString(out.toByteArray());
      return "data:image/png;base64," + b64;
    } catch (WriterException | IOException e) {
      log.error("[Payment] 生成二维码失败: {}", e.getMessage());
      return "";
    }
  }
}
