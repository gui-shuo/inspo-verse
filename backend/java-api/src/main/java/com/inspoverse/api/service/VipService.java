package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.*;
import com.inspoverse.api.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * VIP服务 — 完整支付流程集成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipService {
  private final VipPlanMapper vipPlanMapper;
  private final VipOrderMapper vipOrderMapper;
  private final VipMembershipMapper vipMembershipMapper;
  private final PaymentService paymentService;
  private final ExperienceService experienceService;
  private final WalletService walletService;

  public List<VipPlan> getAvailablePlans() {
    return vipPlanMapper.selectList(new LambdaQueryWrapper<VipPlan>()
        .eq(VipPlan::getStatus, 1)
        .eq(VipPlan::getIsDeleted, 0)
        .orderByAsc(VipPlan::getLevel)
        .orderByAsc(VipPlan::getDurationDays));
  }

  /**
   * 创建VIP订单并生成支付二维码
   *
   * @param userId    用户ID
   * @param planId    套餐ID
   * @param payMethod ALIPAY / WECHAT
   * @return 包含订单信息和支付二维码的Map
   */
  @Transactional
  public Map<String, Object> createOrderWithPayment(Long userId, Long planId, String payMethod) {
    VipPlan plan = vipPlanMapper.selectById(planId);
    if (plan == null || plan.getIsDeleted() == 1 || plan.getStatus() != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在或已下架");
    }
    if (!"ALIPAY".equals(payMethod) && !"WECHAT".equals(payMethod)) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的支付方式");
    }

    // 创建VIP订单
    VipOrder order = new VipOrder();
    order.setOrderNo("VIP-" + System.currentTimeMillis() + "-" +
        UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    order.setUserId(userId);
    order.setVipPlanId(planId);
    order.setAmountCents(plan.getPriceCents());
    order.setPayChannel(payMethod);
    order.setPayStatus(0); // 待支付
    order.setCreatedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    order.setIsDeleted(0);
    vipOrderMapper.insert(order);

    // 创建支付订单（复用 PaymentService 支付能力）
    PaymentOrder paymentOrder = paymentService.createVipPaymentOrder(
        userId, order.getOrderNo(), plan.getPlanName(),
        plan.getPriceCents(), payMethod);

    Map<String, Object> result = new HashMap<>();
    result.put("orderId", order.getId());
    result.put("orderNo", order.getOrderNo());
    result.put("amount", plan.getPriceCents() / 100.0);
    result.put("payStatus", 0);
    result.put("planName", plan.getPlanName());
    result.put("payMethod", payMethod);
    result.put("payUrl", paymentOrder.getPayUrl());
    result.put("qrCode", paymentOrder.getQrCodeDataUrl());
    result.put("paymentOrderNo", paymentOrder.getOrderNo());
    result.put("expiredAt", paymentOrder.getExpiredAt().toString());
    result.put("mockMode", paymentService.isMockMode());
    return result;
  }

  /**
   * VIP支付成功回调（由PaymentService确认支付后调用）
   */
  @Transactional
  public void onPaymentSuccess(String vipOrderNo, String payChannel) {
    VipOrder order = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
        .eq(VipOrder::getOrderNo, vipOrderNo)
        .eq(VipOrder::getIsDeleted, 0));
    if (order == null) {
      log.warn("[VIP] 支付回调找不到订单: {}", vipOrderNo);
      return;
    }
    if (order.getPayStatus() == 1) return; // 幂等

    // 更新订单状态
    order.setPayStatus(1);
    order.setPayChannel(payChannel);
    order.setPaidAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    vipOrderMapper.updateById(order);

    // 获取套餐信息
    VipPlan plan = vipPlanMapper.selectById(order.getVipPlanId());
    if (plan == null) {
      log.error("[VIP] 订单关联套餐不存在: orderId={} planId={}", order.getId(), order.getVipPlanId());
      return;
    }

    // 创建或延长会员
    activateMembership(order.getUserId(), plan, order.getOrderNo());

    // 赠送经验值
    experienceService.addExp(order.getUserId(), 100, "VIP_BUY",
        "开通" + plan.getPlanName(), order.getOrderNo());

    // 赠送VIP月度灵感点数
    if (plan.getLevel() >= 2) {
      walletService.earn(order.getUserId(), 1000, "EARN_VIP_GIFT",
          "黄金会员月度灵感点数赠送", order.getOrderNo());
    } else {
      walletService.earn(order.getUserId(), 300, "EARN_VIP_GIFT",
          "白银会员月度灵感点数赠送", order.getOrderNo());
    }

    log.info("[VIP] 会员开通成功: userId={} plan={} orderNo={}",
        order.getUserId(), plan.getPlanName(), vipOrderNo);
  }

  /**
   * 激活/延长会员
   */
  private void activateMembership(Long userId, VipPlan plan, String orderNo) {
    VipMembership existing = vipMembershipMapper.selectOne(new LambdaQueryWrapper<VipMembership>()
        .eq(VipMembership::getUserId, userId)
        .eq(VipMembership::getStatus, 1)
        .eq(VipMembership::getIsDeleted, 0)
        .orderByDesc(VipMembership::getEndTime)
        .last("LIMIT 1"));

    LocalDateTime startTime;
    LocalDateTime endTime;

    if (existing != null && existing.getEndTime().isAfter(LocalDateTime.now())) {
      startTime = existing.getEndTime();
      endTime = startTime.plusDays(plan.getDurationDays());
      // 如果新等级更高，升级现有会员等级
      if (plan.getLevel() > existing.getVipLevel()) {
        existing.setVipLevel(plan.getLevel());
        existing.setUpdatedAt(LocalDateTime.now());
        vipMembershipMapper.updateById(existing);
      }
    } else {
      startTime = LocalDateTime.now();
      endTime = startTime.plusDays(plan.getDurationDays());
    }

    VipMembership membership = new VipMembership();
    membership.setUserId(userId);
    membership.setVipLevel(plan.getLevel());
    membership.setStartTime(startTime);
    membership.setEndTime(endTime);
    membership.setSourceOrderNo(orderNo);
    membership.setStatus(1);
    membership.setCreatedAt(LocalDateTime.now());
    membership.setUpdatedAt(LocalDateTime.now());
    membership.setIsDeleted(0);
    vipMembershipMapper.insert(membership);
  }

  /**
   * 模拟支付（开发环境直接确认VIP）
   */
  @Transactional
  public void mockPay(Long orderId, Long userId) {
    VipOrder order = vipOrderMapper.selectById(orderId);
    if (order == null || order.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if (!order.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此订单");
    }
    if (order.getPayStatus() != 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "订单状态不允许支付");
    }
    onPaymentSuccess(order.getOrderNo(), "mock");
  }

  /**
   * 获取用户VIP订单列表
   */
  public List<VipOrder> getUserOrders(Long userId) {
    return vipOrderMapper.selectList(new LambdaQueryWrapper<VipOrder>()
        .eq(VipOrder::getUserId, userId)
        .eq(VipOrder::getIsDeleted, 0)
        .orderByDesc(VipOrder::getCreatedAt));
  }

  /**
   * 获取当前有效会员
   */
  public VipMembership getCurrentMembership(Long userId) {
    return vipMembershipMapper.selectOne(new LambdaQueryWrapper<VipMembership>()
        .eq(VipMembership::getUserId, userId)
        .eq(VipMembership::getStatus, 1)
        .eq(VipMembership::getIsDeleted, 0)
        .gt(VipMembership::getEndTime, LocalDateTime.now())
        .orderByDesc(VipMembership::getEndTime)
        .last("LIMIT 1"));
  }

  public int getUserVipLevel(Long userId) {
    VipMembership membership = getCurrentMembership(userId);
    return membership != null ? membership.getVipLevel() : 0;
  }

  public VipPlan getPlanById(Long planId) {
    return vipPlanMapper.selectById(planId);
  }

  /**
   * 查询VIP支付订单状态
   */
  public Map<String, Object> queryVipPaymentStatus(Long userId, String vipOrderNo) {
    VipOrder order = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
        .eq(VipOrder::getUserId, userId)
        .eq(VipOrder::getOrderNo, vipOrderNo)
        .eq(VipOrder::getIsDeleted, 0));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }

    Map<String, Object> result = new HashMap<>();
    result.put("orderNo", order.getOrderNo());
    result.put("payStatus", order.getPayStatus());
    result.put("payStatusText", getPayStatusText(order.getPayStatus()));
    result.put("paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : null);
    return result;
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
}
