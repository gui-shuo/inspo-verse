package com.inspoverse.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.WorkshopProject;
import com.inspoverse.api.service.FileStorageService;
import com.inspoverse.api.service.UserService;
import com.inspoverse.api.service.WorkshopService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 创意工坊控制器
 */
@RestController
@RequestMapping("/api/v1/workshop")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopService workshopService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    /**
     * 获取工坊项目列表（公开，支持分页/筛选/排序）
     */
    @GetMapping("/projects")
    public ApiResponse<Map<String, Object>> listProjects(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "hot") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        IPage<WorkshopProject> result = workshopService.listProjects(category, keyword, sortBy, page, pageSize);
        List<Map<String, Object>> items = result.getRecords().stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(Map.of(
                "items", items,
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "pageSize", result.getSize()
        ));
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/projects/{id}")
    public ApiResponse<Map<String, Object>> getProjectDetail(@PathVariable Long id) {
        WorkshopProject project = workshopService.getProject(id);
        return ApiResponse.success(toDetailResponse(project));
    }

    /**
     * 获取我的工坊项目
     */
    @GetMapping("/my-projects")
    public ApiResponse<Map<String, Object>> getMyProjects(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        Long userId = (Long) request.getAttribute("userId");
        IPage<WorkshopProject> result = workshopService.getMyProjects(userId, page, pageSize);
        List<Map<String, Object>> items = result.getRecords().stream()
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(Map.of(
                "items", items,
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "pageSize", result.getSize()
        ));
    }

    /**
     * 创建工坊项目
     */
    @PostMapping("/projects")
    public ApiResponse<Map<String, Object>> createProject(
            HttpServletRequest request,
            @Valid @RequestBody CreateProjectRequest req
    ) {
        Long userId = (Long) request.getAttribute("userId");
        WorkshopProject project = workshopService.createProject(
                userId, req.title(), req.description(), req.coverUrl(),
                req.tags(), req.version(), req.fileUrl(), req.fileSize(), req.category());
        return ApiResponse.success(toDetailResponse(project));
    }

    /**
     * 更新工坊项目
     */
    @PutMapping("/projects/{id}")
    public ApiResponse<Map<String, Object>> updateProject(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest req
    ) {
        Long userId = (Long) request.getAttribute("userId");
        WorkshopProject project = workshopService.updateProject(
                userId, id, req.title(), req.description(), req.coverUrl(),
                req.tags(), req.version(), req.fileUrl(), req.fileSize(), req.category());
        return ApiResponse.success(toDetailResponse(project));
    }

    /**
     * 删除工坊项目
     */
    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = (Long) request.getAttribute("userId");
        workshopService.deleteProject(userId, id);
        return ApiResponse.success(null);
    }

    /**
     * 订阅/取消订阅
     */
    @PostMapping("/projects/{id}/subscribe")
    public ApiResponse<Map<String, Object>> toggleSubscription(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = (Long) request.getAttribute("userId");
        boolean subscribed = workshopService.toggleSubscription(userId, id);
        return ApiResponse.success(Map.of("subscribed", subscribed));
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/projects/{id}/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = (Long) request.getAttribute("userId");
        boolean liked = workshopService.toggleLike(userId, id);
        return ApiResponse.success(Map.of("liked", liked));
    }

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/projects/{id}/favorite")
    public ApiResponse<Map<String, Object>> toggleFavorite(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = (Long) request.getAttribute("userId");
        boolean favorited = workshopService.toggleFavorite(userId, id);
        return ApiResponse.success(Map.of("favorited", favorited));
    }

    /**
     * 评分
     */
    @PostMapping("/projects/{id}/rate")
    public ApiResponse<Map<String, Object>> rateProject(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody RateRequest req
    ) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = workshopService.rateProject(userId, id, req.score());
        return ApiResponse.success(result);
    }

    /**
     * 获取用户对项目的交互状态（点赞/收藏/订阅/评分）
     */
    @GetMapping("/projects/{id}/interaction")
    public ApiResponse<Map<String, Object>> getInteraction(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(workshopService.getInteractionStatus(userId, id));
    }

    /**
     * 上传项目封面图
     */
    @PostMapping("/upload-cover")
    public ApiResponse<Map<String, String>> uploadCover(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = (Long) request.getAttribute("userId");
        String url = workshopService.uploadCover(userId, file, fileStorageService);
        return ApiResponse.success(Map.of("url", url));
    }

    // ========================= Request DTOs =========================

    record CreateProjectRequest(
            @NotBlank String title,
            String description,
            String coverUrl,
            String tags,
            String version,
            String fileUrl,
            String fileSize,
            String category
    ) {}

    record UpdateProjectRequest(
            String title,
            String description,
            String coverUrl,
            String tags,
            String version,
            String fileUrl,
            String fileSize,
            String category
    ) {}

    record RateRequest(
            @Min(1) @Max(10) int score
    ) {}

    // ========================= Response Helpers =========================

    private Map<String, Object> toListResponse(WorkshopProject p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("projectNo", p.getProjectNo());
        map.put("title", p.getTitle());
        map.put("coverUrl", p.getCoverUrl() != null ? p.getCoverUrl() : "");
        map.put("category", p.getCategory());
        map.put("tags", p.getTags());
        map.put("downloadCount", p.getDownloadCount());
        map.put("likeCount", p.getLikeCount());
        map.put("favoriteCount", p.getFavoriteCount());
        map.put("rating", computeRating(p));
        map.put("ratingCount", p.getRatingCount());
        map.put("createdAt", p.getCreatedAt().toString());

        // 获取作者信息
        enrichAuthor(map, p.getUserId());

        return map;
    }

    private Map<String, Object> toDetailResponse(WorkshopProject p) {
        Map<String, Object> map = toListResponse(p);
        map.put("description", p.getDescription() != null ? p.getDescription() : "");
        map.put("version", p.getVersion() != null ? p.getVersion() : "v1.0.0");
        map.put("fileUrl", p.getFileUrl() != null ? p.getFileUrl() : "");
        map.put("fileSize", p.getFileSize() != null ? p.getFileSize() : "");
        map.put("visibility", p.getVisibility());
        map.put("status", p.getStatus());
        map.put("updatedAt", p.getUpdatedAt().toString());
        return map;
    }

    private void enrichAuthor(Map<String, Object> map, Long userId) {
        try {
            var user = userService.getUserById(userId);
            map.put("author", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "nickname", user.getNickname(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            ));
        } catch (Exception e) {
            map.put("author", Map.of(
                    "id", userId,
                    "username", "unknown",
                    "nickname", "未知用户",
                    "avatarUrl", ""
            ));
        }
    }

    private double computeRating(WorkshopProject p) {
        if (p.getRatingCount() == null || p.getRatingCount() == 0) return 0;
        return Math.round((double) p.getRatingSum() / p.getRatingCount() * 10.0) / 10.0;
    }
}
