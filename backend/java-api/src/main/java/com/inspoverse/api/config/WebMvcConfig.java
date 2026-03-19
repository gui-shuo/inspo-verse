package com.inspoverse.api.config;

import com.inspoverse.api.security.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private final JwtAuthInterceptor jwtAuthInterceptor;

  @Value("${local.upload.path:/tmp/inspo-uploads}")
  private String uploadPath;

  public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor) {
    this.jwtAuthInterceptor = jwtAuthInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(jwtAuthInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/api/v1/forum/posts",
            "/api/v1/forum/posts/*",
            "/api/v1/forum/comments",
            "/api/v1/forum/stats",
            "/actuator/**"
        );
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 本地上传文件静态资源映射（开发环境使用）
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + uploadPath + "/");
  }
}
