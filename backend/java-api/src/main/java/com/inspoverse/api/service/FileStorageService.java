package com.inspoverse.api.service;

import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import jakarta.annotation.PostConstruct;
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
 * 支持本地存储（开发模式）与阿里云OSS（生产模式）双模式
 * 通过 aliyun.oss.enabled 开关切换
 */
@Slf4j
@Service
public class FileStorageService {

  private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
      "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
  );
  private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024L;    // 5MB
  private static final long MAX_CREATION_SIZE = 50 * 1024 * 1024L; // 50MB

  @Value("${aliyun.oss.enabled:false}")
  private boolean ossEnabled;

  @Value("${local.upload.path:/tmp/inspo-uploads}")
  private String localUploadPath;

  @Value("${local.base-url:http://localhost:8080/uploads}")
  private String localBaseUrl;

  /**
   * 启动后将 localUploadPath 转换为当前平台的绝对路径并创建目录
   * 解决 Windows 下 /tmp/... 无盘符路径被 Tomcat 解析为相对路径的问题
   */
  @PostConstruct
  public void init() throws IOException {
    // 如果是 Linux/Mac 的绝对路径，在 Windows 上将其转换为用户临时目录下的同名子目录
    Path resolved = Paths.get(localUploadPath).toAbsolutePath();
    Files.createDirectories(resolved);
    localUploadPath = resolved.toString();
    log.info("本地文件存储目录: {}", localUploadPath);
  }

  // OSS 配置（生产注入）
  @Value("${aliyun.oss.endpoint:}")
  private String ossEndpoint;
  @Value("${aliyun.oss.access-key-id:}")
  private String ossAccessKeyId;
  @Value("${aliyun.oss.access-key-secret:}")
  private String ossAccessKeySecret;
  @Value("${aliyun.oss.bucket-name:inspo-verse}")
  private String ossBucketName;
  @Value("${aliyun.oss.domain:}")
  private String ossDomain;

  /**
   * 上传头像
   */
  public String uploadAvatar(Long userId, MultipartFile file) {
    validateImage(file);
    if (file.getSize() > MAX_AVATAR_SIZE) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "头像文件不能超过5MB");
    }
    String ext = getExtension(file.getOriginalFilename());
    String key = "avatars/" + userId + "/" + UUID.randomUUID() + "." + ext;
    return store(file, key);
  }

  /**
   * 上传帖子图片
   */
  public String uploadPostImage(Long userId, MultipartFile file) {
    validateImage(file);
    if (file.getSize() > MAX_CREATION_SIZE) {
      throw new BusinessException(ErrorCode.PARAM_ERROR, "图片文件不能超过50MB");
    }
    String ext = getExtension(file.getOriginalFilename());
    String key = "posts/" + userId + "/" + UUID.randomUUID() + "." + ext;
    return store(file, key);
  }

  /**
   * 上传创作文件
   */
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
    if (ossEnabled) {
      return storeToOss(file, key);
    }
    return storeToLocal(file, key);
  }

  private String storeToLocal(MultipartFile file, String key) {
    try {
      Path targetPath = Paths.get(localUploadPath, key).toAbsolutePath();
      Files.createDirectories(targetPath.getParent());
      // 使用 Files.copy 而非 transferTo，避免 Tomcat Part.write() 在 Windows 上
      // 将无盘符路径（如 /tmp/...）解析为相对 work-dir 的路径导致 FileNotFoundException
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
      return localBaseUrl + "/" + key;
    } catch (IOException e) {
      log.error("本地文件存储失败: key={}", key, e);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件上传失败");
    }
  }

  private String storeToOss(MultipartFile file, String key) {
    // 阿里云 OSS 上传（生产环境）
    // 需在 pom.xml 中添加: com.aliyun.oss:aliyun-sdk-oss:3.17.4
    try {
      // OSS SDK 动态调用，避免编译期依赖
      Class<?> ossClientClass = Class.forName("com.aliyun.oss.OSSClientBuilder");
      Object ossClient = ossClientClass.getMethod("build", String.class, String.class, String.class)
          .invoke(ossClientClass.getDeclaredConstructor().newInstance(),
              ossEndpoint, ossAccessKeyId, ossAccessKeySecret);

      ossClientClass.getSuperclass().getMethod("putObject",
          String.class, String.class, java.io.InputStream.class)
          .invoke(ossClient, ossBucketName, key, file.getInputStream());

      ossClientClass.getSuperclass().getMethod("shutdown").invoke(ossClient);

      String domain = (ossDomain != null && !ossDomain.isEmpty())
          ? ossDomain
          : "https://" + ossBucketName + "." + ossEndpoint;
      return domain + "/" + key;
    } catch (Exception e) {
      log.error("OSS上传失败，降级至本地存储: key={}", key, e);
      return storeToLocal(file, key);
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
