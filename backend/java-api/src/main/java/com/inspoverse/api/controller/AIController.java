package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.AIChatMessage;
import com.inspoverse.api.entity.AIChatSession;
import com.inspoverse.api.security.RequireVip;
import com.inspoverse.api.service.AIService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 控制器 — 会话管理 / 流式对话 / 语音识别 / 文件解析
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {
  private final AIService aiService;

  /**
   * 创建会话
   */
  @PostMapping("/sessions")
  public ApiResponse<Map<String, Object>> createSession(
      HttpServletRequest request,
      @Valid @RequestBody CreateSessionRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    AIChatSession session = aiService.createSession(userId, req.scene(), req.modelName());

    return ApiResponse.success(Map.of(
        "sessionId", session.getId(),
        "sessionNo", session.getSessionNo(),
        "title", session.getTitle(),
        "modelName", session.getModelName()
    ));
  }

  /**
   * 获取会话列表
   */
  @GetMapping("/sessions")
  public ApiResponse<List<Map<String, Object>>> getSessions(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<AIChatSession> sessions = aiService.getUserSessions(userId);

    List<Map<String, Object>> result = sessions.stream().map(session -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", session.getId());
      map.put("sessionNo", session.getSessionNo());
      map.put("title", session.getTitle());
      map.put("modelName", session.getModelName());
      map.put("scene", session.getScene());
      map.put("tokenUsed", session.getTokenUsed());
      map.put("createdAt", session.getCreatedAt().toString());
      map.put("updatedAt", session.getUpdatedAt().toString());
      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(result);
  }

  /**
   * 获取会话消息历史
   */
  @GetMapping("/sessions/{sessionId}/messages")
  public ApiResponse<List<Map<String, Object>>> getMessages(
      HttpServletRequest request,
      @PathVariable Long sessionId
  ) {
    List<AIChatMessage> messages = aiService.getSessionMessages(sessionId);

    List<Map<String, Object>> result = messages.stream().map(msg -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", msg.getId());
      map.put("role", msg.getRole());
      map.put("content", msg.getContent());
      map.put("tokens", msg.getTokens());
      map.put("latencyMs", msg.getLatencyMs());
      map.put("createdAt", msg.getCreatedAt().toString());
      return map;
    }).collect(Collectors.toList());

    return ApiResponse.success(result);
  }

  /**
   * 发送消息 — SSE 流式回复（主要接口）
   */
  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter sendMessageStream(
      HttpServletRequest request,
      @Valid @RequestBody ChatRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    return aiService.sendMessageStream(userId, req.sessionId(), req.content());
  }

  /**
   * 发送消息 — 非流式（兼容旧接口）
   */
  @PostMapping("/chat")
  public ApiResponse<Map<String, Object>> sendMessage(
      HttpServletRequest request,
      @Valid @RequestBody ChatRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    AIChatMessage aiMsg = aiService.sendMessage(userId, req.sessionId(), req.content());

    return ApiResponse.success(Map.of(
        "id", aiMsg.getId(),
        "role", aiMsg.getRole(),
        "content", aiMsg.getContent(),
        "tokens", aiMsg.getTokens(),
        "latencyMs", aiMsg.getLatencyMs() != null ? aiMsg.getLatencyMs() : 0,
        "createdAt", aiMsg.getCreatedAt().toString()
    ));
  }

  /**
   * 上传文件并提取内容（黄金会员多模态功能）
   */
  @RequireVip(level = 2, message = "文件解析为黄金会员专属功能，请升级后使用")
  @PostMapping("/upload-file")
  public ApiResponse<Map<String, Object>> uploadFile(
      HttpServletRequest request,
      @RequestParam("file") MultipartFile file
  ) {
    if (file.isEmpty()) {
      return ApiResponse.failure(40001, "文件不能为空");
    }
    if (file.getSize() > 10 * 1024 * 1024) {
      return ApiResponse.failure(40001, "文件大小不能超过10MB");
    }

    try {
      Map<String, Object> result = aiService.extractFileContent(
          file.getBytes(),
          file.getOriginalFilename()
      );
      return ApiResponse.success(result);
    } catch (Exception e) {
      return ApiResponse.failure(50000, "文件解析失败: " + e.getMessage());
    }
  }

  /**
   * 语音转文字（黄金会员多模态功能）
   */
  @RequireVip(level = 2, message = "语音识别为黄金会员专属功能，请升级后使用")
  @PostMapping("/voice-to-text")
  public ApiResponse<Map<String, Object>> voiceToText(
      HttpServletRequest request,
      @RequestParam("file") MultipartFile file
  ) {
    if (file.isEmpty()) {
      return ApiResponse.failure(40001, "音频文件不能为空");
    }
    if (file.getSize() > 25 * 1024 * 1024) {
      return ApiResponse.failure(40001, "音频大小不能超过25MB");
    }

    try {
      Map<String, Object> result = aiService.transcribeVoice(
          file.getBytes(),
          file.getOriginalFilename()
      );
      return ApiResponse.success(result);
    } catch (Exception e) {
      return ApiResponse.failure(50000, "语音识别失败: " + e.getMessage());
    }
  }

  /**
   * 更新会话模型
   */
  @PatchMapping("/sessions/{sessionId}/model")
  public ApiResponse<Void> updateSessionModel(
      HttpServletRequest request,
      @PathVariable Long sessionId,
      @Valid @RequestBody UpdateModelRequest req
  ) {
    Long userId = (Long) request.getAttribute("userId");
    aiService.updateSessionModel(userId, sessionId, req.modelName());
    return ApiResponse.success(null);
  }

  /**
   * 删除会话
   */
  @DeleteMapping("/sessions/{sessionId}")
  public ApiResponse<Void> deleteSession(
      HttpServletRequest request,
      @PathVariable Long sessionId
  ) {
    Long userId = (Long) request.getAttribute("userId");
    aiService.deleteSession(userId, sessionId);
    return ApiResponse.success(null);
  }

  // ── Request DTOs ──────────────────────────────────────────────

  public record CreateSessionRequest(
      String scene,
      String modelName
  ) {}

  public record ChatRequest(
      @NotNull(message = "会话ID不能为空") Long sessionId,
      @NotBlank(message = "消息内容不能为空") String content
  ) {}

  public record UpdateModelRequest(
      @NotBlank(message = "模型名称不能为空") String modelName
  ) {}
}
