package com.pmrodrigues.commons.config;

import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration that registers the {@link RequestIdInterceptor} for all non-auth, non-actuator paths.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RequestIdInterceptor requestIdInterceptor;

    /**
     * Registers the request-ID interceptor on all paths except {@code /auth/**} and {@code /actuator/**}.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestIdInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/actuator/**");
    }
}