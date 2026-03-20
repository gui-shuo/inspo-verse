package com.inspoverse.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.ContentComment;
import com.inspoverse.api.entity.ContentItem;
import com.inspoverse.api.service.ContentService;
import com.inspoverse.api.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 发现内容控制器 —— 浏览/搜索/筛选/详情/点赞/评论/关注/分享
 */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {
  private final ContentService contentService;
  private final FileStorageService fileStorageService;

  // ==================== 内容列表（分页） ====================

  /**
   * 获取发现内容列表（支持分类、搜索、排序、分页）
   * 匿名可访问；已登录用户额外返回 isLiked 状态
   */
  @GetMapping("/explore")
  public ApiResponse<Map<String, Object>> getExploreContent(
      HttpServletRequest request,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "new") String sortBy,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    Long userId = (Long) request.getAttribute("userId");

    IPage<ContentItem> contentPage = contentService.getContentPage(category, keyword, sortBy, page, pageSize);

    // 批量获取点赞状态
    List<Long> ids = contentPage.getRecords().stream().map(ContentItem::getId).collect(Collectors.toList());
    Set<Long> likedIds = contentService.batchCheckLiked(userId, ids);

    List<Map<String, Object>> items = contentPage.getRecords().stream().map(item -> {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", item.getId());
      map.put("contentNo", item.getContentNo());
      map.put("title", item.getTitle());
      map.put("description", item.getDescription() != null
          ? (item.getDescription().length() > 200 ? item.getDescription().substring(0, 200) + "..." : item.getDescription())
          : "");
      map.put("image", item.getCoverUrl());
      map.put("images", item.getImages());
      map.put("category", item.getCategory());
      map.put("tag", item.getTag());
      map.put("likes", item.getLikeCount());
      map.put("comments", item.getCommentCount());
      map.put("views", item.getViewCount());
      map.put("isLiked", likedIds.contains(item.getId()));
      map.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : "");
      map.put("author", contentService.buildAuthorInfo(item.getUserId()));
      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(Map.of(
        "items", items,
        "total", contentPage.getTotal(),
        "page", contentPage.getCurrent(),
        "pageSize", contentPage.getSize()
    ));
  }

  // ==================== 内容详情 ====================

  /**
   * 获取内容详情（增加浏览数）
   */
  @GetMapping("/explore/{id}")
  public ApiResponse<Map<String, Object>> getContentDetail(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    ContentItem item = contentService.getContentDetail(id);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", item.getId());
    result.put("contentNo", item.getContentNo());
    result.put("title", item.getTitle());
    result.put("description", item.getDescription());
    result.put("image", item.getCoverUrl());
    result.put("images", item.getImages());
    result.put("category", item.getCategory());
    result.put("tag", item.getTag());
    result.put("likes", item.getLikeCount());
    result.put("comments", item.getCommentCount());
    result.put("views", item.getViewCount());
    result.put("isLiked", contentService.hasLiked(userId, id));
    result.put("isFollowed", contentService.hasFollowed(userId, item.getUserId()));
    result.put("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().toString() : "");
    result.put("author", contentService.buildAuthorInfo(item.getUserId()));

    return ApiResponse.success(result);
  }

  // ==================== 点赞 ====================

  /**
   * 点赞/取消点赞
   */
  @PostMapping("/explore/{id}/like")
  public ApiResponse<Map<String, Object>> toggleLike(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    boolean isLiked = contentService.toggleLike(userId, id);
    ContentItem item = contentService.getContentById(id);
    return ApiResponse.success(Map.of("isLiked", isLiked, "likes", item.getLikeCount()));
  }

  // ==================== 评论 ====================

  /**
   * 获取内容评论列表
   */
  @GetMapping("/explore/{id}/comments")
  public ApiResponse<Map<String, Object>> getComments(
      @PathVariable Long id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int pageSize
  ) {
    IPage<ContentComment> commentPage = contentService.getComments(id, page, pageSize);

    List<Map<String, Object>> comments = commentPage.getRecords().stream().map(c -> {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", c.getId());
      map.put("contentId", c.getContentId());
      map.put("userId", c.getUserId());
      map.put("content", c.getContent());
      map.put("parentCommentId", c.getParentCommentId());
      map.put("replyToUserId", c.getReplyToUserId());
      map.put("likeCount", c.getLikeCount());
      map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
      map.put("author", contentService.buildAuthorInfo(c.getUserId()));
      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(Map.of(
        "comments", comments,
        "total", commentPage.getTotal(),
        "page", commentPage.getCurrent(),
        "pageSize", commentPage.getSize()
    ));
  }

  /**
   * 发布评论
   */
  @PostMapping("/explore/{id}/comments")
  public ApiResponse<Map<String, Object>> addComment(
      HttpServletRequest request,
      @PathVariable Long id,
      @Valid @RequestBody AddCommentRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    ContentComment comment = contentService.addComment(id, userId, req.content(), req.parentCommentId(), req.replyToUserId());
    ContentItem item = contentService.getContentById(id);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("commentId", comment.getId());
    result.put("commentCount", item.getCommentCount());
    result.put("author", contentService.buildAuthorInfo(userId));
    return ApiResponse.success(result);
  }

  // ==================== 关注 ====================

  /**
   * 关注/取消关注作者
   */
  @PostMapping("/explore/{id}/follow")
  public ApiResponse<Map<String, Object>> toggleFollow(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    ContentItem item = contentService.getContentById(id);
    boolean isFollowed = contentService.toggleFollow(userId, item.getUserId());
    return ApiResponse.success(Map.of("isFollowed", isFollowed));
  }

  // ==================== 发布 ====================

  /**
   * 发布新内容
   */
  @PostMapping("/explore")
  public ApiResponse<Map<String, Object>> publishContent(
      HttpServletRequest request,
      @Valid @RequestBody PublishContentRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
    }
    ContentItem item = contentService.publishContent(
        userId, req.category(), req.title(), req.description(),
        req.coverUrl(), req.images(), req.tag());
    return ApiResponse.success(Map.of("id", item.getId(), "contentNo", item.getContentNo()));
  }

  /**
   * 上传内容图片
   */
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

  // ==================== 兼容旧接口 ====================

  @GetMapping("/anime")
  public ApiResponse<Map<String, Object>> getAnimeContent(
      HttpServletRequest request,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return getExploreContent(request, "anime", null, "new", page, pageSize);
  }

  @GetMapping("/games")
  public ApiResponse<Map<String, Object>> getGamesContent(
      HttpServletRequest request,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    return getExploreContent(request, "game", null, "new", page, pageSize);
  }

  // ==================== Request DTOs ====================

  public record AddCommentRequest(
      @NotBlank(message = "评论内容不能为空") String content,
      Long parentCommentId,
      Long replyToUserId
  ) {}

  public record PublishContentRequest(
      @NotBlank(message = "分类不能为空") String category,
      @NotBlank(message = "标题不能为空") String title,
      String description,
      String coverUrl,
      String images,
      String tag
  ) {}
}
