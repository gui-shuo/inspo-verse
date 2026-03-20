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
 * 默认运行在 mock 模式（app.payment.mock-mode=true），适用于开发和演示环境。
 * 生产环境需配置支付宝/微信支付 SDK 凭证，并将 mock-mode 关闭。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentOrderMapper paymentOrderMapper;
  private final WalletService walletService;

  @Value("${app.payment.mock-mode:true}")
  private boolean mockMode;

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

  // ── 微信绿 / 支付宝蓝 ──
  private static final int COLOR_WECHAT = 0xFF07C160;
  private static final int COLOR_ALIPAY  = 0xFF1677FF;

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

    String orderNo = ("WECHAT".equals(payMethod) ? "WX" : "ALI")
        + System.currentTimeMillis()
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
      // 开发模式：payUrl 存 mock 标识，QR 内容为订单号
      order.setPayUrl("MOCK:" + orderNo);
    } else {
      // ─── TODO: 生产环境集成 ──────────────────────────────────────────────────
      // 支付宝 Native 支付示例：
      //   AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
      //   model.setSubject("灵感点数充值 " + points + "pts");
      //   model.setOutTradeNo(orderNo);
      //   model.setTotalAmount(amt.toString());
      //   AlipayTradePrecreateResponse resp = alipayClient.execute(request);
      //   order.setPayUrl(resp.getQrCode());
      //
      // 微信支付 Native 示例：
      //   NativePayRequest req = new NativePayRequest();
      //   req.setDescription("灵感点数充值 " + points + "pts");
      //   req.setOutTradeNo(orderNo);
      //   ... 设置 amount / notifyUrl 等
      //   NativePayResponse resp = wechatPayService.getNativePayApi().nativePay(req);
      //   order.setPayUrl(resp.getCodeUrl());
      // ────────────────────────────────────────────────────────────────────────
      order.setPayUrl("MOCK:" + orderNo);
      log.warn("[Payment] 生产 SDK 未集成，仍使用 mock 模式");
    }

    paymentOrderMapper.insert(order);
    log.info("[Payment] 创建订单 orderNo={} userId={} method={} points={} amount={}",
        orderNo, userId, payMethod, points, amt);

    // 生成二维码图片（不存 DB，仅在返回体中携带）
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
   * 支付宝异步通知回调处理（生产接入后取消注释并实现）
   *
   * @param params 支付宝回调参数（需验签）
   * @return "success" 表示处理成功
   */
  @Transactional
  public String handleAlipayNotify(Map<String, String> params) {
    // TODO: 1. 使用 AlipaySignature.rsaCheckV1(...) 验证签名
    //       2. 判断 trade_status == "TRADE_SUCCESS"
    //       3. 查询 out_trade_no 对应订单，校验金额
    //       4. 调用 confirmPayment(orderNo) 充值点数
    log.warn("[Payment] 支付宝回调收到，SDK 未集成，params={}", params);
    return "success";
  }

  /**
   * 微信支付异步通知回调处理
   *
   * @param body 微信回调原始 XML/JSON
   * @return 微信规定的成功响应体
   */
  @Transactional
  public String handleWechatNotify(String body) {
    // TODO: 1. 解密微信回调密文（WechatPayHttpClientBuilder + AES-256-GCM）
    //       2. 校验签名
    //       3. 查询 out_trade_no 对应订单，校验金额
    //       4. 调用 confirmPayment(orderNo) 充值点数
    log.warn("[Payment] 微信支付回调收到，SDK 未集成，body={}", body);
    return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
  }

  // ─────────────────────────── 私有工具方法 ───────────────────────────────────

  /**
   * 生成 QR 码图像，返回 Base64 Data URL
   *
   * @param content   二维码内容（订单标识或支付 URL）
   * @param payMethod ALIPAY / WECHAT（决定前景色）
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
