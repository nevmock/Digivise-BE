package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.user.UserRequestDto;
import org.nevmock.digivise.application.dto.user.UserResponseDto;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseDto createUser(UserRequestDto user);

    UserResponseDto getUserById(UUID userId);

    UserResponseDto getUserByUsername(String username);

    List<UserResponseDto> getAllUsers();

    UserResponseDto updateUser(UUID userId, UserRequestDto user);

    void deleteUser(UUID userId);
}