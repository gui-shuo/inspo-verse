package com.inspoverse.api.config;

import com.inspoverse.api.common.ApiResponse;
import com.inspoverse.api.common.BusinessException;
import com.inspoverse.api.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleBusinessException(BusinessException e) {
    log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
    return ApiResponse.failure(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
    String errors = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));
    log.warn("Validation failed: {}", errors);
    return ApiResponse.failure(ErrorCode.PARAM_ERROR.getCode(), errors);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
    log.warn("Upload size exceeded: {}", e.getMessage());
    return ApiResponse.failure(ErrorCode.PARAM_ERROR.getCode(), "文件大小超出限制，头像请不超过 5MB，创作文件请不超过 50MB");
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> handleException(Exception e) {
    log.error("Unexpected error", e);
    return ApiResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常，请稍后重试");
  }
}
