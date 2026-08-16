package com.manish.smartcart.config;

import com.manish.smartcart.shared.converter.FlexibleDateConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC customizations.
 * - addResourceHandlers: exposes the local uploads/ folder as a public URL.
 * - addFormatters: registers FlexibleDateConverter so ALL LocalDate @RequestParams
 *   in the entire API accept both yyyy-MM-dd and yyyyMMdd — zero annotation noise.
 */

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FlexibleDateConverter flexibleDateConverter;

    @Value("${app.upload-dir:uploads/products/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose the uploads folder as a public URL: /resources/**
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("file:" + uploadDir);
    }

    public void addFormatters(FormatterRegistry registry) {
        // One registration → multi-format LocalDate support across the entire API
        registry.addConverter(flexibleDateConverter);
    }

}
