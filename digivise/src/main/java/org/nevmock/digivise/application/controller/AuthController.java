package org.nevmock.digivise.application.controller;

import org.nevmock.digivise.application.dto.auth.LoginRequestDto;
import org.nevmock.digivise.application.dto.auth.LoginResponseDto;
import org.nevmock.digivise.application.dto.auth.RefreshRequestDto;
import org.nevmock.digivise.application.dto.generic.GenericDto;
import org.nevmock.digivise.application.dto.user.UserRequestDto;
import org.nevmock.digivise.domain.port.in.AuthService;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto accessToken = authService.login(request.getUsername(), request.getPassword());
        String refreshToken = authService.refreshToken(accessToken.getAccessToken());

        accessToken.setRefreshToken(refreshToken);

        return ResponseEntity.ok(accessToken);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody RefreshRequestDto request) {
        String newAccessToken = authService.refreshToken(request.getRefreshToken());
        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequestDto request) {
        try {
            authService.register(request.getName(), request.getUsername(), request.getEmail(), request.getPassword());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    GenericDto.builder()
                            .data(null)
                            .code(HttpStatusCode.valueOf(400))
                            .status("Bad Request")
                            .error(e.getMessage())
                            .build()
            );
        }
        return ResponseEntity.ok().build();
    }
}