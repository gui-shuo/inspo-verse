package com.inspoverse.api.common;

/**
 * 统一错误码
 */
public enum ErrorCode {
  SUCCESS(0, "success"),
  PARAM_ERROR(40001, "参数错误"),
  UNAUTHORIZED(40100, "未登录或Token无效"),
  FORBIDDEN(40300, "无权限"),
  NOT_FOUND(40400, "资源不存在"),
  CONFLICT(40900, "业务冲突"),
  TOO_MANY_REQUESTS(42900, "请求过多"),
  INTERNAL_ERROR(50000, "系统内部错误"),
  SERVICE_UNAVAILABLE(50300, "下游服务不可用");

  private final int code;
  private final String message;

  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
