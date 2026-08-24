package com.aegiszero.security.service;

import java.io.Serializable;

public class MfaChallenge implements Serializable {
    private String userId;
    private String method;
    private String codeHash;
    private int attempts;

    public MfaChallenge() {
    }

    public MfaChallenge(String userId, String method, String codeHash, int attempts) {
        this.userId = userId;
        this.method = method;
        this.codeHash = codeHash;
        this.attempts = attempts;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
}
