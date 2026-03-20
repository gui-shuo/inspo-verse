package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.*;
import com.inspoverse.api.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发现内容服务 —— 分类浏览、关键词搜索、排序、点赞/评论/关注/分享
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {
  private final ContentItemMapper contentItemMapper;
  private final ContentCommentMapper contentCommentMapper;
  private final ForumInteractionMapper forumInteractionMapper;
  private final UserFollowMapper userFollowMapper;
  private final UserMapper userMapper;

  // ==================== 内容列表 ====================

  /**
   * 分页获取内容列表（支持分类筛选、关键词搜索、排序）
   */
  public IPage<ContentItem> getContentPage(String category, String keyword, String sortBy, int pageNum, int pageSize) {
    Page<ContentItem> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<ContentItem> wrapper = new LambdaQueryWrapper<ContentItem>()
        .eq(ContentItem::getStatus, 1);

    // 分类筛选
    if (StringUtils.hasText(category) && !"all".equals(category)) {
      wrapper.eq(ContentItem::getCategory, category);
    }

    // 关键词搜索（标题 + 标签 + 描述）
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w
          .like(ContentItem::getTitle, keyword)
          .or().like(ContentItem::getTag, keyword)
          .or().like(ContentItem::getDescription, keyword));
    }

    // 排序
    if ("hot".equals(sortBy)) {
      wrapper.orderByDesc(ContentItem::getLikeCount)
             .orderByDesc(ContentItem::getCommentCount)
             .orderByDesc(ContentItem::getViewCount);
    } else {
      wrapper.orderByDesc(ContentItem::getCreatedAt);
    }

    return contentItemMapper.selectPage(page, wrapper);
  }

  /**
   * 获取内容列表（不分页，兼容旧接口）
   */
  public List<ContentItem> getContentByCategory(String category) {
    LambdaQueryWrapper<ContentItem> wrapper = new LambdaQueryWrapper<ContentItem>()
        .eq(ContentItem::getStatus, 1)
        .eq(ContentItem::getIsDeleted, 0)
        .orderByDesc(ContentItem::getCreatedAt);

    if (category != null && !"all".equals(category)) {
      wrapper.eq(ContentItem::getCategory, category);
    }

    return contentItemMapper.selectList(wrapper);
  }

  // ==================== 内容详情 ====================

  /**
   * 获取内容详情（增加浏览数）
   */
  @Transactional
  public ContentItem getContentDetail(Long id) {
    ContentItem item = contentItemMapper.selectById(id);
    if (item == null || item.getIsDeleted() == 1 || item.getStatus() != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
    }
    // 增加浏览数
    item.setViewCount(item.getViewCount() + 1);
    item.setUpdatedAt(LocalDateTime.now());
    contentItemMapper.updateById(item);
    return item;
  }

  /**
   * 获取内容详情（不增加浏览数）
   */
  public ContentItem getContentById(Long id) {
    ContentItem item = contentItemMapper.selectById(id);
    if (item == null || item.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
    }
    return item;
  }

  // ==================== 发布内容 ====================

  /**
   * 发布新内容
   */
  public ContentItem publishContent(Long userId, String category, String title, String description,
                                     String coverUrl, String images, String tag) {
    ContentItem item = new ContentItem();
    item.setContentNo("CNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    item.setUserId(userId);
    item.setCategory(category);
    item.setTitle(title);
    item.setDescription(description);
    item.setCoverUrl(coverUrl);
    item.setImages(images);
    item.setTag(tag);
    item.setLikeCount(0);
    item.setCommentCount(0);
    item.setViewCount(0);
    item.setStatus(1);
    item.setCreatedAt(LocalDateTime.now());
    item.setUpdatedAt(LocalDateTime.now());
    item.setIsDeleted(0);

    contentItemMapper.insert(item);
    return item;
  }

  // ==================== 点赞（复用 forum_interaction 表） ====================

  /**
   * 点赞/取消点赞（幂等切换）
   */
  @Transactional
  public boolean toggleLike(Long userId, Long contentId) {
    ContentItem item = contentItemMapper.selectById(contentId);
    if (item == null || item.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
    }

    ForumInteraction existing = forumInteractionMapper.selectOne(
        new LambdaQueryWrapper<ForumInteraction>()
            .eq(ForumInteraction::getUserId, userId)
            .eq(ForumInteraction::getTargetType, "content")
            .eq(ForumInteraction::getTargetId, contentId)
            .eq(ForumInteraction::getActionType, "like"));

    if (existing != null) {
      forumInteractionMapper.deleteById(existing.getId());
      item.setLikeCount(Math.max(0, item.getLikeCount() - 1));
      item.setUpdatedAt(LocalDateTime.now());
      contentItemMapper.updateById(item);
      return false; // 取消点赞
    } else {
      ForumInteraction interaction = new ForumInteraction();
      interaction.setUserId(userId);
      interaction.setTargetType("content");
      interaction.setTargetId(contentId);
      interaction.setActionType("like");
      interaction.setCreatedAt(LocalDateTime.now());
      forumInteractionMapper.insert(interaction);
      item.setLikeCount(item.getLikeCount() + 1);
      item.setUpdatedAt(LocalDateTime.now());
      contentItemMapper.updateById(item);
      return true; // 已点赞
    }
  }

  /**
   * 检查用户是否已点赞
   */
  public boolean hasLiked(Long userId, Long contentId) {
    if (userId == null) return false;
    return forumInteractionMapper.selectCount(
        new LambdaQueryWrapper<ForumInteraction>()
            .eq(ForumInteraction::getUserId, userId)
            .eq(ForumInteraction::getTargetType, "content")
            .eq(ForumInteraction::getTargetId, contentId)
            .eq(ForumInteraction::getActionType, "like")) > 0;
  }

  /**
   * 批量检查用户是否已点赞
   */
  public Set<Long> batchCheckLiked(Long userId, List<Long> contentIds) {
    if (userId == null || contentIds == null || contentIds.isEmpty()) {
      return Collections.emptySet();
    }
    List<ForumInteraction> interactions = forumInteractionMapper.selectList(
        new LambdaQueryWrapper<ForumInteraction>()
            .eq(ForumInteraction::getUserId, userId)
            .eq(ForumInteraction::getTargetType, "content")
            .eq(ForumInteraction::getActionType, "like")
            .in(ForumInteraction::getTargetId, contentIds));
    return interactions.stream()
        .map(ForumInteraction::getTargetId)
        .collect(Collectors.toSet());
  }

  // ==================== 评论 ====================

  /**
   * 发布评论
   */
  @Transactional
  public ContentComment addComment(Long contentId, Long userId, String text,
                                    Long parentCommentId, Long replyToUserId) {
    ContentItem item = contentItemMapper.selectById(contentId);
    if (item == null || item.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "内容不存在");
    }

    ContentComment comment = new ContentComment();
    comment.setContentId(contentId);
    comment.setUserId(userId);
    comment.setContent(text);
    comment.setParentCommentId(parentCommentId != null ? parentCommentId : 0L);
    comment.setReplyToUserId(replyToUserId);
    comment.setLikeCount(0);
    comment.setStatus(1);
    comment.setCreatedAt(LocalDateTime.now());
    comment.setUpdatedAt(LocalDateTime.now());
    comment.setIsDeleted(0);

    contentCommentMapper.insert(comment);

    // 更新评论计数
    item.setCommentCount(item.getCommentCount() + 1);
    item.setUpdatedAt(LocalDateTime.now());
    contentItemMapper.updateById(item);

    return comment;
  }

  /**
   * 获取评论列表（分页）
   */
  public IPage<ContentComment> getComments(Long contentId, int pageNum, int pageSize) {
    Page<ContentComment> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<ContentComment> wrapper = new LambdaQueryWrapper<ContentComment>()
        .eq(ContentComment::getContentId, contentId)
        .eq(ContentComment::getStatus, 1)
        .orderByDesc(ContentComment::getCreatedAt);
    return contentCommentMapper.selectPage(page, wrapper);
  }

  // ==================== 关注 ====================

  /**
   * 关注/取消关注（幂等切换）
   */
  @Transactional
  public boolean toggleFollow(Long followerId, Long followingId) {
    if (followerId.equals(followingId)) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "不能关注自己");
    }

    UserFollow existing = userFollowMapper.selectOne(
        new LambdaQueryWrapper<UserFollow>()
            .eq(UserFollow::getFollowerId, followerId)
            .eq(UserFollow::getFollowingId, followingId));

    if (existing != null) {
      userFollowMapper.deleteById(existing.getId());
      return false; // 取消关注
    } else {
      UserFollow follow = new UserFollow();
      follow.setFollowerId(followerId);
      follow.setFollowingId(followingId);
      follow.setCreatedAt(LocalDateTime.now());
      userFollowMapper.insert(follow);
      return true; // 已关注
    }
  }

  /**
   * 检查是否已关注
   */
  public boolean hasFollowed(Long followerId, Long followingId) {
    if (followerId == null || followingId == null) return false;
    return userFollowMapper.selectCount(
        new LambdaQueryWrapper<UserFollow>()
            .eq(UserFollow::getFollowerId, followerId)
            .eq(UserFollow::getFollowingId, followingId)) > 0;
  }

  // ==================== 工具方法 ====================

  /**
   * 构建作者信息 Map
   */
  public Map<String, Object> buildAuthorInfo(Long userId) {
    try {
      User user = userMapper.selectById(userId);
      if (user != null) {
        Map<String, Object> author = new HashMap<>();
        author.put("id", user.getId());
        author.put("username", user.getUsername());
        author.put("nickname", user.getNickname());
        author.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        author.put("bio", user.getBio() != null ? user.getBio() : "");
        return author;
      }
    } catch (Exception e) {
      log.warn("获取作者信息失败: userId={}, error={}", userId, e.getMessage());
    }
    return Map.of("id", 0, "username", "unknown", "nickname", "未知用户", "avatarUrl", "", "bio", "");
  }
}
