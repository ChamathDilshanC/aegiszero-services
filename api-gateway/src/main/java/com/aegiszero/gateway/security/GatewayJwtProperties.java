package com.aegiszero.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aegiszero.jwt")
public class GatewayJwtProperties {

    private String secret = "change-this-secret-change-this-secret-change-this-secret";
    private String issuer = "aegiszero";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
