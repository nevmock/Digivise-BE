package org.nevmock.digivise.application.dto.auth;

public class RefreshRequestDto {
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}