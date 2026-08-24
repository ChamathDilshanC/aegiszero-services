package com.aegiszero.security.service;

import java.io.Serializable;
import java.time.Instant;

public class SessionRecord implements Serializable {
    private String userId;
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
    private Instant lastActivityAt;

    public SessionRecord() {
    }

    public SessionRecord(String userId, String deviceId, String ipAddress, String userAgent,
                          Instant createdAt, Instant lastActivityAt) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}
