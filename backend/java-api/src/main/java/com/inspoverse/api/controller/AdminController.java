package com.inspoverse.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import com.inspoverse.api.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器 - 仅管理员可访问
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    private Long getAdminUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        adminService.requireAdmin(userId);
        return userId;
    }

    // ========================= 仪表盘 =========================

    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Object>> getDashboardStats(HttpServletRequest request) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getDashboardStats());
    }

    @GetMapping("/dashboard/user-growth")
    public ApiResponse<List<Map<String, Object>>> getUserGrowthTrend(
            HttpServletRequest request,
            @RequestParam(defaultValue = "7") int days) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getUserGrowthTrend(days));
    }

    @GetMapping("/dashboard/revenue-composition")
    public ApiResponse<List<Map<String, Object>>> getRevenueComposition(HttpServletRequest request) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getRevenueComposition());
    }

    // ========================= 用户管理 =========================

    @GetMapping("/users")
    public ApiResponse<Page<Map<String, Object>>> getUserList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getUserList(page, size, keyword, status, role));
    }

    @PutMapping("/users/{userId}/toggle-status")
    public ApiResponse<Void> toggleUserStatus(HttpServletRequest request, @PathVariable Long userId) {
        Long adminId = getAdminUserId(request);
        adminService.toggleUserStatus(adminId, userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/role")
    public ApiResponse<Void> updateUserRole(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        Long adminId = getAdminUserId(request);
        String roleCode = body.get("roleCode");
        if (roleCode == null || roleCode.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色编码不能为空");
        }
        adminService.updateUserRole(adminId, userId, roleCode);
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/reset-password")
    public ApiResponse<Void> resetUserPassword(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        Long adminId = getAdminUserId(request);
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能少于8位");
        }
        adminService.resetUserPassword(adminId, userId, newPassword);
        return ApiResponse.success(null);
    }

    // ========================= 帖子管理 =========================

    @GetMapping("/posts")
    public ApiResponse<Page<Map<String, Object>>> getPostList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getPostList(page, size, keyword, category, status));
    }

    @PutMapping("/posts/{postId}/toggle-top")
    public ApiResponse<Void> togglePostTop(HttpServletRequest request, @PathVariable Long postId) {
        Long adminId = getAdminUserId(request);
        adminService.togglePostTop(adminId, postId);
        return ApiResponse.success(null);
    }

    @PutMapping("/posts/{postId}/toggle-essence")
    public ApiResponse<Void> togglePostEssence(HttpServletRequest request, @PathVariable Long postId) {
        Long adminId = getAdminUserId(request);
        adminService.togglePostEssence(adminId, postId);
        return ApiResponse.success(null);
    }

    @PutMapping("/posts/{postId}/status")
    public ApiResponse<Void> updatePostStatus(
            HttpServletRequest request,
            @PathVariable Long postId,
            @RequestBody Map<String, Integer> body) {
        Long adminId = getAdminUserId(request);
        adminService.updatePostStatus(adminId, postId, body.getOrDefault("status", 1));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(HttpServletRequest request, @PathVariable Long postId) {
        Long adminId = getAdminUserId(request);
        adminService.deletePost(adminId, postId);
        return ApiResponse.success(null);
    }

    // ========================= 订单管理 =========================

    @GetMapping("/orders")
    public ApiResponse<Page<Map<String, Object>>> getOrderList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bizType) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getOrderList(page, size, keyword, status, bizType));
    }

    // ========================= AI 监控 =========================

    @GetMapping("/ai/stats")
    public ApiResponse<Map<String, Object>> getAiStats(HttpServletRequest request) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getAiStats());
    }

    @GetMapping("/ai/messages")
    public ApiResponse<Page<Map<String, Object>>> getAiMessageLogs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String safetyFilter) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getAiMessageLogs(page, size, safetyFilter));
    }

    // ========================= 内容管理（发现） =========================

    @GetMapping("/content")
    public ApiResponse<Page<Map<String, Object>>> getContentList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getContentList(page, size, keyword, category, status));
    }

    @PutMapping("/content/{contentId}/status")
    public ApiResponse<Void> updateContentStatus(
            HttpServletRequest request,
            @PathVariable Long contentId,
            @RequestBody Map<String, Integer> body) {
        Long adminId = getAdminUserId(request);
        adminService.updateContentStatus(adminId, contentId, body.getOrDefault("status", 1));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/content/{contentId}")
    public ApiResponse<Void> deleteContent(HttpServletRequest request, @PathVariable Long contentId) {
        Long adminId = getAdminUserId(request);
        adminService.deleteContent(adminId, contentId);
        return ApiResponse.success(null);
    }

    // ========================= 番剧管理 =========================

    @GetMapping("/anime")
    public ApiResponse<Page<Map<String, Object>>> getAnimeList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getAnimeList(page, size, keyword, status));
    }

    @PutMapping("/anime/{animeId}/status")
    public ApiResponse<Void> updateAnimeStatus(
            HttpServletRequest request,
            @PathVariable Long animeId,
            @RequestBody Map<String, String> body) {
        Long adminId = getAdminUserId(request);
        adminService.updateAnimeStatus(adminId, animeId, body.getOrDefault("status", "ONGOING"));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/anime/{animeId}")
    public ApiResponse<Void> deleteAnime(HttpServletRequest request, @PathVariable Long animeId) {
        Long adminId = getAdminUserId(request);
        adminService.deleteAnime(adminId, animeId);
        return ApiResponse.success(null);
    }

    // ========================= 游戏管理 =========================

    @GetMapping("/games")
    public ApiResponse<Page<Map<String, Object>>> getGameList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String genre) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getGameList(page, size, keyword, genre));
    }

    @PutMapping("/games/{gameId}/status")
    public ApiResponse<Void> updateGameStatus(
            HttpServletRequest request,
            @PathVariable Long gameId,
            @RequestBody Map<String, Integer> body) {
        Long adminId = getAdminUserId(request);
        adminService.updateGameStatus(adminId, gameId, body.getOrDefault("status", 1));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/games/{gameId}")
    public ApiResponse<Void> deleteGame(HttpServletRequest request, @PathVariable Long gameId) {
        Long adminId = getAdminUserId(request);
        adminService.deleteGame(adminId, gameId);
        return ApiResponse.success(null);
    }

    // ========================= 工坊管理 =========================

    @GetMapping("/workshop")
    public ApiResponse<Page<Map<String, Object>>> getWorkshopList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getWorkshopList(page, size, keyword, category));
    }

    @PutMapping("/workshop/{projectId}/status")
    public ApiResponse<Void> updateWorkshopStatus(
            HttpServletRequest request,
            @PathVariable Long projectId,
            @RequestBody Map<String, Integer> body) {
        Long adminId = getAdminUserId(request);
        adminService.updateWorkshopStatus(adminId, projectId, body.getOrDefault("status", 1));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/workshop/{projectId}")
    public ApiResponse<Void> deleteWorkshopProject(HttpServletRequest request, @PathVariable Long projectId) {
        Long adminId = getAdminUserId(request);
        adminService.deleteWorkshopProject(adminId, projectId);
        return ApiResponse.success(null);
    }

    // ========================= 系统配置 =========================

    @GetMapping("/settings")
    public ApiResponse<List<Map<String, Object>>> getAllConfigs(HttpServletRequest request) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getAllConfigs());
    }

    @PutMapping("/settings")
    public ApiResponse<Void> batchUpdateConfigs(
            HttpServletRequest request,
            @RequestBody Map<String, String> configs) {
        getAdminUserId(request);
        adminService.batchUpdateConfigs(configs);
        return ApiResponse.success(null);
    }

    @PutMapping("/settings/{key}")
    public ApiResponse<Void> updateConfig(
            HttpServletRequest request,
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        getAdminUserId(request);
        adminService.upsertConfig(key, body.get("value"), body.get("valueType"), body.get("remark"));
        return ApiResponse.success(null);
    }

    // ========================= VIP管理 =========================

    @GetMapping("/vip/members")
    public ApiResponse<Page<Map<String, Object>>> getVipMemberList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getVipMemberList(page, size));
    }

    @GetMapping("/vip/plans")
    public ApiResponse<List<Map<String, Object>>> getVipPlans(HttpServletRequest request) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getVipPlans());
    }

    @PutMapping("/vip/plans/{planId}")
    public ApiResponse<Void> updateVipPlan(
            HttpServletRequest request,
            @PathVariable Long planId,
            @RequestBody Map<String, Object> body) {
        getAdminUserId(request);
        adminService.updateVipPlan(planId,
                (String) body.get("planName"),
                body.get("priceCents") != null ? ((Number) body.get("priceCents")).intValue() : null,
                body.get("durationDays") != null ? ((Number) body.get("durationDays")).intValue() : null,
                body.get("status") != null ? ((Number) body.get("status")).intValue() : null);
        return ApiResponse.success(null);
    }

    // ========================= 每日任务管理 =========================

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> getDailyTasks(HttpServletRequest request) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getDailyTasks());
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse<Void> updateDailyTask(
            HttpServletRequest request,
            @PathVariable Long taskId,
            @RequestBody Map<String, Object> body) {
        getAdminUserId(request);
        adminService.updateDailyTask(taskId,
                (String) body.get("taskName"),
                (String) body.get("description"),
                body.get("rewardPoints") != null ? ((Number) body.get("rewardPoints")).intValue() : null,
                body.get("rewardExp") != null ? ((Number) body.get("rewardExp")).intValue() : null,
                body.get("dailyLimit") != null ? ((Number) body.get("dailyLimit")).intValue() : null,
                body.get("status") != null ? ((Number) body.get("status")).intValue() : null);
        return ApiResponse.success(null);
    }

    // ========================= 邮件订阅管理 =========================

    @GetMapping("/email-subscriptions")
    public ApiResponse<Page<Map<String, Object>>> getEmailSubscriptions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        getAdminUserId(request);
        return ApiResponse.success(adminService.getEmailSubscriptions(page, size));
    }

    // ========================= 权限验证 =========================

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> checkAdminAccess(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean isAdmin = adminService.isAdmin(userId);
        List<String> roles = adminService.getUserRoles(userId);
        return ApiResponse.success(Map.of("isAdmin", isAdmin, "roles", roles));
    }
}
