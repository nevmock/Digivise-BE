package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.auth.LoginResponseDto;

public interface AuthService {
    LoginResponseDto login(String username, String password);

    void logout(String token);

    String refreshToken(String token);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    void register(String name, String username, String password, String email);
}
