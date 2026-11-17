package com.rentalhub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get(uploadDir)
            .toAbsolutePath().normalize().toString();

        registry
            .addResourceHandler("/files/**")
            .addResourceLocations("file:" + absoluteUploadPath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
            .addInterceptor(rateLimitInterceptor)
            /*
             * Only apply rate limiting to authentication endpoints.
             * These are the ones most vulnerable to brute force.
             * /auth/refresh is included because it could be used to
             * probe for valid tokens.
             */
            .addPathPatterns(
                "/auth/login",
                "/auth/register",
                "/auth/refresh"
            );
    }
}