package com.deltatrade.platform.common.auth;

public class AuthPrincipal {

    private final String token;
    private final Long userId;
    private final String nickname;
    private final String phone;
    private final boolean verified;
    private final boolean hasPassword;

    public AuthPrincipal(String token, Long userId, String nickname, String phone, boolean verified, boolean hasPassword) {
        this.token = token;
        this.userId = userId;
        this.nickname = nickname;
        this.phone = phone;
        this.verified = verified;
        this.hasPassword = hasPassword;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isHasPassword() {
        return hasPassword;
    }
}
