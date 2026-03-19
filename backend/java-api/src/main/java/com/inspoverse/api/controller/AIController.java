package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.AIChatMessage;
import com.inspoverse.api.entity.AIChatSession;
import com.inspoverse.api.service.AIService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI控制器
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
   * 发送消息
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
        "latencyMs", aiMsg.getLatencyMs(),
        "createdAt", aiMsg.getCreatedAt().toString()
    ));
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

  public record CreateSessionRequest(
      String scene,
      String modelName
  ) {}

  public record ChatRequest(
      @NotNull(message = "会话ID不能为空") Long sessionId,
      @NotBlank(message = "消息内容不能为空") String content
  ) {}
}
