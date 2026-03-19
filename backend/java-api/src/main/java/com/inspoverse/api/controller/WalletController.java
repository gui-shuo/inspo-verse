package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.PointTransaction;
import com.inspoverse.api.entity.UserPoints;
import com.inspoverse.api.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数字钱包控制器（灵感点数）
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

  private final WalletService walletService;

  /**
   * 获取钱包信息（余额 + 本月消耗）
   */
  @GetMapping
  public ApiResponse<Map<String, Object>> getWallet(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    UserPoints wallet = walletService.getOrCreateWallet(userId);
    int monthlySpent = walletService.getMonthlySpent(userId);

    Map<String, Object> result = new HashMap<>();
    result.put("balance", wallet.getBalance());
    result.put("totalEarned", wallet.getTotalEarned());
    result.put("totalSpent", wallet.getTotalSpent());
    result.put("monthlySpent", monthlySpent);
    // 下个月度礼包阈值：每月消耗满1000pts赠送礼包
    int monthlyThreshold = 1000;
    result.put("monthlyThreshold", monthlyThreshold);
    result.put("monthlyProgress", Math.min((int) ((monthlySpent * 100.0) / monthlyThreshold), 100));
    result.put("monthlyRemaining", Math.max(monthlyThreshold - monthlySpent, 0));
    return ApiResponse.success(result);
  }

  /**
   * 获取收支明细（最近N条）
   */
  @GetMapping("/transactions")
  public ApiResponse<List<Map<String, Object>>> getTransactions(
      HttpServletRequest request,
      @RequestParam(defaultValue = "20") int limit
  ) {
    Long userId = (Long) request.getAttribute("userId");
    List<PointTransaction> txList = walletService.getTransactions(userId, limit);
    List<Map<String, Object>> result = txList.stream().map(tx -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", tx.getId());
      map.put("amount", tx.getAmount());
      map.put("type", tx.getType());
      map.put("description", tx.getDescription());
      map.put("balanceAfter", tx.getBalanceAfter());
      map.put("refId", tx.getRefId());
      map.put("createdAt", tx.getCreatedAt().toString());
      return map;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  /**
   * 每日签到（+50点）
   */
  @PostMapping("/sign-in")
  public ApiResponse<Map<String, Object>> dailySignIn(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    walletService.dailySignIn(userId);
    UserPoints wallet = walletService.getOrCreateWallet(userId);
    return ApiResponse.success(Map.of(
        "balance", wallet.getBalance(),
        "earned", 50,
        "message", "签到成功，获得50灵感点数"
    ));
  }

  /**
   * 充值（生产环境应先创建支付订单，支付回调后再调用此接口）
   * 当前为开发模式直接充值
   */
  @PostMapping("/recharge")
  public ApiResponse<Map<String, Object>> recharge(
      HttpServletRequest request,
      @Valid @RequestBody RechargeRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    String orderNo = "RECHARGE-" + System.currentTimeMillis();
    walletService.recharge(userId, req.points(), orderNo);
    UserPoints wallet = walletService.getOrCreateWallet(userId);
    return ApiResponse.success(Map.of(
        "balance", wallet.getBalance(),
        "recharged", req.points(),
        "orderNo", orderNo
    ));
  }

  public record RechargeRequest(
      @NotNull @Min(value = 1, message = "充值点数必须大于0") Integer points
  ) {}
}
