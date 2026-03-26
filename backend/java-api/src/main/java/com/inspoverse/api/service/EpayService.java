package com.inspoverse.api.service;

import com.inspoverse.api.config.EpayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 第三方易支付（Epay）API 服务
 * <p>
 * 支持：
 * - MAPI 接口发起支付（返回支付页面URL，前端可渲染为二维码或跳转）
 * - 异步通知签名验证
 * - 订单查询
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EpayService {

  private final EpayProperties epayProperties;

  /**
   * 生成 Epay 支付链接（通过 MAPI 拼接收银台 URL）
   *
   * @param outTradeNo 商户订单号
   * @param name       商品名称
   * @param money      金额（元）
   * @param notifyUrl  异步通知地址
   * @param returnUrl  同步返回地址
   * @param payType    支付类型：alipay / wxpay / qqpay
   * @return 完整支付页面 URL（可直接跳转或渲染为二维码）
   */
  public String createPayUrl(String outTradeNo, String name, BigDecimal money,
                             String notifyUrl, String returnUrl, String payType) {
    Map<String, String> params = new TreeMap<>();
    params.put("pid", epayProperties.getMerchantId());
    params.put("type", payType);
    params.put("out_trade_no", outTradeNo);
    params.put("notify_url", notifyUrl);
    params.put("return_url", returnUrl);
    params.put("name", name);
    params.put("money", money.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());

    String sign = generateSign(params);
    params.put("sign", sign);
    params.put("sign_type", "MD5");

    // 拼接为完整的收银台URL
    String queryString = params.entrySet().stream()
        .map(e -> e.getKey() + "=" + urlEncode(e.getValue()))
        .collect(Collectors.joining("&"));

    return epayProperties.getSubmitUrl() + "?" + queryString;
  }

  /**
   * 验证 Epay 异步通知签名
   *
   * @param params 通知参数（不含 sign 和 sign_type）
   * @param sign   签名值
   * @return 签名是否有效
   */
  public boolean verifyNotifySign(Map<String, String> params, String sign) {
    // 过滤 sign / sign_type / 空值
    Map<String, String> filtered = new TreeMap<>();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if ("sign".equals(key) || "sign_type".equals(key)) continue;
      if (value == null || value.isEmpty()) continue;
      filtered.put(key, value);
    }

    String expectedSign = generateSign(filtered);
    return expectedSign.equals(sign);
  }

  /**
   * 查询订单支付状态
   *
   * @param outTradeNo 商户订单号
   * @return 查询结果 Map（code=1 表示已支付）
   */
  public Map<String, String> queryOrder(String outTradeNo) {
    try {
      Map<String, String> params = new TreeMap<>();
      params.put("act", "order");
      params.put("pid", epayProperties.getMerchantId());
      params.put("out_trade_no", outTradeNo);

      String sign = generateSign(params);
      params.put("sign", sign);
      params.put("sign_type", "MD5");

      String queryString = params.entrySet().stream()
          .map(e -> e.getKey() + "=" + urlEncode(e.getValue()))
          .collect(Collectors.joining("&"));

      String apiUrl = epayProperties.getApiUrl() + "?" + queryString;

      URL url = URI.create(apiUrl).toURL();
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(10000);

      int statusCode = conn.getResponseCode();
      if (statusCode != 200) {
        log.error("[Epay] 查询订单失败 HTTP {}: orderNo={}", statusCode, outTradeNo);
        return Map.of("code", "-1", "msg", "HTTP " + statusCode);
      }

      String responseBody;
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        responseBody = reader.lines().collect(Collectors.joining());
      }

      // 简单解析 JSON（避免引入额外 JSON 库，Spring 环境中也可用 ObjectMapper）
      return parseSimpleJson(responseBody);
    } catch (Exception e) {
      log.error("[Epay] 查询订单异常: orderNo={}", outTradeNo, e);
      return Map.of("code", "-1", "msg", e.getMessage());
    }
  }

  /**
   * 生成 MD5 签名
   * 规则：按 key 字母排序 → key=value& 拼接 → 末尾追加 key=merchantKey → MD5
   */
  private String generateSign(Map<String, String> params) {
    // TreeMap 保证字母序
    String preSign = params.entrySet().stream()
        .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("&"));

    preSign += epayProperties.getMerchantKey();

    return md5(preSign);
  }

  private String md5(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException("MD5 计算失败", e);
    }
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  /**
   * 简易 JSON 解析（适用于 Epay 简单响应格式）
   */
  private Map<String, String> parseSimpleJson(String json) {
    Map<String, String> result = new HashMap<>();
    if (json == null || json.isEmpty()) return result;
    // 去掉首尾花括号
    json = json.trim();
    if (json.startsWith("{")) json = json.substring(1);
    if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

    // 按逗号分割键值对
    for (String pair : json.split(",")) {
      String[] kv = pair.split(":", 2);
      if (kv.length == 2) {
        String key = kv[0].trim().replace("\"", "");
        String value = kv[1].trim().replace("\"", "");
        result.put(key, value);
      }
    }
    return result;
  }
}
