package com.inspoverse.api.service;

import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务
 * 支持本地存储（开发模式）与腾讯云COS（生产模式）双模式
 * 通过 tencent.cos.enabled 开关切换
 */
@Slf4j
@Service
public class FileStorageService {

  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
  );
  private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024L;    // 5MB
  private static final long MAX_CREATION_SIZE = 50 * 1024 * 1024L; // 50MB

  @Value("${tencent.cos.enabled:false}")
  private boolean cosEnabled;

  @Value("${tencent.cos.secret-id:}")
  private String cosSecretId;

  @Value("${tencent.cos.secret-key:}")
  private String cosSecretKey;

  @Value("${tencent.cos.region:ap-beijing}")
  private String cosRegion;

  @Value("${tencent.cos.bucket:}")
  private String cosBucket;

  @Value("${local.upload.path:/tmp/inspo-uploads}")
  private String localUploadPath;

  @Value("${local.base-url:http://localhost:8080/uploads}")
  private String localBaseUrl;

  private COSClient cosClient;

  @PostConstruct
  public void init() throws IOException {
    if (cosEnabled && cosSecretId != null && !cosSecretId.isEmpty()) {
      // 生产模式：仅使用腾讯云 COS
      COSCredentials cred = new BasicCOSCredentials(cosSecretId, cosSecretKey);
      ClientConfig clientConfig = new ClientConfig(new Region(cosRegion));
      cosClient = new COSClient(cred, clientConfig);
      log.info("腾讯云 COS 已启用: region={} bucket={}", cosRegion, cosBucket);
    } else {
      // 开发模式：使用本地文件存储
      Path resolved = Paths.get(localUploadPath).toAbsolutePath();
      Files.createDirectories(resolved);
      localUploadPath = resolved.toString();
      log.info("COS 未启用，使用本地文件存储: {}", localUploadPath);
    }
  }

  @PreDestroy
  public void destroy() {
    if (cosClient != null) {
      cosClient.shutdown();
    }
  }

  public String uploadAvatar(Long userId, MultipartFile file) {
    validateImage(file);
    if (file.getSize() > MAX_AVATAR_SIZE) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "头像文件不能超过5MB");
    }
    String ext = getExtension(file.getOriginalFilename());
    String key = "avatars/" + userId + "/" + UUID.randomUUID() + "." + ext;
    return store(file, key);
  }

  public String uploadPostImage(Long userId, MultipartFile file) {
    validateImage(file);
    if (file.getSize() > MAX_CREATION_SIZE) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "图片文件不能超过50MB");
    }
    String ext = getExtension(file.getOriginalFilename());
    String key = "posts/" + userId + "/" + UUID.randomUUID() + "." + ext;
    return store(file, key);
  }

  public String uploadCreation(Long userId, MultipartFile file) {
    if (file.getSize() > MAX_CREATION_SIZE) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能超过50MB");
    }
    String contentType = file.getContentType();
    if (contentType == null) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "无法识别文件类型");
    }
    String ext = getExtension(file.getOriginalFilename());
    String key = "creations/" + userId + "/" + UUID.randomUUID() + "." + ext;
    return store(file, key);
  }

  private String store(MultipartFile file, String key) {
    if (cosEnabled && cosClient != null) {
      return storeToCos(file, key);
    }
    return storeToLocal(file, key);
  }

  private String storeToLocal(MultipartFile file, String key) {
    try {
      Path targetPath = Paths.get(localUploadPath, key).toAbsolutePath();
      Files.createDirectories(targetPath.getParent());
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
      return localBaseUrl + "/" + key;
    } catch (IOException e) {
      log.error("本地文件存储失败: key={}", key, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件上传失败");
    }
  }

  private String storeToCos(MultipartFile file, String key) {
    try {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(file.getSize());
      if (file.getContentType() != null) {
        metadata.setContentType(file.getContentType());
      }
      cosClient.putObject(cosBucket, key, file.getInputStream(), metadata);
      String url = String.format("https://%s.cos.%s.myqcloud.com/%s", cosBucket, cosRegion, key);
      log.debug("COS 上传成功: {}", url);
      return url;
    } catch (Exception e) {
      log.error("COS 上传失败: key={}", key, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件上传失败，请稍后重试");
    }
  }

  private void validateImage(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
      throw new BusinessException(ErrorCode.PARAM_ERROR,
          "仅支持 JPEG/PNG/GIF/WebP/SVG 格式图片");
    }
  }

  private String getExtension(String filename) {
    if (filename == null || !filename.contains(".")) return "bin";
    return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
  }
}
