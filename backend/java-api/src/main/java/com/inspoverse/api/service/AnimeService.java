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
import com.inspoverse.api.entity.*;
import com.inspoverse.api.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimeService {

  private final AnimeSeriesMapper animeSeriesMapper;
  private final AnimeSubscriptionMapper animeSubscriptionMapper;
  private final AnimeOrderMapper animeOrderMapper;
  private final AnimePurchaseMapper animePurchaseMapper;
  private final UserMapper userMapper;

  @Value("${app.payment.mock-mode:true}")
  private boolean mockMode;

  private static final int COLOR_WECHAT = 0xFF07C160;
  private static final int COLOR_ALIPAY = 0xFF1677FF;

  // ==================== 番剧 CRUD ====================

  /**
   * 获取指定星期的番剧列表
   */
  public List<AnimeSeries> getSchedule(int day) {
    if (day < 0 || day > 6) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "day 必须在 0-6 之间");
    }
    return animeSeriesMapper.selectList(new LambdaQueryWrapper<AnimeSeries>()
        .eq(AnimeSeries::getScheduleDay, day)
        .orderByDesc(AnimeSeries::getSubscribeCount)
        .orderByDesc(AnimeSeries::getScore));
  }

  /**
   * 获取全部番剧（热门排序）
   */
  public List<AnimeSeries> getHotAnime(int limit) {
    return animeSeriesMapper.selectList(new LambdaQueryWrapper<AnimeSeries>()
        .orderByDesc(AnimeSeries::getSubscribeCount)
        .orderByDesc(AnimeSeries::getScore)
        .last("LIMIT " + Math.min(limit, 100)));
  }

  /**
   * 获取番剧详情
   */
  public AnimeSeries getDetail(Long animeId) {
    AnimeSeries anime = animeSeriesMapper.selectById(animeId);
    if (anime == null || anime.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "番剧不存在");
    }
    // 增加浏览量
    animeSeriesMapper.update(null, new LambdaUpdateWrapper<AnimeSeries>()
        .eq(AnimeSeries::getId, animeId)
        .setSql("view_count = view_count + 1"));
    return anime;
  }

  /**
   * 发布番剧
   */
  @Transactional
  public AnimeSeries createAnime(Long userId, String title, String description,
      String coverUrl, String heroUrl, BigDecimal score, int scheduleDay,
      String updateTime, String currentEpisode, String status,
      boolean isPaid, int freeEpisodes, int priceCents, int totalEpisodes) {

    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
    }

    String seriesNo = "AN" + System.currentTimeMillis()
        + ThreadLocalRandom.current().nextInt(1000, 9999);

    AnimeSeries anime = new AnimeSeries();
    anime.setSeriesNo(seriesNo);
    anime.setTitle(title);
    anime.setDescription(description);
    anime.setCoverUrl(coverUrl);
    anime.setHeroUrl(heroUrl);
    anime.setAuthorName(user.getNickname());
    anime.setUserId(userId);
    anime.setScore(score != null ? score : BigDecimal.ZERO);
    anime.setScheduleDay(scheduleDay);
    anime.setUpdateTime(updateTime);
    anime.setCurrentEpisode(currentEpisode);
    anime.setStatus(StringUtils.hasText(status) ? status : "ONGOING");
    anime.setIsPaid(isPaid ? 1 : 0);
    anime.setFreeEpisodes(freeEpisodes);
    anime.setPriceCents(priceCents);
    anime.setTotalEpisodes(totalEpisodes);
    anime.setViewCount(0);
    anime.setSubscribeCount(0);

    animeSeriesMapper.insert(anime);
    log.info("[Anime] 发布番剧 seriesNo={} title={} userId={}", seriesNo, title, userId);
    return anime;
  }

  /**
   * 编辑番剧（仅发布者或管理员可编辑）
   */
  @Transactional
  public AnimeSeries updateAnime(Long userId, Long animeId, String title, String description,
      String coverUrl, String heroUrl, BigDecimal score, Integer scheduleDay,
      String updateTime, String currentEpisode, String status,
      Boolean isPaid, Integer freeEpisodes, Integer priceCents, Integer totalEpisodes) {

    AnimeSeries anime = animeSeriesMapper.selectById(animeId);
    if (anime == null || anime.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "番剧不存在");
    }
    if (!anime.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此番剧");
    }

    if (StringUtils.hasText(title)) anime.setTitle(title);
    if (StringUtils.hasText(description)) anime.setDescription(description);
    if (StringUtils.hasText(coverUrl)) anime.setCoverUrl(coverUrl);
    if (StringUtils.hasText(heroUrl)) anime.setHeroUrl(heroUrl);
    if (score != null) anime.setScore(score);
    if (scheduleDay != null) anime.setScheduleDay(scheduleDay);
    if (StringUtils.hasText(updateTime)) anime.setUpdateTime(updateTime);
    if (StringUtils.hasText(currentEpisode)) anime.setCurrentEpisode(currentEpisode);
    if (StringUtils.hasText(status)) anime.setStatus(status);
    if (isPaid != null) anime.setIsPaid(isPaid ? 1 : 0);
    if (freeEpisodes != null) anime.setFreeEpisodes(freeEpisodes);
    if (priceCents != null) anime.setPriceCents(priceCents);
    if (totalEpisodes != null) anime.setTotalEpisodes(totalEpisodes);

    animeSeriesMapper.updateById(anime);
    log.info("[Anime] 更新番剧 id={} userId={}", animeId, userId);
    return anime;
  }

  /**
   * 删除番剧（逻辑删除）
   */
  @Transactional
  public void deleteAnime(Long userId, Long animeId) {
    AnimeSeries anime = animeSeriesMapper.selectById(animeId);
    if (anime == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "番剧不存在");
    }
    if (!anime.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此番剧");
    }
    animeSeriesMapper.deleteById(animeId);
    log.info("[Anime] 删除番剧 id={} userId={}", animeId, userId);
  }

  // ==================== 追番功能 ====================

  /**
   * 追番（订阅）
   */
  @Transactional
  public void subscribe(Long userId, Long animeId) {
    AnimeSeries anime = animeSeriesMapper.selectById(animeId);
    if (anime == null || anime.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "番剧不存在");
    }

    Long count = animeSubscriptionMapper.selectCount(new LambdaQueryWrapper<AnimeSubscription>()
        .eq(AnimeSubscription::getUserId, userId)
        .eq(AnimeSubscription::getAnimeId, animeId));
    if (count > 0) {
      return; // 已追番，幂等处理
    }

    AnimeSubscription sub = new AnimeSubscription();
    sub.setUserId(userId);
    sub.setAnimeId(animeId);
    animeSubscriptionMapper.insert(sub);

    // 更新追番计数
    animeSeriesMapper.update(null, new LambdaUpdateWrapper<AnimeSeries>()
        .eq(AnimeSeries::getId, animeId)
        .setSql("subscribe_count = subscribe_count + 1"));

    log.info("[Anime] 追番 userId={} animeId={}", userId, animeId);
  }

  /**
   * 取消追番
   */
  @Transactional
  public void unsubscribe(Long userId, Long animeId) {
    int deleted = animeSubscriptionMapper.delete(new LambdaQueryWrapper<AnimeSubscription>()
        .eq(AnimeSubscription::getUserId, userId)
        .eq(AnimeSubscription::getAnimeId, animeId));

    if (deleted > 0) {
      animeSeriesMapper.update(null, new LambdaUpdateWrapper<AnimeSeries>()
          .eq(AnimeSeries::getId, animeId)
          .setSql("subscribe_count = GREATEST(subscribe_count - 1, 0)"));
      log.info("[Anime] 取消追番 userId={} animeId={}", userId, animeId);
    }
  }

  /**
   * 检查用户是否已追番
   */
  public boolean isSubscribed(Long userId, Long animeId) {
    if (userId == null) return false;
    return animeSubscriptionMapper.selectCount(new LambdaQueryWrapper<AnimeSubscription>()
        .eq(AnimeSubscription::getUserId, userId)
        .eq(AnimeSubscription::getAnimeId, animeId)) > 0;
  }

  /**
   * 获取用户追番列表
   */
  public List<AnimeSeries> getMySubscriptions(Long userId) {
    List<AnimeSubscription> subs = animeSubscriptionMapper.selectList(
        new LambdaQueryWrapper<AnimeSubscription>()
            .eq(AnimeSubscription::getUserId, userId)
            .orderByDesc(AnimeSubscription::getCreatedAt));
    if (subs.isEmpty()) return Collections.emptyList();

    List<Long> animeIds = subs.stream()
        .map(AnimeSubscription::getAnimeId)
        .collect(Collectors.toList());
    return animeSeriesMapper.selectBatchIds(animeIds);
  }

  /**
   * 批量检查用户追番状态
   */
  public Set<Long> getSubscribedAnimeIds(Long userId, List<Long> animeIds) {
    if (userId == null || animeIds.isEmpty()) return Collections.emptySet();
    List<AnimeSubscription> subs = animeSubscriptionMapper.selectList(
        new LambdaQueryWrapper<AnimeSubscription>()
            .eq(AnimeSubscription::getUserId, userId)
            .in(AnimeSubscription::getAnimeId, animeIds));
    return subs.stream()
        .map(AnimeSubscription::getAnimeId)
        .collect(Collectors.toSet());
  }

  // ==================== 付费 / 购买 ====================

  /**
   * 检查用户是否已购买某番剧
   */
  public boolean hasPurchased(Long userId, Long animeId) {
    if (userId == null) return false;
    return animePurchaseMapper.selectCount(new LambdaQueryWrapper<AnimePurchase>()
        .eq(AnimePurchase::getUserId, userId)
        .eq(AnimePurchase::getAnimeId, animeId)) > 0;
  }

  /**
   * 创建番剧支付订单
   */
  @Transactional
  public AnimeOrder createPayOrder(Long userId, Long animeId, String payMethod) {
    if (!"ALIPAY".equals(payMethod) && !"WECHAT".equals(payMethod)) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的支付方式：" + payMethod);
    }

    AnimeSeries anime = animeSeriesMapper.selectById(animeId);
    if (anime == null || anime.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "番剧不存在");
    }
    if (anime.getIsPaid() == 0) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "该番剧为免费番剧");
    }

    // 已购买则不需要再支付
    if (hasPurchased(userId, animeId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "已购买此番剧");
    }

    // 检查重复未完成订单
    long pending = animeOrderMapper.selectCount(new LambdaQueryWrapper<AnimeOrder>()
        .eq(AnimeOrder::getUserId, userId)
        .eq(AnimeOrder::getAnimeId, animeId)
        .eq(AnimeOrder::getStatus, "PENDING")
        .gt(AnimeOrder::getExpiredAt, LocalDateTime.now()));
    if (pending > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "存在未完成的支付订单");
    }

    String orderNo = ("WECHAT".equals(payMethod) ? "AWX" : "AALI")
        + System.currentTimeMillis()
        + ThreadLocalRandom.current().nextInt(1000, 9999);

    AnimeOrder order = new AnimeOrder();
    order.setOrderNo(orderNo);
    order.setUserId(userId);
    order.setAnimeId(animeId);
    order.setAnimeTitle(anime.getTitle());
    order.setAmountCents(anime.getPriceCents());
    order.setPayMethod(payMethod);
    order.setStatus("PENDING");
    order.setExpiredAt(LocalDateTime.now().plusMinutes(5));

    if (mockMode) {
      order.setPayUrl("MOCK:" + orderNo);
    } else {
      // TODO: 生产环境集成真实支付宝/微信支付
      order.setPayUrl("MOCK:" + orderNo);
      log.warn("[AnimePayment] 生产 SDK 未集成, 仍使用 mock 模式");
    }

    animeOrderMapper.insert(order);
    log.info("[AnimePayment] 创建番剧支付订单 orderNo={} animeId={} userId={} amount={}分",
        orderNo, animeId, userId, anime.getPriceCents());

    order.setQrCodeDataUrl(generateQrCode(order.getPayUrl(), payMethod));
    return order;
  }

  /**
   * 查询番剧支付订单状态
   */
  public AnimeOrder queryOrderStatus(Long userId, String orderNo) {
    AnimeOrder order = animeOrderMapper.selectOne(new LambdaQueryWrapper<AnimeOrder>()
        .eq(AnimeOrder::getUserId, userId)
        .eq(AnimeOrder::getOrderNo, orderNo));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if ("PENDING".equals(order.getStatus()) && LocalDateTime.now().isAfter(order.getExpiredAt())) {
      order.setStatus("EXPIRED");
      animeOrderMapper.update(null, new LambdaUpdateWrapper<AnimeOrder>()
          .eq(AnimeOrder::getId, order.getId())
          .set(AnimeOrder::getStatus, "EXPIRED"));
    }
    return order;
  }

  /**
   * Mock 模式：模拟番剧支付成功
   */
  @Transactional
  public void mockConfirmOrder(Long userId, String orderNo) {
    if (!mockMode) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "非 mock 模式不支持此接口");
    }
    AnimeOrder order = animeOrderMapper.selectOne(new LambdaQueryWrapper<AnimeOrder>()
        .eq(AnimeOrder::getUserId, userId)
        .eq(AnimeOrder::getOrderNo, orderNo));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if ("PAID".equals(order.getStatus())) return;
    if (!"PENDING".equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "订单状态异常：" + order.getStatus());
    }

    // 更新订单状态
    animeOrderMapper.update(null, new LambdaUpdateWrapper<AnimeOrder>()
        .eq(AnimeOrder::getId, order.getId())
        .set(AnimeOrder::getStatus, "PAID")
        .set(AnimeOrder::getPaidAt, LocalDateTime.now()));

    // 记录购买
    AnimePurchase purchase = new AnimePurchase();
    purchase.setUserId(userId);
    purchase.setAnimeId(order.getAnimeId());
    purchase.setOrderNo(orderNo);
    animePurchaseMapper.insert(purchase);

    log.info("[AnimePayment] Mock 支付成功：orderNo={} userId={} animeId={}",
        orderNo, userId, order.getAnimeId());
  }

  /**
   * 获取用户的番剧订单列表
   */
  public List<AnimeOrder> getUserAnimeOrders(Long userId) {
    return animeOrderMapper.selectList(new LambdaQueryWrapper<AnimeOrder>()
        .eq(AnimeOrder::getUserId, userId)
        .orderByDesc(AnimeOrder::getCreatedAt)
        .last("LIMIT 50"));
  }

  /**
   * 批量检查购买状态
   */
  public Set<Long> getPurchasedAnimeIds(Long userId, List<Long> animeIds) {
    if (userId == null || animeIds.isEmpty()) return Collections.emptySet();
    List<AnimePurchase> purchases = animePurchaseMapper.selectList(
        new LambdaQueryWrapper<AnimePurchase>()
            .eq(AnimePurchase::getUserId, userId)
            .in(AnimePurchase::getAnimeId, animeIds));
    return purchases.stream()
        .map(AnimePurchase::getAnimeId)
        .collect(Collectors.toSet());
  }

  // ==================== 私有工具方法 ====================

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
      log.error("[AnimePayment] 生成二维码失败: {}", e.getMessage());
      return "";
    }
  }
}
