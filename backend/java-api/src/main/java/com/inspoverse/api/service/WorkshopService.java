package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.ForumInteraction;
import com.inspoverse.api.entity.WorkshopProject;
import com.inspoverse.api.entity.WorkshopRating;
import com.inspoverse.api.entity.WorkshopSubscription;
import com.inspoverse.api.mapper.ForumInteractionMapper;
import com.inspoverse.api.mapper.WorkshopProjectMapper;
import com.inspoverse.api.mapper.WorkshopRatingMapper;
import com.inspoverse.api.mapper.WorkshopSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 创意工坊服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkshopService {

    private final WorkshopProjectMapper projectMapper;
    private final WorkshopRatingMapper ratingMapper;
    private final WorkshopSubscriptionMapper subscriptionMapper;
    private final ForumInteractionMapper interactionMapper;

    /**
     * 分页查询公开项目列表
     */
    public IPage<WorkshopProject> listProjects(String category, String keyword,
                                                String sortBy, int pageNum, int pageSize) {
        Page<WorkshopProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WorkshopProject> wrapper = new LambdaQueryWrapper<WorkshopProject>()
                .eq(WorkshopProject::getVisibility, 1)
                .eq(WorkshopProject::getStatus, 1)
                .eq(WorkshopProject::getIsDeleted, 0);

        if (StringUtils.hasText(category)) {
            wrapper.eq(WorkshopProject::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(WorkshopProject::getTitle, keyword)
                    .or().like(WorkshopProject::getDescription, keyword)
                    .or().like(WorkshopProject::getTags, keyword));
        }

        switch (sortBy != null ? sortBy : "hot") {
            case "new" -> wrapper.orderByDesc(WorkshopProject::getCreatedAt);
            case "rating" -> wrapper.orderByDesc(WorkshopProject::getRatingSum);
            default -> wrapper.orderByDesc(WorkshopProject::getDownloadCount);
        }

        return projectMapper.selectPage(page, wrapper);
    }

    /**
     * 获取项目详情
     */
    public WorkshopProject getProject(Long projectId) {
        WorkshopProject project = projectMapper.selectById(projectId);
        if (project == null || project.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        return project;
    }

    /**
     * 获取用户自己的项目列表
     */
    public IPage<WorkshopProject> getMyProjects(Long userId, int pageNum, int pageSize) {
        Page<WorkshopProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WorkshopProject> wrapper = new LambdaQueryWrapper<WorkshopProject>()
                .eq(WorkshopProject::getUserId, userId)
                .eq(WorkshopProject::getIsDeleted, 0)
                .orderByDesc(WorkshopProject::getCreatedAt);
        return projectMapper.selectPage(page, wrapper);
    }

    /**
     * 创建工坊项目
     */
    public WorkshopProject createProject(Long userId, String title, String description,
                                          String coverUrl, String tags, String version,
                                          String fileUrl, String fileSize, String category) {
        WorkshopProject project = new WorkshopProject();
        project.setProjectNo("WS-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        project.setUserId(userId);
        project.setTitle(title.trim());
        project.setDescription(description);
        project.setCoverUrl(coverUrl);
        project.setTags(tags);
        project.setVersion(version != null ? version.trim() : "v1.0.0");
        project.setFileUrl(fileUrl);
        project.setFileSize(fileSize);
        project.setCategory(category != null ? category.trim() : "other");
        project.setVisibility(1);
        project.setLikeCount(0);
        project.setFavoriteCount(0);
        project.setDownloadCount(0);
        project.setRatingSum(0);
        project.setRatingCount(0);
        project.setStatus(1);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.setIsDeleted(0);

        projectMapper.insert(project);
        return project;
    }

    /**
     * 更新项目
     */
    public WorkshopProject updateProject(Long userId, Long projectId, String title,
                                          String description, String coverUrl, String tags,
                                          String version, String fileUrl, String fileSize,
                                          String category) {
        WorkshopProject project = getProjectAndCheckOwner(userId, projectId);

        if (StringUtils.hasText(title)) project.setTitle(title.trim());
        if (description != null) project.setDescription(description);
        if (coverUrl != null) project.setCoverUrl(coverUrl);
        if (tags != null) project.setTags(tags);
        if (StringUtils.hasText(version)) project.setVersion(version.trim());
        if (fileUrl != null) project.setFileUrl(fileUrl);
        if (fileSize != null) project.setFileSize(fileSize);
        if (StringUtils.hasText(category)) project.setCategory(category.trim());
        project.setUpdatedAt(LocalDateTime.now());

        projectMapper.updateById(project);
        return project;
    }

    /**
     * 删除项目（逻辑删除）
     */
    public void deleteProject(Long userId, Long projectId) {
        getProjectAndCheckOwner(userId, projectId);
        LambdaUpdateWrapper<WorkshopProject> wrapper = new LambdaUpdateWrapper<WorkshopProject>()
                .eq(WorkshopProject::getId, projectId)
                .eq(WorkshopProject::getIsDeleted, 0)
                .set(WorkshopProject::getIsDeleted, 1)
                .set(WorkshopProject::getUpdatedAt, LocalDateTime.now());
        projectMapper.update(null, wrapper);
    }

    /**
     * 订阅/取消订阅项目
     */
    @Transactional
    public boolean toggleSubscription(Long userId, Long projectId) {
        WorkshopProject project = getProject(projectId);

        WorkshopSubscription existing = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<WorkshopSubscription>()
                        .eq(WorkshopSubscription::getUserId, userId)
                        .eq(WorkshopSubscription::getProjectId, projectId));

        if (existing != null) {
            subscriptionMapper.deleteById(existing.getId());
            projectMapper.update(null, new LambdaUpdateWrapper<WorkshopProject>()
                    .eq(WorkshopProject::getId, projectId)
                    .setSql("download_count = GREATEST(download_count - 1, 0)"));
            return false;
        } else {
            WorkshopSubscription sub = new WorkshopSubscription();
            sub.setUserId(userId);
            sub.setProjectId(projectId);
            sub.setCreatedAt(LocalDateTime.now());
            subscriptionMapper.insert(sub);
            projectMapper.update(null, new LambdaUpdateWrapper<WorkshopProject>()
                    .eq(WorkshopProject::getId, projectId)
                    .setSql("download_count = download_count + 1"));
            return true;
        }
    }

    /**
     * 点赞/取消点赞
     */
    @Transactional
    public boolean toggleLike(Long userId, Long projectId) {
        getProject(projectId);
        return toggleInteraction(userId, projectId, "like", "like_count");
    }

    /**
     * 收藏/取消收藏
     */
    @Transactional
    public boolean toggleFavorite(Long userId, Long projectId) {
        getProject(projectId);
        return toggleInteraction(userId, projectId, "favorite", "favorite_count");
    }

    /**
     * 评分
     */
    @Transactional
    public Map<String, Object> rateProject(Long userId, Long projectId, int score) {
        if (score < 1 || score > 10) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评分需在1-10之间");
        }
        getProject(projectId);

        WorkshopRating existing = ratingMapper.selectOne(
                new LambdaQueryWrapper<WorkshopRating>()
                        .eq(WorkshopRating::getUserId, userId)
                        .eq(WorkshopRating::getProjectId, projectId));

        if (existing != null) {
            int oldScore = existing.getScore();
            existing.setScore(score);
            existing.setUpdatedAt(LocalDateTime.now());
            ratingMapper.updateById(existing);
            // 更新项目评分总和  
            projectMapper.update(null, new LambdaUpdateWrapper<WorkshopProject>()
                    .eq(WorkshopProject::getId, projectId)
                    .setSql("rating_sum = rating_sum - " + oldScore + " + " + score));
        } else {
            WorkshopRating rating = new WorkshopRating();
            rating.setUserId(userId);
            rating.setProjectId(projectId);
            rating.setScore(score);
            rating.setCreatedAt(LocalDateTime.now());
            rating.setUpdatedAt(LocalDateTime.now());
            ratingMapper.insert(rating);
            projectMapper.update(null, new LambdaUpdateWrapper<WorkshopProject>()
                    .eq(WorkshopProject::getId, projectId)
                    .setSql("rating_sum = rating_sum + " + score + ", rating_count = rating_count + 1"));
        }

        // 返回最新评分信息
        WorkshopProject updated = projectMapper.selectById(projectId);
        double avgRating = updated.getRatingCount() > 0
                ? Math.round((double) updated.getRatingSum() / updated.getRatingCount() * 10.0) / 10.0
                : 0;
        return Map.of(
                "rating", avgRating,
                "ratingCount", updated.getRatingCount(),
                "myScore", score
        );
    }

    /**
     * 获取用户对某项目的交互状态
     */
    public Map<String, Object> getInteractionStatus(Long userId, Long projectId) {
        boolean liked = interactionMapper.selectCount(
                new LambdaQueryWrapper<ForumInteraction>()
                        .eq(ForumInteraction::getUserId, userId)
                        .eq(ForumInteraction::getTargetType, "project")
                        .eq(ForumInteraction::getTargetId, projectId)
                        .eq(ForumInteraction::getActionType, "like")) > 0;

        boolean favorited = interactionMapper.selectCount(
                new LambdaQueryWrapper<ForumInteraction>()
                        .eq(ForumInteraction::getUserId, userId)
                        .eq(ForumInteraction::getTargetType, "project")
                        .eq(ForumInteraction::getTargetId, projectId)
                        .eq(ForumInteraction::getActionType, "favorite")) > 0;

        boolean subscribed = subscriptionMapper.selectCount(
                new LambdaQueryWrapper<WorkshopSubscription>()
                        .eq(WorkshopSubscription::getUserId, userId)
                        .eq(WorkshopSubscription::getProjectId, projectId)) > 0;

        WorkshopRating rating = ratingMapper.selectOne(
                new LambdaQueryWrapper<WorkshopRating>()
                        .eq(WorkshopRating::getUserId, userId)
                        .eq(WorkshopRating::getProjectId, projectId));

        return Map.of(
                "liked", liked,
                "favorited", favorited,
                "subscribed", subscribed,
                "myScore", rating != null ? rating.getScore() : 0
        );
    }

    /**
     * 上传项目封面
     */
    public String uploadCover(Long userId, org.springframework.web.multipart.MultipartFile file,
                               FileStorageService fileStorageService) {
        return fileStorageService.uploadPostImage(userId, file);
    }

    // ========================= 内部方法 =========================

    private boolean toggleInteraction(Long userId, Long projectId,
                                       String actionType, String countField) {
        ForumInteraction existing = interactionMapper.selectOne(
                new LambdaQueryWrapper<ForumInteraction>()
                        .eq(ForumInteraction::getUserId, userId)
                        .eq(ForumInteraction::getTargetType, "project")
                        .eq(ForumInteraction::getTargetId, projectId)
                        .eq(ForumInteraction::getActionType, actionType));

        if (existing != null) {
            interactionMapper.deleteById(existing.getId());
            projectMapper.update(null, new LambdaUpdateWrapper<WorkshopProject>()
                    .eq(WorkshopProject::getId, projectId)
                    .setSql(countField + " = GREATEST(" + countField + " - 1, 0)"));
            return false;
        } else {
            ForumInteraction interaction = new ForumInteraction();
            interaction.setUserId(userId);
            interaction.setTargetType("project");
            interaction.setTargetId(projectId);
            interaction.setActionType(actionType);
            interaction.setCreatedAt(LocalDateTime.now());
            interactionMapper.insert(interaction);
            projectMapper.update(null, new LambdaUpdateWrapper<WorkshopProject>()
                    .eq(WorkshopProject::getId, projectId)
                    .setSql(countField + " = " + countField + " + 1"));
            return true;
        }
    }

    private WorkshopProject getProjectAndCheckOwner(Long userId, Long projectId) {
        WorkshopProject project = projectMapper.selectById(projectId);
        if (project == null || project.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此项目");
        }
        return project;
    }
}
