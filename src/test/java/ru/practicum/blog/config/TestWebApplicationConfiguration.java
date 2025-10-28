package ru.practicum.blog.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TestDataSourceConfiguration.class, RestConfiguration.class, MultipartConfiguration.class, WebCorsConfiguration.class})
@ComponentScan(
        basePackages = "ru.practicum.blog",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DataSourceConfiguration.class)
)
public class TestWebApplicationConfiguration {
}
