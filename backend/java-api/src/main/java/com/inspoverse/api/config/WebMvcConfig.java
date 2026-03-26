package com.inspoverse.api.config;

import com.inspoverse.api.security.JwtAuthInterceptor;
import com.inspoverse.api.security.VipAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private final JwtAuthInterceptor jwtAuthInterceptor;
  private final VipAuthInterceptor vipAuthInterceptor;

  @Value("${local.upload.path:/tmp/inspo-uploads}")
  private String uploadPath;

  public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor, VipAuthInterceptor vipAuthInterceptor) {
    this.jwtAuthInterceptor = jwtAuthInterceptor;
    this.vipAuthInterceptor = vipAuthInterceptor;
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
            "/api/v1/forum/stats",           // 社区统计无需登录
            "/api/v1/payment/packages",      // 套餐列表公开
            "/api/v1/payment/notify/**",     // 支付回调由支付平台调用，无 JWT
            "/api/v1/anime/schedule",        // 番剧放映表公开
            "/api/v1/anime/hot",             // 热门番剧公开
            "/api/v1/anime/notify/**",       // 番剧支付回调
            "/api/v1/games/notify/**",       // 游戏购买支付回调
            "/actuator/**"
            // 论坛帖子/评论的 GET 读取在拦截器内处理为可选认证，
            // 避免把整个路径从POST写操作也一起排除进而（userId=null）导致数据库报错
        );

    // VIP 权限拦截器（在 JWT 之后执行，检查 @RequireVip 注解）
    registry.addInterceptor(vipAuthInterceptor)
        .addPathPatterns("/api/**");
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 本地上传文件静态资源映射（开发环境使用）
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + uploadPath + "/");
  }
}
