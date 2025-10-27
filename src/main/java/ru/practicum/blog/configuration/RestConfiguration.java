package ru.practicum.blog.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class RestConfiguration {

    @Bean
    public HttpMessageConverter<Object> objectHttpMessageConverter() {
        return new MappingJackson2HttpMessageConverter();
    }
}
