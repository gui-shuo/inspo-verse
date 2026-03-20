package com.inspoverse.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.Game;
import com.inspoverse.api.entity.GameOrder;
import com.inspoverse.api.service.FileStorageService;
import com.inspoverse.api.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏控制器 —— 游戏列表/详情/发布/编辑/删除/收藏/购买支付
 */
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {
  private final GameService gameService;
  private final FileStorageService fileStorageService;

  @Value("${app.payment.mock-mode:true}")
  private boolean mockMode;

  // ==================== 游戏列表（分页，匿名可访问） ====================

  @GetMapping
  public ApiResponse<Map<String, Object>> getGameList(
      HttpServletRequest request,
      @RequestParam(required = false) String genre,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "new") String sortBy,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    Long userId = (Long) request.getAttribute("userId");

    IPage<Game> gamePage = gameService.getGamePage(genre, keyword, sortBy, page, pageSize);

    List<Long> ids = gamePage.getRecords().stream().map(Game::getId).collect(Collectors.toList());
    Set<Long> favIds = gameService.batchCheckFavorited(userId, ids);

    List<Map<String, Object>> items = gamePage.getRecords().stream().map(g -> {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", g.getId());
      map.put("gameNo", g.getGameNo());
      map.put("title", g.getTitle());
      map.put("genre", g.getGenre());
      map.put("description", g.getDescription() != null
          ? (g.getDescription().length() > 200 ? g.getDescription().substring(0, 200) + "..." : g.getDescription())
          : "");
      map.put("cover", g.getCoverUrl());
      map.put("hero", g.getHeroUrl());
      map.put("gameUrl", g.getGameUrl());
      map.put("tags", g.getTags());
      map.put("developer", g.getDeveloper());
      map.put("releaseDate", g.getReleaseDate());
      map.put("rating", g.getRating());
      map.put("ratingCount", g.getRatingCount());
      map.put("playCount", g.getPlayCount());
      map.put("favoriteCount", g.getFavoriteCount());
      map.put("isPaid", g.getIsPaid());
      map.put("priceCents", g.getPriceCents());
      map.put("trialMinutes", g.getTrialMinutes());
      map.put("isFavorited", favIds.contains(g.getId()));
      map.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
      map.put("author", gameService.buildAuthorInfo(g.getUserId()));
      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(Map.of(
        "items", items,
        "total", gamePage.getTotal(),
        "page", gamePage.getCurrent(),
        "pageSize", gamePage.getSize()
    ));
  }

  // ==================== 游戏详情（匿名可访问，增加浏览数） ====================

  @GetMapping("/{id}")
  public ApiResponse<Map<String, Object>> getGameDetail(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    Game g = gameService.getGameDetail(id);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", g.getId());
    result.put("gameNo", g.getGameNo());
    result.put("title", g.getTitle());
    result.put("genre", g.getGenre());
    result.put("description", g.getDescription());
    result.put("cover", g.getCoverUrl());
    result.put("hero", g.getHeroUrl());
    result.put("gameUrl", g.getGameUrl());
    result.put("tags", g.getTags());
    result.put("developer", g.getDeveloper());
    result.put("releaseDate", g.getReleaseDate());
    result.put("rating", g.getRating());
    result.put("ratingCount", g.getRatingCount());
    result.put("playCount", g.getPlayCount());
    result.put("favoriteCount", g.getFavoriteCount());
    result.put("isPaid", g.getIsPaid());
    result.put("priceCents", g.getPriceCents());
    result.put("trialMinutes", g.getTrialMinutes());
    result.put("isFavorited", gameService.hasFavorited(userId, id));
    result.put("isPurchased", gameService.hasPurchased(userId, id));
    result.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
    result.put("author", gameService.buildAuthorInfo(g.getUserId()));

    return ApiResponse.success(result);
  }

  // ==================== 发布游戏（需登录） ====================

  @PostMapping
  public ApiResponse<Map<String, Object>> publishGame(
      HttpServletRequest request,
      @Valid @RequestBody PublishGameRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }

    Game game = gameService.publishGame(
        userId, req.title(), req.genre(), req.description(),
        req.coverUrl(), req.heroUrl(), req.gameUrl(), req.tags(),
        req.developer(), req.releaseDate(),
        req.isPaid() != null ? req.isPaid() : 0,
        req.priceCents() != null ? req.priceCents() : 0,
        req.trialMinutes() != null ? req.trialMinutes() : 0
    );

    return ApiResponse.success(Map.of(
        "id", game.getId(),
        "gameNo", game.getGameNo()
    ));
  }

  // ==================== 编辑游戏（需登录 + 本人） ====================

  @PutMapping("/{id}")
  public ApiResponse<String> updateGame(
      HttpServletRequest request,
      @PathVariable Long id,
      @Valid @RequestBody UpdateGameRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }

    gameService.updateGame(userId, id, req.title(), req.genre(), req.description(),
        req.coverUrl(), req.heroUrl(), req.gameUrl(), req.tags(),
        req.developer(), req.releaseDate(), req.isPaid(), req.priceCents(), req.trialMinutes());

    return ApiResponse.success("更新成功");
  }

  // ==================== 删除游戏（需登录 + 本人） ====================

  @DeleteMapping("/{id}")
  public ApiResponse<String> deleteGame(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    gameService.deleteGame(userId, id);
    return ApiResponse.success("删除成功");
  }

  // ==================== 收藏/取消收藏 ====================

  @PostMapping("/{id}/favorite")
  public ApiResponse<Map<String, Object>> toggleFavorite(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    boolean isFavorited = gameService.toggleFavorite(userId, id);
    Game game = gameService.getGameById(id);
    return ApiResponse.success(Map.of(
        "isFavorited", isFavorited,
        "favoriteCount", game.getFavoriteCount()
    ));
  }

  // ==================== 游戏购买 - 创建支付订单 ====================

  @PostMapping("/{id}/purchase")
  public ApiResponse<Map<String, Object>> createPurchaseOrder(
      HttpServletRequest request,
      @PathVariable Long id,
      @Valid @RequestBody PurchaseRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }

    GameOrder order = gameService.createGameOrder(userId, id, req.payMethod());

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("orderNo", order.getOrderNo());
    result.put("gameId", order.getGameId());
    result.put("gameTitle", order.getGameTitle());
    result.put("amountCents", order.getAmountCents());
    result.put("amount", BigDecimal.valueOf(order.getAmountCents(), 2));
    result.put("payMethod", order.getPayMethod());
    result.put("qrCode", order.getQrCodeDataUrl());
    result.put("expiredAt", order.getExpiredAt().toString());
    result.put("mockMode", mockMode);
    return ApiResponse.success(result);
  }

  // ==================== 游戏购买 - 查询支付状态 ====================

  @GetMapping("/orders/{orderNo}/status")
  public ApiResponse<Map<String, Object>> queryOrderStatus(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    GameOrder order = gameService.queryGameOrderStatus(userId, orderNo);
    return ApiResponse.success(Map.of(
        "orderNo", order.getOrderNo(),
        "status", order.getStatus(),
        "gameId", order.getGameId(),
        "amountCents", order.getAmountCents(),
        "paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : ""
    ));
  }

  // ==================== 游戏购买 - Mock 确认 ====================

  @PostMapping("/orders/{orderNo}/mock-confirm")
  public ApiResponse<String> mockConfirm(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    gameService.mockConfirmGameOrder(userId, orderNo);
    return ApiResponse.success("支付成功");
  }

  // ==================== 回调接口（无需 JWT，由支付平台调用） ====================

  @PostMapping("/notify/alipay")
  public String alipayNotify(@RequestParam Map<String, String> params) {
    return gameService.handleAlipayNotify(params);
  }

  @PostMapping("/notify/wechat")
  public String wechatNotify(@RequestBody String body) {
    return gameService.handleWechatNotify(body);
  }

  // ==================== 获取我的游戏订单 ====================

  @GetMapping("/orders/my")
  public ApiResponse<List<Map<String, Object>>> getMyGameOrders(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    List<GameOrder> list = gameService.getUserGameOrders(userId);
    List<Map<String, Object>> result = list.stream().map(o -> {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", o.getId());
      m.put("orderNo", o.getOrderNo());
      m.put("orderType", "GAME");
      m.put("gameId", o.getGameId());
      m.put("gameTitle", o.getGameTitle());
      m.put("amountCents", o.getAmountCents());
      m.put("amount", BigDecimal.valueOf(o.getAmountCents(), 2));
      m.put("payMethod", o.getPayMethod());
      m.put("status", o.getStatus());
      m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
      m.put("paidAt", o.getPaidAt() != null ? o.getPaidAt().toString() : null);
      return m;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ==================== 获取我发布的游戏 ====================

  @GetMapping("/my")
  public ApiResponse<List<Map<String, Object>>> getMyGames(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    List<Game> list = gameService.getMyGames(userId);
    List<Map<String, Object>> result = list.stream().map(g -> {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", g.getId());
      map.put("gameNo", g.getGameNo());
      map.put("title", g.getTitle());
      map.put("genre", g.getGenre());
      map.put("cover", g.getCoverUrl());
      map.put("rating", g.getRating());
      map.put("playCount", g.getPlayCount());
      map.put("isPaid", g.getIsPaid());
      map.put("priceCents", g.getPriceCents());
      map.put("status", g.getStatus());
      map.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
      return map;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ==================== 上传游戏图片 ====================

  @PostMapping("/upload-image")
  public ApiResponse<Map<String, String>> uploadImage(
      HttpServletRequest request,
      @RequestParam("file") MultipartFile file
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    String url = fileStorageService.uploadPostImage(userId, file);
    return ApiResponse.success(Map.of("url", url));
  }

  // ==================== 请求体定义 ====================

  public record PublishGameRequest(
      @NotBlank String title,
      @NotBlank String genre,
      String description,
      String coverUrl,
      String heroUrl,
      String gameUrl,
      String tags,
      String developer,
      String releaseDate,
      Integer isPaid,
      Integer priceCents,
      Integer trialMinutes
  ) {}

  public record UpdateGameRequest(
      String title,
      String genre,
      String description,
      String coverUrl,
      String heroUrl,
      String gameUrl,
      String tags,
      String developer,
      String releaseDate,
      Integer isPaid,
      Integer priceCents,
      Integer trialMinutes
  ) {}

  public record PurchaseRequest(
      @NotBlank String payMethod
  ) {}
}
