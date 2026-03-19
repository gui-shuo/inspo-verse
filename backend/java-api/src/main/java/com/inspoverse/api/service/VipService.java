package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.VipMembership;
import com.inspoverse.api.entity.VipOrder;
import com.inspoverse.api.entity.VipPlan;
import com.inspoverse.api.mapper.VipMembershipMapper;
import com.inspoverse.api.mapper.VipOrderMapper;
import com.inspoverse.api.mapper.VipPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * VIP服务
 */
@Service
@RequiredArgsConstructor
public class VipService {
  private final VipPlanMapper vipPlanMapper;
  private final VipOrderMapper vipOrderMapper;
  private final VipMembershipMapper vipMembershipMapper;

  /**
   * 获取所有上架的VIP套餐
   */
  public List<VipPlan> getAvailablePlans() {
    return vipPlanMapper.selectList(new LambdaQueryWrapper<VipPlan>()
        .eq(VipPlan::getStatus, 1)
        .eq(VipPlan::getIsDeleted, 0)
        .orderByAsc(VipPlan::getLevel)
        .orderByAsc(VipPlan::getDurationDays));
  }

  /**
   * 创建VIP订单
   */
  public VipOrder createOrder(Long userId, Long planId) {
    VipPlan plan = vipPlanMapper.selectById(planId);
    if (plan == null || plan.getIsDeleted() == 1 || plan.getStatus() != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在或已下架");
    }

    VipOrder order = new VipOrder();
    order.setOrderNo("VIP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    order.setUserId(userId);
    order.setVipPlanId(planId);
    order.setAmountCents(plan.getPriceCents());
    order.setPayStatus(0); // 待支付
    order.setCreatedAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    order.setIsDeleted(0);

    vipOrderMapper.insert(order);
    return order;
  }

  /**
   * 模拟支付（生产环境需对接真实支付网关）
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

    // 更新订单状态
    order.setPayStatus(1); // 支付成功
    order.setPayChannel("mock");
    order.setPaidAt(LocalDateTime.now());
    order.setUpdatedAt(LocalDateTime.now());
    vipOrderMapper.updateById(order);

    // 获取套餐信息
    VipPlan plan = vipPlanMapper.selectById(order.getVipPlanId());
    if (plan == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "套餐信息异常");
    }

    // 创建或延长会员
    VipMembership existing = vipMembershipMapper.selectOne(new LambdaQueryWrapper<VipMembership>()
        .eq(VipMembership::getUserId, userId)
        .eq(VipMembership::getStatus, 1)
        .eq(VipMembership::getIsDeleted, 0)
        .orderByDesc(VipMembership::getEndTime)
        .last("LIMIT 1"));

    LocalDateTime startTime;
    LocalDateTime endTime;

    if (existing != null && existing.getEndTime().isAfter(LocalDateTime.now())) {
      // 有效会员，延长时间
      startTime = existing.getEndTime();
      endTime = startTime.plusDays(plan.getDurationDays());
    } else {
      // 新会员或已过期
      startTime = LocalDateTime.now();
      endTime = startTime.plusDays(plan.getDurationDays());
    }

    VipMembership membership = new VipMembership();
    membership.setUserId(userId);
    membership.setVipLevel(plan.getLevel());
    membership.setStartTime(startTime);
    membership.setEndTime(endTime);
    membership.setSourceOrderNo(order.getOrderNo());
    membership.setStatus(1);
    membership.setCreatedAt(LocalDateTime.now());
    membership.setUpdatedAt(LocalDateTime.now());
    membership.setIsDeleted(0);

    vipMembershipMapper.insert(membership);
  }

  /**
   * 获取用户订单列表
   */
  public List<VipOrder> getUserOrders(Long userId) {
    return vipOrderMapper.selectList(new LambdaQueryWrapper<VipOrder>()
        .eq(VipOrder::getUserId, userId)
        .eq(VipOrder::getIsDeleted, 0)
        .orderByDesc(VipOrder::getCreatedAt));
  }

  /**
   * 获取用户当前有效会员信息
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

  /**
   * 检查用户VIP等级
   */
  public int getUserVipLevel(Long userId) {
    VipMembership membership = getCurrentMembership(userId);
    return membership != null ? membership.getVipLevel() : 0;
  }
}
