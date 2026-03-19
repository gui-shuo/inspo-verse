package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.UserCreation;
import com.inspoverse.api.service.CreationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户创作控制器
 */
@RestController
@RequestMapping("/api/v1/creations")
@RequiredArgsConstructor
public class CreationController {

  private final CreationService creationService;

  /**
   * 获取我的创作列表
   */
  @GetMapping
  public ApiResponse<List<Map<String, Object>>> getMyCreations(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<UserCreation> creations = creationService.getUserCreations(userId);
    return ApiResponse.success(toResponseList(creations));
  }

  /**
   * 上传新创作
   */
  @PostMapping
  public ApiResponse<Map<String, Object>> uploadCreation(
      HttpServletRequest request,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "visibility", defaultValue = "0") Integer visibility
  ) {
    Long userId = (Long) request.getAttribute("userId");
    UserCreation creation = creationService.create(userId, file, title, description, visibility);
    return ApiResponse.success(toResponse(creation));
  }

  /**
   * 更新创作可见性
   */
  @PatchMapping("/{id}/visibility")
  public ApiResponse<Void> updateVisibility(
      HttpServletRequest request,
      @PathVariable Long id,
      @RequestBody Map<String, Integer> body
  ) {
    Long userId = (Long) request.getAttribute("userId");
    Integer visibility = body.get("visibility");
    if (visibility == null || (visibility != 0 && visibility != 1)) {
      return ApiResponse.failure(400, "visibility值无效，应为0或1");
    }
    creationService.updateVisibility(userId, id, visibility);
    return ApiResponse.success(null);
  }

  /**
   * 删除创作
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteCreation(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    creationService.delete(userId, id);
    return ApiResponse.success(null);
  }

  /**
   * 获取创作下载信息（返回文件URL，由前端fetch后保存）
   */
  @GetMapping("/{id}/download")
  public ApiResponse<Map<String, Object>> download(
      HttpServletRequest request,
      @PathVariable Long id
  ) {
    Long userId = (Long) request.getAttribute("userId");
    UserCreation creation = creationService.getForDownload(userId, id);
    return ApiResponse.success(Map.of(
        "fileUrl", creation.getFileUrl(),
        "fileName", creation.getTitle() != null ? creation.getTitle() : "creation-" + id,
        "fileType", creation.getFileType(),
        "fileSize", creation.getFileSize()
    ));
  }

  // ========================= Helpers =========================

  private Map<String, Object> toResponse(UserCreation c) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", c.getId());
    map.put("title", c.getTitle() != null ? c.getTitle() : "");
    map.put("description", c.getDescription() != null ? c.getDescription() : "");
    map.put("fileUrl", c.getFileUrl());
    map.put("coverUrl", c.getCoverUrl() != null ? c.getCoverUrl() : c.getFileUrl());
    map.put("fileType", c.getFileType());
    map.put("fileSize", c.getFileSize());
    map.put("visibility", c.getVisibility());  // 0=私密 1=公开
    map.put("createdAt", c.getCreatedAt().toString());
    return map;
  }

  private List<Map<String, Object>> toResponseList(List<UserCreation> list) {
    return list.stream().map(this::toResponse).collect(Collectors.toList());
  }
}
