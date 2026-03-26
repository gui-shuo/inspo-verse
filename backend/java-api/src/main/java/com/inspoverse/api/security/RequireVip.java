package com.inspoverse.api.security;

import java.lang.annotation.*;

/**
 * 标注在 Controller 方法上，要求用户具备指定 VIP 等级才能访问。
 * <p>
 * 等级对照：
 * <ul>
 *   <li>0 — 普通用户（免费）</li>
 *   <li>1 — 白银会员</li>
 *   <li>2 — 黄金会员</li>
 * </ul>
 *
 * 示例：
 * <pre>
 * &#64;RequireVip(level = 1)
 * &#64;PostMapping("/some-silver-feature")
 * public ApiResponse&lt;Void&gt; silverFeature(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireVip {
  /** 最低 VIP 等级（默认 1 = 白银会员） */
  int level() default 1;

  /** 权限不足时的自定义提示 */
  String message() default "";
}
