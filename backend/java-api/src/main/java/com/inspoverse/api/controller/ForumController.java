package com.inspoverse.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.ForumComment;
import com.inspoverse.api.entity.ForumPost;
import com.inspoverse.api.service.ForumService;
import com.inspoverse.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 论坛控制器
 */
@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
public class ForumController {
  private final ForumService forumService;
  private final UserService userService;

  /**
   * 发布帖子
   */
  @PostMapping("/posts")
  public ApiResponse<Map<String, Object>> createPost(
      HttpServletRequest request,
      @Valid @RequestBody CreatePostRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    ForumPost post = forumService.createPost(userId, req.category(), req.title(), req.content(), req.tags());

    return ApiResponse.success(Map.of(
        "postId", post.getId(),
        "postNo", post.getPostNo()
    ));
  }

  /**
   * 获取帖子列表
   */
  @GetMapping("/posts")
  public ApiResponse<Map<String, Object>> getPostList(
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize
  ) {
    IPage<ForumPost> postPage = forumService.getPostList(category, page, pageSize);

    List<Map<String, Object>> posts = postPage.getRecords().stream().map(post -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", post.getId());
      map.put("postNo", post.getPostNo());
      map.put("userId", post.getUserId());
      map.put("category", post.getCategory());
      map.put("title", post.getTitle());
      map.put("content", post.getContent().length() > 200 ? post.getContent().substring(0, 200) + "..." : post.getContent());
      map.put("tags", post.getTags());
      map.put("viewCount", post.getViewCount());
      map.put("likeCount", post.getLikeCount());
      map.put("commentCount", post.getCommentCount());
      map.put("isTop", post.getIsTop());
      map.put("isEssence", post.getIsEssence());
      map.put("createdAt", post.getCreatedAt().toString());

      // 获取作者信息
      try {
        var user = userService.getUserById(post.getUserId());
        map.put("author", Map.of(
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        ));
      } catch (Exception e) {
        map.put("author", Map.of("username", "unknown", "nickname", "未知用户", "avatarUrl", ""));
      }

      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(Map.of(
        "posts", posts,
        "total", postPage.getTotal(),
        "page", postPage.getCurrent(),
        "pageSize", postPage.getSize()
    ));
  }

  /**
   * 获取帖子详情
   */
  @GetMapping("/posts/{id}")
  public ApiResponse<Map<String, Object>> getPostDetail(@PathVariable Long id) {
    ForumPost post = forumService.getPostDetail(id);

    Map<String, Object> result = new HashMap<>();
    result.put("id", post.getId());
    result.put("postNo", post.getPostNo());
    result.put("userId", post.getUserId());
    result.put("category", post.getCategory());
    result.put("title", post.getTitle());
    result.put("content", post.getContent());
    result.put("tags", post.getTags());
    result.put("viewCount", post.getViewCount());
    result.put("likeCount", post.getLikeCount());
    result.put("commentCount", post.getCommentCount());
    result.put("isTop", post.getIsTop());
    result.put("isEssence", post.getIsEssence());
    result.put("createdAt", post.getCreatedAt().toString());

    // 获取作者信息
    try {
      var user = userService.getUserById(post.getUserId());
      result.put("author", Map.of(
          "username", user.getUsername(),
          "nickname", user.getNickname(),
          "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
      ));
    } catch (Exception e) {
      result.put("author", Map.of("username", "unknown", "nickname", "未知用户", "avatarUrl", ""));
    }

    return ApiResponse.success(result);
  }

  /**
   * 删除帖子
   */
  @DeleteMapping("/posts/{id}")
  public ApiResponse<Void> deletePost(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    forumService.deletePost(id, userId);
    return ApiResponse.success(null);
  }

  /**
   * 发表评论
   */
  @PostMapping("/comments")
  public ApiResponse<Map<String, Object>> createComment(
      HttpServletRequest request,
      @Valid @RequestBody CreateCommentRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    ForumComment comment = forumService.createComment(
        req.postId(),
        userId,
        req.content(),
        req.parentCommentId(),
        req.replyToUserId()
    );

    return ApiResponse.success(Map.of(
        "commentId", comment.getId()
    ));
  }

  /**
   * 获取评论列表
   */
  @GetMapping("/comments")
  public ApiResponse<Map<String, Object>> getCommentList(
      @RequestParam Long postId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "50") int pageSize
  ) {
    IPage<ForumComment> commentPage = forumService.getCommentList(postId, page, pageSize);

    List<Map<String, Object>> comments = commentPage.getRecords().stream().map(comment -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", comment.getId());
      map.put("postId", comment.getPostId());
      map.put("userId", comment.getUserId());
      map.put("content", comment.getContent());
      map.put("parentCommentId", comment.getParentCommentId());
      map.put("replyToUserId", comment.getReplyToUserId());
      map.put("likeCount", comment.getLikeCount());
      map.put("createdAt", comment.getCreatedAt().toString());

      // 获取评论者信息
      try {
        var user = userService.getUserById(comment.getUserId());
        map.put("author", Map.of(
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
        ));
      } catch (Exception e) {
        map.put("author", Map.of("username", "unknown", "nickname", "未知用户", "avatarUrl", ""));
      }

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
   * 互动操作（点赞/收藏）
   */
  @PostMapping("/interactions")
  public ApiResponse<Void> interact(
      HttpServletRequest request,
      @Valid @RequestBody InteractRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    forumService.interact(userId, req.targetType(), req.targetId(), req.actionType());
    return ApiResponse.success(null);
  }

  /**
   * 检查是否已互动
   */
  @GetMapping("/interactions/check")
  public ApiResponse<Map<String, Boolean>> checkInteraction(
      HttpServletRequest request,
      @RequestParam String targetType,
      @RequestParam Long targetId,
      @RequestParam String actionType
  ) {
    Long userId = (Long) request.getAttribute("userId");
    boolean hasInteracted = forumService.hasInteracted(userId, targetType, targetId, actionType);
    return ApiResponse.success(Map.of("hasInteracted", hasInteracted));
  }

  public record CreatePostRequest(
      @NotBlank(message = "分类不能为空") String category,
      @NotBlank(message = "标题不能为空") String title,
      @NotBlank(message = "内容不能为空") String content,
      String tags
  ) {}

  public record CreateCommentRequest(
      @NotNull(message = "帖子ID不能为空") Long postId,
      @NotBlank(message = "评论内容不能为空") String content,
      Long parentCommentId,
      Long replyToUserId
  ) {}

  public record InteractRequest(
      @NotBlank(message = "目标类型不能为空") String targetType,
      @NotNull(message = "目标ID不能为空") Long targetId,
      @NotBlank(message = "操作类型不能为空") String actionType
  ) {}
}
