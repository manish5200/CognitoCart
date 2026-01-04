package com.manish.smartcart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        // Expose the 'uploads' folder as a public URL path
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("file:uploads/products/");
    }
}
