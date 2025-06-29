package com.wewe.temjaimusic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // เปลี่ยน path ให้ตรงกับที่คุณอัปโหลดไว้จริง
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/c:/temjaimusic/uploads/");
    }
}
