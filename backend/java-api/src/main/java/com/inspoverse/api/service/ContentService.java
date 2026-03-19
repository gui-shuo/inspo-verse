package com.inspoverse.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspoverse.api.entity.ContentItem;
import com.inspoverse.api.mapper.ContentItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容服务
 */
@Service
@RequiredArgsConstructor
public class ContentService {
  private final ContentItemMapper contentItemMapper;

  /**
   * 获取内容列表（按分类）
   */
  public List<ContentItem> getContentByCategory(String category) {
    LambdaQueryWrapper<ContentItem> wrapper = new LambdaQueryWrapper<ContentItem>()
        .eq(ContentItem::getStatus, 1)
        .eq(ContentItem::getIsDeleted, 0)
        .orderByDesc(ContentItem::getCreatedAt);

    if (category != null && !"all".equals(category)) {
      wrapper.eq(ContentItem::getCategory, category);
    }

    return contentItemMapper.selectList(wrapper);
  }

  /**
   * 获取内容详情
   */
  public ContentItem getContentById(Long id) {
    return contentItemMapper.selectById(id);
  }
}
