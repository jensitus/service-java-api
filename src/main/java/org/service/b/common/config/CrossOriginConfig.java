package org.service.b.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CrossOriginConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://new.service-b.org", "https://www.service-b.org", "https://service-b.org", "http://localhost:4200",
                        "http://localhost:8080")
                .allowedMethods("*")
                .allowedHeaders("*")
                .maxAge(3600);
    }

}
