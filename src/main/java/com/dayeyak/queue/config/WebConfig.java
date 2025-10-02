package com.dayeyak.queue.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    public static final String ALLOWED_METHOD_NAMES = "GET,HEAD,POST,PUT,DELETE,TRACE,OPTIONS,PATCH";

    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods(ALLOWED_METHOD_NAMES.split(","));
    }

//    @Bean
//    public BucketFilter bucketFilter(){
//        return new BucketFilter();
//    }

//    @Bean
//    public FilterRegistrationBean<ResilienceFilter> resilienceFilterRegistration(ResilienceFilter filter) {
//        FilterRegistrationBean<ResilienceFilter> reg = new FilterRegistrationBean<>();
//        reg.setFilter(filter);
//        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // CORS 뒤, 보안 필터 앞
//        reg.addUrlPatterns("/*");
//        return reg;
//    }
}