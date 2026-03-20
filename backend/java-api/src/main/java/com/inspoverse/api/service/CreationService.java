package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.entity.UserCreation;
import com.inspoverse.api.mapper.UserCreationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户创作服务
 */
@Service
@RequiredArgsConstructor
public class CreationService {

  private final UserCreationMapper creationMapper;
  private final FileStorageService fileStorageService;

  /**
   * 获取用户所有创作
   */
  public List<UserCreation> getUserCreations(Long userId) {
    return creationMapper.selectList(new LambdaQueryWrapper<UserCreation>()
        .eq(UserCreation::getUserId, userId)
        .eq(UserCreation::getIsDeleted, 0)
        .orderByDesc(UserCreation::getCreatedAt));
  }

  /**
   * 上传新创作
   */
  public UserCreation create(Long userId, MultipartFile file, String title,
      String description, Integer visibility) {
    String fileUrl = fileStorageService.uploadCreation(userId, file);
    String fileType = resolveFileType(file.getContentType());

    UserCreation creation = new UserCreation();
    creation.setUserId(userId);
    creation.setTitle(title != null ? title.trim() : file.getOriginalFilename());
    creation.setDescription(description);
    creation.setFileUrl(fileUrl);
    creation.setCoverUrl(isImage(file.getContentType()) ? fileUrl : null);
    creation.setFileType(fileType);
    creation.setFileSize(file.getSize());
    creation.setVisibility(visibility != null ? visibility : 0);
    creation.setStatus(1);
    creation.setCreatedAt(LocalDateTime.now());
    creation.setUpdatedAt(LocalDateTime.now());
    creation.setIsDeleted(0);

    creationMapper.insert(creation);
    return creation;
  }

  /**
   * 更新可见性
   */
  public void updateVisibility(Long userId, Long creationId, Integer visibility) {
    UserCreation creation = getCreationAndCheckOwner(userId, creationId);
    creation.setVisibility(visibility);
    creation.setUpdatedAt(LocalDateTime.now());
    creationMapper.updateById(creation);
  }

  /**
   * 逻辑删除创作
   * 注意：必须使用 deleteById 而非手动设置 isDeleted=1 + updateById
   * 原因：@TableLogic 会将 is_deleted 字段从 UPDATE SET 子句中排除，updateById 不会实际写入 is_deleted=1
   */
  public void delete(Long userId, Long creationId) {
    // 先验证归属权
    getCreationAndCheckOwner(userId, creationId);
    // 使用 deleteById，MyBatis-Plus 自动生成 UPDATE ... SET is_deleted=1 WHERE id=? AND is_deleted=0
    creationMapper.deleteById(creationId);
  }

  /**
   * 获取创作详情（含权限校验）
   */
  public UserCreation getForDownload(Long userId, Long creationId) {
    return getCreationAndCheckOwner(userId, creationId);
  }

  private UserCreation getCreationAndCheckOwner(Long userId, Long creationId) {
    UserCreation creation = creationMapper.selectById(creationId);
    if (creation == null || creation.getIsDeleted() == 1) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "创作不存在");
    }
    if (!creation.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此创作");
    }
    return creation;
  }

  private String resolveFileType(String contentType) {
    if (contentType == null) return "other";
    if (contentType.startsWith("image/")) return "image";
    if (contentType.startsWith("video/")) return "video";
    if (contentType.startsWith("audio/")) return "audio";
    return "other";
  }

  private boolean isImage(String contentType) {
    return contentType != null && contentType.startsWith("image/");
  }
}
