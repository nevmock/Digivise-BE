package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;
import org.nevmock.digivise.application.dto.user.UserRequestDto;
import org.nevmock.digivise.application.dto.user.UserResponseDto;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.User;
import org.nevmock.digivise.domain.port.in.UserService;
import org.nevmock.digivise.domain.port.out.UserRepository;
import org.nevmock.digivise.infrastructure.config.exceptions.AlreadyExistException;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.nevmock.digivise.utils.UtilsKt.hashPasswordBcrypt;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public String createUser(UserRequestDto user) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        User newUser = new User();

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new AlreadyExistException(
                    "User already exists with email: " + user.getEmail() + ". Please choose a different email."
            );
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new AlreadyExistException(
                    "User already exists with username: " + user.getUsername() + ". Please choose a different username."
            );
        }

        newUser.setId(UUID.randomUUID());
        newUser.setName(user.getName());
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(hashPasswordBcrypt(user.getPassword()));
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        userRepository.save(newUser);

        return "Login successful. User created with ID: " + newUser.getId();
    }

    @Override
    public UserResponseDto getUserById(UUID userId) {
        User newUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        return toDto(newUser);
    }

    @Override
    public UserResponseDto getUserByUsername(String username) {
        User newUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        return toDto(newUser);
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDto updateUser(UUID userId, UserRequestDto updatedUser) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPassword(updatedUser.getPassword());
        existingUser.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        userRepository.save(existingUser);

        return toDto(existingUser);
    }

    @Override
    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }

    private UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .merchants(user.getMerchants().stream().map(
                        this::toMerchantDto
                ).collect(Collectors.toList()))
                .createdAt(Timestamp.valueOf(user.getCreatedAt().toString()))
                .updatedAt(Timestamp.valueOf(user.getUpdatedAt().toString()))
                .activeMerchant(toMerchantDto(user.getActiveMerchant()))
                .username(user.getUsername())
                .build();
    }

    private MerchantResponseDto toMerchantDto(Merchant merchant) {
        if (merchant == null) {
            return null;
        }
        return MerchantResponseDto.builder()
                .id(merchant.getId())
                .merchantName(merchant.getMerchantName())
                .merchantShopeeId(merchant.getMerchantShopeeId())
                .createdAt(Timestamp.valueOf(merchant.getCreatedAt().toString()))
                .updatedAt(Timestamp.valueOf(merchant.getUpdatedAt().toString()))
                .merchantShopeeId(merchant.getMerchantShopeeId())
                .officeAddress(merchant.getOfficeAddress())
                .factoryAddress(merchant.getFactoryAddress())
                .sectorIndustry(merchant.getSectorIndustry())
                .name(merchant.getName())
                .lastLogin(merchant.getLastLogin())
                .build();
    }
}
