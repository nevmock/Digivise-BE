package org.nevmock.digivise.domain.port.in;

public interface AuthService {
    String login(String username, String password);
    void logout(String token);
    String refreshToken(String token);
    boolean validateToken(String token);
    String getUsernameFromToken(String token);
    void register(String name, String username, String password, String email);
}
