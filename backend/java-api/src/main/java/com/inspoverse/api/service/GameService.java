package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

/**
 * 游戏服务 —— 游戏列表、详情、发布/编辑、收藏、购买支付
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {
  private final GameMapper gameMapper;
  private final GameOrderMapper gameOrderMapper;
  private final ForumInteractionMapper forumInteractionMapper;
  private final UserMapper userMapper;

  @Value("${app.payment.mock-mode:true}")
  private boolean mockMode;

  private static final int COLOR_WECHAT = 0xFF07C160;
  private static final int COLOR_ALIPAY = 0xFF1677FF;

  // ==================== 游戏列表 ====================

  public IPage<Game> getGamePage(String genre, String keyword, String sortBy, int pageNum, int pageSize) {
    Page<Game> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<Game> wrapper = new LambdaQueryWrapper<Game>()
        .eq(Game::getStatus, 1);

    if (StringUtils.hasText(genre) && !"all".equals(genre)) {
      wrapper.eq(Game::getGenre, genre);
    }
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w
          .like(Game::getTitle, keyword)
          .or().like(Game::getTags, keyword)
          .or().like(Game::getDeveloper, keyword));
    }

    if ("hot".equals(sortBy)) {
      wrapper.orderByDesc(Game::getPlayCount).orderByDesc(Game::getRating);
    } else if ("rating".equals(sortBy)) {
      wrapper.orderByDesc(Game::getRating);
    } else {
      wrapper.orderByDesc(Game::getCreatedAt);
    }

    return gameMapper.selectPage(page, wrapper);
  }

  // ==================== 游戏详情 ====================

  @Transactional
  public Game getGameDetail(Long id) {
    Game game = gameMapper.selectById(id);
    if (game == null || game.getIsDeleted() == 1 || game.getStatus() != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "游戏不存在");
    }
    game.setPlayCount(game.getPlayCount() + 1);
    gameMapper.updateById(game);
    return game;
  }

  public Game getGameById(Long id) {
    Game game = gameMapper.selectById(id);
    if (game == null || game.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "游戏不存在");
    }
    return game;
  }

  // ==================== 发布/编辑游戏 ====================

  public Game publishGame(Long userId, String title, String genre, String description,
                          String coverUrl, String heroUrl, String gameUrl, String tags,
                          String developer, String releaseDate, int isPaid, int priceCents,
                          int trialMinutes) {
    Game game = new Game();
    game.setGameNo("GAME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    game.setUserId(userId);
    game.setTitle(title);
    game.setGenre(genre);
    game.setDescription(description);
    game.setCoverUrl(coverUrl);
    game.setHeroUrl(heroUrl);
    game.setGameUrl(gameUrl);
    game.setTags(tags);
    game.setDeveloper(developer);
    game.setReleaseDate(releaseDate);
    game.setRating(BigDecimal.ZERO);
    game.setRatingCount(0);
    game.setPlayCount(0);
    game.setFavoriteCount(0);
    game.setIsPaid(isPaid);
    game.setPriceCents(priceCents);
    game.setTrialMinutes(trialMinutes);
    game.setStatus(1);
    game.setIsDeleted(0);

    gameMapper.insert(game);
    log.info("[Game] 发布游戏 gameNo={} title={} userId={}", game.getGameNo(), title, userId);
    return game;
  }

  public Game updateGame(Long userId, Long gameId, String title, String genre, String description,
                         String coverUrl, String heroUrl, String gameUrl, String tags,
                         String developer, String releaseDate, Integer isPaid, Integer priceCents,
                         Integer trialMinutes) {
    Game game = gameMapper.selectById(gameId);
    if (game == null || game.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "游戏不存在");
    }
    if (!game.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权编辑此游戏");
    }
    if (StringUtils.hasText(title)) game.setTitle(title);
    if (StringUtils.hasText(genre)) game.setGenre(genre);
    if (description != null) game.setDescription(description);
    if (coverUrl != null) game.setCoverUrl(coverUrl);
    if (heroUrl != null) game.setHeroUrl(heroUrl);
    if (gameUrl != null) game.setGameUrl(gameUrl);
    if (tags != null) game.setTags(tags);
    if (developer != null) game.setDeveloper(developer);
    if (releaseDate != null) game.setReleaseDate(releaseDate);
    if (isPaid != null) game.setIsPaid(isPaid);
    if (priceCents != null) game.setPriceCents(priceCents);
    if (trialMinutes != null) game.setTrialMinutes(trialMinutes);

    gameMapper.updateById(game);
    log.info("[Game] 更新游戏 id={} title={}", gameId, game.getTitle());
    return game;
  }

  public void deleteGame(Long userId, Long gameId) {
    Game game = gameMapper.selectById(gameId);
    if (game == null || game.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "游戏不存在");
    }
    if (!game.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此游戏");
    }
    gameMapper.deleteById(gameId);
    log.info("[Game] 删除游戏 id={}", gameId);
  }

  // ==================== 收藏（复用 forum_interaction 表） ====================

  @Transactional
  public boolean toggleFavorite(Long userId, Long gameId) {
    Game game = gameMapper.selectById(gameId);
    if (game == null || game.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "游戏不存在");
    }

    ForumInteraction existing = forumInteractionMapper.selectOne(
        new LambdaQueryWrapper<ForumInteraction>()
            .eq(ForumInteraction::getUserId, userId)
            .eq(ForumInteraction::getTargetType, "game")
            .eq(ForumInteraction::getTargetId, gameId)
            .eq(ForumInteraction::getActionType, "favorite"));

    if (existing != null) {
      forumInteractionMapper.deleteById(existing.getId());
      game.setFavoriteCount(Math.max(0, game.getFavoriteCount() - 1));
      gameMapper.updateById(game);
      return false;
    } else {
      ForumInteraction interaction = new ForumInteraction();
      interaction.setUserId(userId);
      interaction.setTargetType("game");
      interaction.setTargetId(gameId);
      interaction.setActionType("favorite");
      interaction.setCreatedAt(LocalDateTime.now());
      forumInteractionMapper.insert(interaction);
      game.setFavoriteCount(game.getFavoriteCount() + 1);
      gameMapper.updateById(game);
      return true;
    }
  }

  public boolean hasFavorited(Long userId, Long gameId) {
    if (userId == null) return false;
    return forumInteractionMapper.selectCount(
        new LambdaQueryWrapper<ForumInteraction>()
            .eq(ForumInteraction::getUserId, userId)
            .eq(ForumInteraction::getTargetType, "game")
            .eq(ForumInteraction::getTargetId, gameId)
            .eq(ForumInteraction::getActionType, "favorite")) > 0;
  }

  public Set<Long> batchCheckFavorited(Long userId, List<Long> gameIds) {
    if (userId == null || gameIds == null || gameIds.isEmpty()) {
      return Collections.emptySet();
    }
    List<ForumInteraction> list = forumInteractionMapper.selectList(
        new LambdaQueryWrapper<ForumInteraction>()
            .eq(ForumInteraction::getUserId, userId)
            .eq(ForumInteraction::getTargetType, "game")
            .in(ForumInteraction::getTargetId, gameIds)
            .eq(ForumInteraction::getActionType, "favorite"));
    return list.stream().map(ForumInteraction::getTargetId).collect(Collectors.toSet());
  }

  // ==================== 检查用户是否已购买游戏 ====================

  public boolean hasPurchased(Long userId, Long gameId) {
    if (userId == null) return false;
    return gameOrderMapper.selectCount(
        new LambdaQueryWrapper<GameOrder>()
            .eq(GameOrder::getUserId, userId)
            .eq(GameOrder::getGameId, gameId)
            .eq(GameOrder::getStatus, "PAID")) > 0;
  }

  // ==================== 游戏购买支付 ====================

  @Transactional
  public GameOrder createGameOrder(Long userId, Long gameId, String payMethod) {
    Game game = gameMapper.selectById(gameId);
    if (game == null || game.getIsDeleted() == 1 || game.getStatus() != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "游戏不存在");
    }
    if (game.getIsPaid() != 1 || game.getPriceCents() <= 0) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "该游戏为免费游戏，无需购买");
    }
    if (!"ALIPAY".equals(payMethod) && !"WECHAT".equals(payMethod)) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的支付方式");
    }

    // 检查是否已购买
    if (hasPurchased(userId, gameId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "您已购买该游戏");
    }

    // 检查未完成订单
    long pending = gameOrderMapper.selectCount(new LambdaQueryWrapper<GameOrder>()
        .eq(GameOrder::getUserId, userId)
        .eq(GameOrder::getGameId, gameId)
        .eq(GameOrder::getStatus, "PENDING")
        .gt(GameOrder::getExpiredAt, LocalDateTime.now()));
    if (pending > 0) {
      throw new BusinessException(ErrorCode.CONFLICT, "存在未完成的支付订单");
    }

    String orderNo = "GAME" + ("WECHAT".equals(payMethod) ? "WX" : "ALI")
        + System.currentTimeMillis()
        + ThreadLocalRandom.current().nextInt(1000, 9999);

    GameOrder order = new GameOrder();
    order.setOrderNo(orderNo);
    order.setUserId(userId);
    order.setGameId(gameId);
    order.setGameTitle(game.getTitle());
    order.setAmountCents(game.getPriceCents());
    order.setPayMethod(payMethod);
    order.setStatus("PENDING");
    order.setExpiredAt(LocalDateTime.now().plusMinutes(5));

    if (mockMode) {
      order.setPayUrl("MOCK:" + orderNo);
    } else {
      // TODO: 生产环境集成支付宝/微信支付 Native
      order.setPayUrl("MOCK:" + orderNo);
      log.warn("[Game] 生产 SDK 未集成，仍使用 mock 模式");
    }

    gameOrderMapper.insert(order);
    log.info("[Game] 创建购买订单 orderNo={} gameId={} userId={} amount={}分",
        orderNo, gameId, userId, game.getPriceCents());

    order.setQrCodeDataUrl(generateQrCode(order.getPayUrl(), payMethod));
    return order;
  }

  public GameOrder queryGameOrderStatus(Long userId, String orderNo) {
    GameOrder order = gameOrderMapper.selectOne(new LambdaQueryWrapper<GameOrder>()
        .eq(GameOrder::getUserId, userId)
        .eq(GameOrder::getOrderNo, orderNo));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if ("PENDING".equals(order.getStatus()) && LocalDateTime.now().isAfter(order.getExpiredAt())) {
      order.setStatus("EXPIRED");
      gameOrderMapper.update(null, new LambdaUpdateWrapper<GameOrder>()
          .eq(GameOrder::getId, order.getId())
          .set(GameOrder::getStatus, "EXPIRED"));
    }
    return order;
  }

  @Transactional
  public void mockConfirmGameOrder(Long userId, String orderNo) {
    if (!mockMode) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "非 mock 模式不支持此接口");
    }
    GameOrder order = gameOrderMapper.selectOne(new LambdaQueryWrapper<GameOrder>()
        .eq(GameOrder::getUserId, userId)
        .eq(GameOrder::getOrderNo, orderNo));
    if (order == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if ("PAID".equals(order.getStatus())) return;
    if (!"PENDING".equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "订单状态异常：" + order.getStatus());
    }

    gameOrderMapper.update(null, new LambdaUpdateWrapper<GameOrder>()
        .eq(GameOrder::getId, order.getId())
        .set(GameOrder::getStatus, "PAID")
        .set(GameOrder::getPaidAt, LocalDateTime.now()));

    log.info("[Game] Mock 支付成功：orderNo={} gameId={} userId={}", orderNo, order.getGameId(), userId);
  }

  @Transactional
  public String handleAlipayNotify(Map<String, String> params) {
    log.warn("[Game] 支付宝回调收到，SDK 未集成，params={}", params);
    return "success";
  }

  @Transactional
  public String handleWechatNotify(String body) {
    log.warn("[Game] 微信支付回调收到，SDK 未集成，body={}", body);
    return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
  }

  // ==================== 获取用户游戏订单 ====================

  public List<GameOrder> getUserGameOrders(Long userId) {
    return gameOrderMapper.selectList(new LambdaQueryWrapper<GameOrder>()
        .eq(GameOrder::getUserId, userId)
        .orderByDesc(GameOrder::getCreatedAt)
        .last("LIMIT 50"));
  }

  // ==================== 构建发布者信息 ====================

  public Map<String, Object> buildAuthorInfo(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
      return Map.of("id", 0, "nickname", "未知用户", "avatar", "");
    }
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", user.getId());
    info.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
    info.put("avatar", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
    return info;
  }

  // ==================== 获取我发布的游戏 ====================

  public List<Game> getMyGames(Long userId) {
    return gameMapper.selectList(new LambdaQueryWrapper<Game>()
        .eq(Game::getUserId, userId)
        .orderByDesc(Game::getCreatedAt));
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
      log.error("[Game] 生成二维码失败: {}", e.getMessage());
      return "";
    }
  }
}
