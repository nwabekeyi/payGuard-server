package com.payguard.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dojah")
public class DojahConfig {
    private String clientId;
    private String clientSecret;
    private String baseUrl = "https://api.dojah.io";
    private String businessRefId;
}