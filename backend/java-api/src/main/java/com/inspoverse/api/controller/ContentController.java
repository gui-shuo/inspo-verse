package com.inspoverse.api.controller;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.entity.ContentItem;
import com.inspoverse.api.entity.User;
import com.inspoverse.api.mapper.UserMapper;
import com.inspoverse.api.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内容控制器
 */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {
  private final ContentService contentService;
  private final UserMapper userMapper;

  /**
   * 获取探索内容
   */
  @GetMapping("/explore")
  public ApiResponse<List<Map<String, Object>>> getExploreContent(
      @RequestParam(required = false) String category
  ) {
    List<ContentItem> items = contentService.getContentByCategory(category);
    return ApiResponse.success(convertToResponse(items));
  }

  /**
   * 获取动漫内容
   */
  @GetMapping("/anime")
  public ApiResponse<List<Map<String, Object>>> getAnimeContent() {
    List<ContentItem> items = contentService.getContentByCategory("anime");
    return ApiResponse.success(convertToResponse(items));
  }

  /**
   * 获取游戏内容
   */
  @GetMapping("/games")
  public ApiResponse<List<Map<String, Object>>> getGamesContent() {
    List<ContentItem> items = contentService.getContentByCategory("game");
    return ApiResponse.success(convertToResponse(items));
  }

  private List<Map<String, Object>> convertToResponse(List<ContentItem> items) {
    return items.stream().map(item -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", item.getId());
      map.put("title", item.getTitle());
      map.put("description", item.getDescription());
      map.put("image", item.getCoverUrl());
      map.put("category", item.getCategory());
      map.put("tag", item.getTag());
      map.put("likes", item.getLikeCount());
      map.put("comments", item.getCommentCount());
      map.put("isLiked", false); // TODO: 查询用户是否点赞

      // 获取作者信息
      User author = userMapper.selectById(item.getUserId());
      if (author != null) {
        map.put("author", author.getNickname());
        map.put("avatar", author.getAvatarUrl());
      }

      return map;
    }).collect(Collectors.toList());
  }
}
