package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.PointTransaction;
import com.inspoverse.api.entity.UserPoints;
import com.inspoverse.api.mapper.PointTransactionMapper;
import com.inspoverse.api.mapper.UserPointsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 数字钱包（灵感点数）服务
 */
@Service
@RequiredArgsConstructor
public class WalletService {

  private final UserPointsMapper userPointsMapper;
  private final PointTransactionMapper transactionMapper;

  /**
   * 获取用户钱包，不存在则自动创建
   */
  public UserPoints getOrCreateWallet(Long userId) {
    UserPoints wallet = userPointsMapper.selectOne(new LambdaQueryWrapper<UserPoints>()
        .eq(UserPoints::getUserId, userId));
    if (wallet == null) {
      wallet = new UserPoints();
      wallet.setUserId(userId);
      wallet.setBalance(0);
      wallet.setTotalEarned(0);
      wallet.setTotalSpent(0);
      wallet.setUpdatedAt(LocalDateTime.now());
      userPointsMapper.insert(wallet);
    }
    return wallet;
  }

  /**
   * 获取收支明细（分页，默认最近20条）
   */
  public List<PointTransaction> getTransactions(Long userId, int limit) {
    return transactionMapper.selectList(new LambdaQueryWrapper<PointTransaction>()
        .eq(PointTransaction::getUserId, userId)
        .orderByDesc(PointTransaction::getCreatedAt)
        .last("LIMIT " + Math.min(limit, 100)));
  }

  /**
   * 本月消耗统计
   */
  public int getMonthlySpent(Long userId) {
    YearMonth current = YearMonth.now();
    LocalDateTime start = current.atDay(1).atStartOfDay();
    LocalDateTime end = current.atEndOfMonth().atTime(23, 59, 59);

    List<PointTransaction> monthly = transactionMapper.selectList(
        new LambdaQueryWrapper<PointTransaction>()
            .eq(PointTransaction::getUserId, userId)
            .lt(PointTransaction::getAmount, 0)
            .between(PointTransaction::getCreatedAt, start, end));

    return monthly.stream().mapToInt(t -> Math.abs(t.getAmount())).sum();
  }

  /**
   * 增加点数（签到、充值、VIP赠送等）
   */
  @Transactional
  public void earn(Long userId, int amount, String type, String description, String refId) {
    if (amount <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "增加点数必须为正整数");
    UserPoints wallet = getOrCreateWallet(userId);
    int newBalance = wallet.getBalance() + amount;
    wallet.setBalance(newBalance);
    wallet.setTotalEarned(wallet.getTotalEarned() + amount);
    wallet.setUpdatedAt(LocalDateTime.now());
    userPointsMapper.updateById(wallet);
    recordTransaction(userId, amount, type, description, newBalance, refId);
  }

  /**
   * 消耗点数（AI绘图、兑换权益等）
   */
  @Transactional
  public void spend(Long userId, int amount, String type, String description, String refId) {
    if (amount <= 0) throw new BusinessException(ErrorCode.PARAM_ERROR, "消耗点数必须为正整数");
    UserPoints wallet = getOrCreateWallet(userId);
    if (wallet.getBalance() < amount) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "灵感点数不足");
    }
    int newBalance = wallet.getBalance() - amount;
    wallet.setBalance(newBalance);
    wallet.setTotalSpent(wallet.getTotalSpent() + amount);
    wallet.setUpdatedAt(LocalDateTime.now());
    userPointsMapper.updateById(wallet);
    recordTransaction(userId, -amount, type, description, newBalance, refId);
  }

  /**
   * 充值（对接支付后调用）
   */
  @Transactional
  public void recharge(Long userId, int pointsAmount, String orderNo) {
    earn(userId, pointsAmount, "EARN_RECHARGE", "充值 " + pointsAmount + " 灵感点数", orderNo);
  }

  /**
   * 每日签到奖励
   */
  @Transactional
  public void dailySignIn(Long userId) {
    // 检查今日是否已签到
    LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
    long count = transactionMapper.selectCount(new LambdaQueryWrapper<PointTransaction>()
        .eq(PointTransaction::getUserId, userId)
        .eq(PointTransaction::getType, "EARN_SIGNIN")
        .ge(PointTransaction::getCreatedAt, todayStart));
    if (count > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "今日已签到");
    }
    earn(userId, 50, "EARN_SIGNIN", "每日签到奖励", null);
  }

  private void recordTransaction(Long userId, int amount, String type,
      String description, int balanceAfter, String refId) {
    PointTransaction tx = new PointTransaction();
    tx.setUserId(userId);
    tx.setAmount(amount);
    tx.setType(type);
    tx.setDescription(description);
    tx.setBalanceAfter(balanceAfter);
    tx.setRefId(refId);
    tx.setCreatedAt(LocalDateTime.now());
    transactionMapper.insert(tx);
  }
}
