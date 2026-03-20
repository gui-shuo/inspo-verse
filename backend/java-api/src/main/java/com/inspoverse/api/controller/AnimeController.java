package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.AnimeOrder;
import com.inspoverse.api.entity.AnimeSeries;
import com.inspoverse.api.service.AnimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/anime")
@RequiredArgsConstructor
public class AnimeController {

  private final AnimeService animeService;

  @Value("${app.payment.mock-mode:true}")
  private boolean mockMode;

  // ── 公开接口：按星期获取番剧列表 ────────────────────────────────────────
  @GetMapping("/schedule")
  public ApiResponse<List<Map<String, Object>>> getSchedule(
      @RequestParam int day,
      HttpServletRequest request
  ) {
    List<AnimeSeries> list = animeService.getSchedule(day);
    Long userId = getUserIdOptional(request);

    // 批量获取追番/购买状态
    List<Long> ids = list.stream().map(AnimeSeries::getId).collect(Collectors.toList());
    Set<Long> subscribedIds = animeService.getSubscribedAnimeIds(userId, ids);
    Set<Long> purchasedIds = animeService.getPurchasedAnimeIds(userId, ids);

    List<Map<String, Object>> result = list.stream().map(a -> {
      Map<String, Object> m = serializeAnime(a);
      m.put("subscribed", subscribedIds.contains(a.getId()));
      m.put("purchased", purchasedIds.contains(a.getId()));
      return m;
    }).collect(Collectors.toList());

    return ApiResponse.success(result);
  }

  // ── 公开接口：热门番剧 ────────────────────────────────────────────────
  @GetMapping("/hot")
  public ApiResponse<List<Map<String, Object>>> getHot(
      @RequestParam(defaultValue = "20") int limit
  ) {
    List<AnimeSeries> list = animeService.getHotAnime(limit);
    List<Map<String, Object>> result = list.stream()
        .map(this::serializeAnime)
        .collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ── 公开接口：番剧详情 ────────────────────────────────────────────────
  @GetMapping("/{id}")
  public ApiResponse<Map<String, Object>> getDetail(
      @PathVariable Long id,
      HttpServletRequest request
  ) {
    AnimeSeries anime = animeService.getDetail(id);
    Long userId = getUserIdOptional(request);

    Map<String, Object> result = serializeAnime(anime);
    result.put("subscribed", animeService.isSubscribed(userId, id));
    result.put("purchased", animeService.hasPurchased(userId, id));
    return ApiResponse.success(result);
  }

  // ── 发布番剧 ──────────────────────────────────────────────────────────
  @PostMapping
  public ApiResponse<Map<String, Object>> createAnime(
      HttpServletRequest request,
      @Valid @RequestBody CreateAnimeRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    AnimeSeries anime = animeService.createAnime(
        userId, req.title(), req.description(), req.coverUrl(), req.heroUrl(),
        req.score(), req.scheduleDay(), req.updateTime(), req.currentEpisode(),
        req.status(), req.isPaid() != null && req.isPaid(),
        req.freeEpisodes() != null ? req.freeEpisodes() : 3,
        req.priceCents() != null ? req.priceCents() : 0,
        req.totalEpisodes() != null ? req.totalEpisodes() : 0
    );
    return ApiResponse.success(serializeAnime(anime));
  }

  // ── 编辑番剧 ──────────────────────────────────────────────────────────
  @PutMapping("/{id}")
  public ApiResponse<Map<String, Object>> updateAnime(
      HttpServletRequest request,
      @PathVariable Long id,
      @Valid @RequestBody UpdateAnimeRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    AnimeSeries anime = animeService.updateAnime(
        userId, id, req.title(), req.description(), req.coverUrl(), req.heroUrl(),
        req.score(), req.scheduleDay(), req.updateTime(), req.currentEpisode(),
        req.status(), req.isPaid(), req.freeEpisodes(), req.priceCents(), req.totalEpisodes()
    );
    return ApiResponse.success(serializeAnime(anime));
  }

  // ── 删除番剧 ──────────────────────────────────────────────────────────
  @DeleteMapping("/{id}")
  public ApiResponse<String> deleteAnime(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    animeService.deleteAnime(userId, id);
    return ApiResponse.success("删除成功");
  }

  // ── 追番 ──────────────────────────────────────────────────────────────
  @PostMapping("/{id}/subscribe")
  public ApiResponse<String> subscribe(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    animeService.subscribe(userId, id);
    return ApiResponse.success("追番成功");
  }

  // ── 取消追番 ──────────────────────────────────────────────────────────
  @DeleteMapping("/{id}/subscribe")
  public ApiResponse<String> unsubscribe(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    animeService.unsubscribe(userId, id);
    return ApiResponse.success("已取消追番");
  }

  // ── 我的追番列表 ──────────────────────────────────────────────────────
  @GetMapping("/my-subscriptions")
  public ApiResponse<List<Map<String, Object>>> getMySubscriptions(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<AnimeSeries> list = animeService.getMySubscriptions(userId);
    List<Map<String, Object>> result = list.stream()
        .map(this::serializeAnime)
        .collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ── 创建番剧支付订单 ──────────────────────────────────────────────────
  @PostMapping("/{id}/pay")
  public ApiResponse<Map<String, Object>> createPayOrder(
      HttpServletRequest request,
      @PathVariable Long id,
      @Valid @RequestBody PayAnimeRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    AnimeOrder order = animeService.createPayOrder(userId, id, req.payMethod());

    Map<String, Object> result = new HashMap<>();
    result.put("orderNo", order.getOrderNo());
    result.put("animeId", order.getAnimeId());
    result.put("animeTitle", order.getAnimeTitle());
    result.put("amountCents", order.getAmountCents());
    result.put("amount", BigDecimal.valueOf(order.getAmountCents(), 2));
    result.put("payMethod", order.getPayMethod());
    result.put("payUrl", order.getPayUrl());
    result.put("qrCode", order.getQrCodeDataUrl());
    result.put("expiredAt", order.getExpiredAt().toString());
    result.put("mockMode", mockMode);
    return ApiResponse.success(result);
  }

  // ── 查询番剧支付状态 ──────────────────────────────────────────────────
  @GetMapping("/order/status/{orderNo}")
  public ApiResponse<Map<String, Object>> queryOrderStatus(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    AnimeOrder order = animeService.queryOrderStatus(userId, orderNo);

    Map<String, Object> result = new HashMap<>();
    result.put("orderNo", order.getOrderNo());
    result.put("status", order.getStatus());
    result.put("animeId", order.getAnimeId());
    result.put("amountCents", order.getAmountCents());
    result.put("paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : null);
    return ApiResponse.success(result);
  }

  // ── Mock 支付确认 ─────────────────────────────────────────────────────
  @PostMapping("/order/mock-confirm/{orderNo}")
  public ApiResponse<String> mockConfirmOrder(
      HttpServletRequest request,
      @PathVariable String orderNo
  ) {
    Long userId = (Long) request.getAttribute("userId");
    animeService.mockConfirmOrder(userId, orderNo);
    return ApiResponse.success("支付成功");
  }

  // ── 我的番剧订单 ──────────────────────────────────────────────────────
  @GetMapping("/my-orders")
  public ApiResponse<List<Map<String, Object>>> getMyOrders(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<AnimeOrder> list = animeService.getUserAnimeOrders(userId);
    List<Map<String, Object>> result = list.stream().map(o -> {
      Map<String, Object> m = new HashMap<>();
      m.put("id", o.getId());
      m.put("orderNo", o.getOrderNo());
      m.put("orderType", "ANIME");
      m.put("animeId", o.getAnimeId());
      m.put("animeTitle", o.getAnimeTitle());
      m.put("amountCents", o.getAmountCents());
      m.put("amount", BigDecimal.valueOf(o.getAmountCents(), 2));
      m.put("payMethod", o.getPayMethod());
      m.put("status", o.getStatus());
      m.put("createdAt", o.getCreatedAt().toString());
      m.put("paidAt", o.getPaidAt() != null ? o.getPaidAt().toString() : null);
      return m;
    }).collect(Collectors.toList());
    return ApiResponse.success(result);
  }

  // ── 支付宝异步回调（番剧订单） ─────────────────────────────────────────
  @PostMapping("/notify/alipay")
  public String alipayNotify(@RequestParam Map<String, String> params) {
    // TODO: 生产环境验签 + 处理
    return "success";
  }

  // ── 微信支付异步回调（番剧订单） ───────────────────────────────────────
  @PostMapping("/notify/wechat")
  public String wechatNotify(@RequestBody String body) {
    // TODO: 生产环境解密 + 处理
    return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
  }

  // ── 辅助方法 ──────────────────────────────────────────────────────────

  private Map<String, Object> serializeAnime(AnimeSeries a) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", a.getId());
    m.put("seriesNo", a.getSeriesNo());
    m.put("title", a.getTitle());
    m.put("description", a.getDescription());
    m.put("coverUrl", a.getCoverUrl());
    m.put("heroUrl", a.getHeroUrl());
    m.put("authorName", a.getAuthorName());
    m.put("userId", a.getUserId());
    m.put("score", a.getScore());
    m.put("scheduleDay", a.getScheduleDay());
    m.put("updateTime", a.getUpdateTime());
    m.put("currentEpisode", a.getCurrentEpisode());
    m.put("status", a.getStatus());
    m.put("isPaid", a.getIsPaid() == 1);
    m.put("freeEpisodes", a.getFreeEpisodes());
    m.put("priceCents", a.getPriceCents());
    m.put("price", BigDecimal.valueOf(a.getPriceCents(), 2));
    m.put("totalEpisodes", a.getTotalEpisodes());
    m.put("viewCount", a.getViewCount());
    m.put("subscribeCount", a.getSubscribeCount());
    m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
    return m;
  }

  /** 安全获取 userId（可能为 null，用于公共接口中的可选功能） */
  private Long getUserIdOptional(HttpServletRequest request) {
    Object attr = request.getAttribute("userId");
    return attr instanceof Long ? (Long) attr : null;
  }

  // ── Request DTOs ──────────────────────────────────────────────────────

  record CreateAnimeRequest(
      @NotBlank String title,
      String description,
      String coverUrl,
      String heroUrl,
      BigDecimal score,
      @Min(0) @Max(6) int scheduleDay,
      String updateTime,
      String currentEpisode,
      String status,
      Boolean isPaid,
      Integer freeEpisodes,
      Integer priceCents,
      Integer totalEpisodes
  ) {}

  record UpdateAnimeRequest(
      String title,
      String description,
      String coverUrl,
      String heroUrl,
      BigDecimal score,
      Integer scheduleDay,
      String updateTime,
      String currentEpisode,
      String status,
      Boolean isPaid,
      Integer freeEpisodes,
      Integer priceCents,
      Integer totalEpisodes
  ) {}

  record PayAnimeRequest(
      @NotBlank @Pattern(regexp = "ALIPAY|WECHAT") String payMethod
  ) {}
}
