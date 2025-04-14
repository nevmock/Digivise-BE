package org.nevmock.digivise.application.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
}
