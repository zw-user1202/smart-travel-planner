package com.travelplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String jwtSecret;
    private int jwtExpireHours = 72;
    private Deepseek deepseek = new Deepseek();
    private Amap amap = new Amap();

    @Data
    public static class Deepseek {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
    }

    @Data
    public static class Amap {
        private String key;
        private String baseUrl = "https://restapi.amap.com";
    }
}
