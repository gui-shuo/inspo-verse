package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.ForumComment;
import com.inspoverse.api.entity.ForumInteraction;
import com.inspoverse.api.entity.ForumPost;
import com.inspoverse.api.entity.User;
import com.inspoverse.api.mapper.ForumCommentMapper;
import com.inspoverse.api.mapper.ForumInteractionMapper;
import com.inspoverse.api.mapper.ForumPostMapper;
import com.inspoverse.api.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 论坛服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForumService {
  private final ForumPostMapper forumPostMapper;
  private final ForumCommentMapper forumCommentMapper;
  private final ForumInteractionMapper forumInteractionMapper;
  private final UserMapper userMapper;
  private final StringRedisTemplate redisTemplate;

  private static final String ONLINE_USERS_KEY = "online:users";

  /**
   * 发布帖子
   */
  public ForumPost createPost(Long userId, String category, String title, String content, String tags) {
    ForumPost post = new ForumPost();
    post.setPostNo("POST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    post.setUserId(userId);
    post.setCategory(category);
    post.setTitle(title);
    post.setContent(content);
    post.setTags(tags);
    post.setViewCount(0);
    post.setLikeCount(0);
    post.setCommentCount(0);
    post.setIsTop(0);
    post.setIsEssence(0);
    post.setStatus(1);
    post.setCreatedAt(LocalDateTime.now());
    post.setUpdatedAt(LocalDateTime.now());
    post.setIsDeleted(0);

    forumPostMapper.insert(post);
    return post;
  }

  /**
   * 获取帖子列表（分页、支持分类、关键词搜索、热门/最新排序）
   */
  public IPage<ForumPost> getPostList(String category, String keyword, String sortBy, int pageNum, int pageSize) {
    Page<ForumPost> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<ForumPost>()
        .eq(StringUtils.hasText(category), ForumPost::getCategory, category)
        .eq(ForumPost::getStatus, 1);

    if (StringUtils.hasText(keyword)) {
      // 先找匹配关键词的用户ID列表（搜索昵称或用户名）
      List<Long> matchingUserIds = userMapper.selectList(
          new LambdaQueryWrapper<User>()
              .select(User::getId)
              .and(w -> w.like(User::getNickname, keyword)
                        .or().like(User::getUsername, keyword))
      ).stream().map(User::getId).collect(Collectors.toList());

      wrapper.and(w -> {
        w.like(ForumPost::getTitle, keyword);
        if (!matchingUserIds.isEmpty()) {
          w.or().in(ForumPost::getUserId, matchingUserIds);
        }
      });
    }

    // 置顶优先，然后按排序模式
    wrapper.orderByDesc(ForumPost::getIsTop);
    if ("hot".equals(sortBy)) {
      wrapper.orderByDesc(ForumPost::getLikeCount)
             .orderByDesc(ForumPost::getCommentCount)
             .orderByDesc(ForumPost::getViewCount);
    } else {
      wrapper.orderByDesc(ForumPost::getCreatedAt);
    }

    return forumPostMapper.selectPage(page, wrapper);
  }

  /**
   * 获取社区统计数据（今日发帖、在线用户数）
   */
  public Map<String, Object> getCommunityStats() {
    // 今日发帖数
    long todayPosts = forumPostMapper.selectCount(
        new LambdaQueryWrapper<ForumPost>()
            .eq(ForumPost::getStatus, 1)
            .apply("DATE(created_at) = CURDATE()")
    );

    // 在线用户数（从Redis有序集合中读取）
    long onlineUsers = 0;
    try {
      Long count = redisTemplate.opsForZSet().zCard(ONLINE_USERS_KEY);
      onlineUsers = count != null ? count : 0;
    } catch (Exception e) {
      log.warn("获取在线用户数失败: {}", e.getMessage());
    }

    return Map.of("todayPosts", todayPosts, "onlineUsers", onlineUsers);
  }

  /**
   * 获取帖子详情（增加浏览数）
   */
  @Transactional
  public ForumPost getPostDetail(Long postId) {
    ForumPost post = forumPostMapper.selectById(postId);
    if (post == null || post.getIsDeleted() == 1 || post.getStatus() != 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
    }

    // 增加浏览数
    post.setViewCount(post.getViewCount() + 1);
    post.setUpdatedAt(LocalDateTime.now());
    forumPostMapper.updateById(post);

    return post;
  }

  /**
   * 删除帖子（仅作者或管理员）
   */
  public void deletePost(Long postId, Long userId) {
    ForumPost post = forumPostMapper.selectById(postId);
    if (post == null || post.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
    }

    if (!post.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此帖子");
    }

    forumPostMapper.deleteById(postId);
  }

  /**
   * 发表评论
   */
  @Transactional
  public ForumComment createComment(Long postId, Long userId, String content, Long parentCommentId, Long replyToUserId) {
    // 检查帖子是否存在
    ForumPost post = forumPostMapper.selectById(postId);
    if (post == null || post.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
    }

    ForumComment comment = new ForumComment();
    comment.setPostId(postId);
    comment.setUserId(userId);
    comment.setContent(content);
    comment.setParentCommentId(parentCommentId != null ? parentCommentId : 0L);
    comment.setReplyToUserId(replyToUserId);
    comment.setLikeCount(0);
    comment.setStatus(1);
    comment.setCreatedAt(LocalDateTime.now());
    comment.setUpdatedAt(LocalDateTime.now());
    comment.setIsDeleted(0);

    forumCommentMapper.insert(comment);

    // 更新帖子评论数
    post.setCommentCount(post.getCommentCount() + 1);
    post.setUpdatedAt(LocalDateTime.now());
    forumPostMapper.updateById(post);

    return comment;
  }

  /**
   * 获取帖子评论列表
   */
  public IPage<ForumComment> getCommentList(Long postId, int pageNum, int pageSize) {
    Page<ForumComment> page = new Page<>(pageNum, pageSize);
    LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<ForumComment>()
        .eq(ForumComment::getPostId, postId)
        .eq(ForumComment::getStatus, 1)
        .orderByAsc(ForumComment::getCreatedAt);

    return forumCommentMapper.selectPage(page, wrapper);
  }

  /**
   * 互动操作（点赞/收藏）- 幂等设计
   */
  @Transactional
  public void interact(Long userId, String targetType, Long targetId, String actionType) {
    // 检查是否已存在
    ForumInteraction existing = forumInteractionMapper.selectOne(new LambdaQueryWrapper<ForumInteraction>()
        .eq(ForumInteraction::getUserId, userId)
        .eq(ForumInteraction::getTargetType, targetType)
        .eq(ForumInteraction::getTargetId, targetId)
        .eq(ForumInteraction::getActionType, actionType));

    if (existing != null) {
      // 已存在则取消（删除记录）
      forumInteractionMapper.deleteById(existing.getId());
      decrementCount(targetType, targetId, actionType);
    } else {
      // 不存在则创建
      ForumInteraction interaction = new ForumInteraction();
      interaction.setUserId(userId);
      interaction.setTargetType(targetType);
      interaction.setTargetId(targetId);
      interaction.setActionType(actionType);
      interaction.setCreatedAt(LocalDateTime.now());
      forumInteractionMapper.insert(interaction);
      incrementCount(targetType, targetId, actionType);
    }
  }

  /**
   * 检查用户是否已互动
   */
  public boolean hasInteracted(Long userId, String targetType, Long targetId, String actionType) {
    return forumInteractionMapper.selectCount(new LambdaQueryWrapper<ForumInteraction>()
        .eq(ForumInteraction::getUserId, userId)
        .eq(ForumInteraction::getTargetType, targetType)
        .eq(ForumInteraction::getTargetId, targetId)
        .eq(ForumInteraction::getActionType, actionType)) > 0;
  }

  private void incrementCount(String targetType, Long targetId, String actionType) {
    if ("post".equals(targetType)) {
      ForumPost post = forumPostMapper.selectById(targetId);
      if (post != null) {
        if ("like".equals(actionType)) {
          post.setLikeCount(post.getLikeCount() + 1);
        }
        post.setUpdatedAt(LocalDateTime.now());
        forumPostMapper.updateById(post);
      }
    } else if ("comment".equals(targetType)) {
      ForumComment comment = forumCommentMapper.selectById(targetId);
      if (comment != null && "like".equals(actionType)) {
        comment.setLikeCount(comment.getLikeCount() + 1);
        comment.setUpdatedAt(LocalDateTime.now());
        forumCommentMapper.updateById(comment);
      }
    }
  }

  private void decrementCount(String targetType, Long targetId, String actionType) {
    if ("post".equals(targetType)) {
      ForumPost post = forumPostMapper.selectById(targetId);
      if (post != null) {
        if ("like".equals(actionType)) {
          post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        }
        post.setUpdatedAt(LocalDateTime.now());
        forumPostMapper.updateById(post);
      }
    } else if ("comment".equals(targetType)) {
      ForumComment comment = forumCommentMapper.selectById(targetId);
      if (comment != null && "like".equals(actionType)) {
        comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        comment.setUpdatedAt(LocalDateTime.now());
        forumCommentMapper.updateById(comment);
      }
    }
  }
}
