package cn.learn.lfy.mtr.config;

import cn.learn.lfy.mtr.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String allowedOrigin;
    private final AuthInterceptor authInterceptor;

    public WebConfig(
            @Value("${app.cors.allowed-origin}") String allowedOrigin,
            AuthInterceptor authInterceptor
    ) {
        this.allowedOrigin = allowedOrigin;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/tasks/**", "/uploads/**", "/auth/me");
    }
}
