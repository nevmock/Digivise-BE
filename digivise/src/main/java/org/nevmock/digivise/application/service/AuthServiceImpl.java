package org.nevmock.digivise.application.service;

import org.nevmock.digivise.application.dto.auth.LoginResponseDto;
import org.nevmock.digivise.application.dto.kpi.KPIResponseDto;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;
import org.nevmock.digivise.application.dto.user.UserRequestDto;
import org.nevmock.digivise.application.dto.user.UserResponseDto;
import org.nevmock.digivise.domain.model.User;
import org.nevmock.digivise.domain.port.in.AuthService;
import org.nevmock.digivise.domain.port.in.UserService;
import org.nevmock.digivise.infrastructure.adapter.security.JwtTokenProvider;
import org.nevmock.digivise.infrastructure.adapter.security.RefreshTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final UserService userService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider,
                           RefreshTokenProvider refreshTokenProvider,
                           UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenProvider = refreshTokenProvider;
        this.userService = userService;
    }

    @Override
    public LoginResponseDto login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(username);

        UserResponseDto user = userService.getUserByUsername(authentication.getName());


        return LoginResponseDto
                .builder()
                .accessToken(token)
                .username(username)
                .userId(user.getId())
                .merchants(user.getMerchants().stream().map(merchant -> MerchantResponseDto
                        .builder()
                        .id(merchant.getId())
                        .merchantName(merchant.getMerchantName())
                        .merchantShopeeId(merchant.getMerchantShopeeId())
                        .userId(user.getId())
                        .createdAt(merchant.getCreatedAt())
                        .updatedAt(merchant.getUpdatedAt())
                        .build()).collect(Collectors.toList()))
                .activeMerchant(
                        user.getActiveMerchant() != null ? MerchantResponseDto
                                .builder()
                                .id(user.getActiveMerchant().getId())
                                .merchantName(user.getActiveMerchant().getMerchantName())
                                .merchantShopeeId(user.getActiveMerchant().getMerchantShopeeId())
                                .userId(user.getId())
                                .createdAt(user.getActiveMerchant().getCreatedAt())
                                .updatedAt(user.getActiveMerchant().getUpdatedAt())
                                .officeAddress(user.getActiveMerchant().getOfficeAddress())
                                .factoryAddress(user.getActiveMerchant().getFactoryAddress())
                                .sectorIndustry(user.getActiveMerchant().getSectorIndustry())
                                .name(user.getActiveMerchant().getName())
                                .build() : null
                )
                .build();
    }

    @Override
    public void logout(String token) {
        System.out.println("Token logged out: " + token);
    }

    @Override
    public String refreshToken(String token) {
        if (refreshTokenProvider.validateToken(token)) {
            String username = refreshTokenProvider.getUsernameFromJWT(token);
            return jwtTokenProvider.generateToken(username);
        } else {
            throw new RuntimeException("Invalid refresh token");
        }
    }

    @Override
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromJWT(token);
    }

    @Override
    public void register(String name, String username, String email, String password) {
        try {
            userService.createUser(new UserRequestDto(name, username, email, password));
        } catch (Exception e) {
            throw new RuntimeException("User registration failed: " + e.getMessage());
        }
    }
}