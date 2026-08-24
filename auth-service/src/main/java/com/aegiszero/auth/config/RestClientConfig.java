package com.aegiszero.auth.config;

import com.aegiszero.common.security.internal.InternalApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient accessServiceRestClient(ServiceUrlsProperties properties,
                                               @Value("${aegiszero.internal-api.key:}") String internalApiKey) {
        return RestClient.builder()
                .baseUrl(properties.getAccessServiceUrl())
                .defaultHeader(InternalApiKeyFilter.HEADER_NAME, internalApiKey)
                .build();
    }

    @Bean
    public RestClient securityServiceRestClient(ServiceUrlsProperties properties,
                                                 @Value("${aegiszero.internal-api.key:}") String internalApiKey) {
        return RestClient.builder()
                .baseUrl(properties.getSecurityServiceUrl())
                .defaultHeader(InternalApiKeyFilter.HEADER_NAME, internalApiKey)
                .build();
    }
}
