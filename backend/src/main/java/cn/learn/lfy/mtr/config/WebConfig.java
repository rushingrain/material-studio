package cn.learn.lfy.mtr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String allowedOrigin;
    private final String uploadLocation;

    public WebConfig(
            @Value("${app.cors.allowed-origin}") String allowedOrigin,
            @Value("${app.upload-dir}") String uploadDir
    ) {
        this.allowedOrigin = allowedOrigin;
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        this.uploadLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation)
                .setCachePeriod(3600);
    }
}
