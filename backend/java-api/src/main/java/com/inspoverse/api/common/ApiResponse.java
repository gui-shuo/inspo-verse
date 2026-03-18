package com.inspoverse.api.common;

import java.time.Instant;
import java.util.UUID;

public record ApiResponse<T>(
    int code,
    String message,
    T data,
    String traceId,
    long timestamp
) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(0, "success", data, UUID.randomUUID().toString(), Instant.now().toEpochMilli());
  }

  public static <T> ApiResponse<T> failure(int code, String message) {
    return new ApiResponse<>(code, message, null, UUID.randomUUID().toString(), Instant.now().toEpochMilli());
  }
}
