package com.tamabee.api_hr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration
 * Static files được serve bởi FileController
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // Static resource handler đã được thay thế bởi FileController
}
