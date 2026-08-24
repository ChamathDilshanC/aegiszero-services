package com.aegiszero.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aegiszero.services")
public class ServiceUrlsProperties {

    private String accessServiceUrl = "http://localhost:8083";
    private String securityServiceUrl = "http://localhost:8084";

    public String getAccessServiceUrl() {
        return accessServiceUrl;
    }

    public void setAccessServiceUrl(String accessServiceUrl) {
        this.accessServiceUrl = accessServiceUrl;
    }

    public String getSecurityServiceUrl() {
        return securityServiceUrl;
    }

    public void setSecurityServiceUrl(String securityServiceUrl) {
        this.securityServiceUrl = securityServiceUrl;
    }
}
