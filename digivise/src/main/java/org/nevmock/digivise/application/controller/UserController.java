package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.generic.GenericDto;
import org.nevmock.digivise.application.dto.user.UserRequestDto;
import org.nevmock.digivise.application.dto.user.UserResponseDto;
import org.nevmock.digivise.domain.port.in.UserService;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<GenericDto<UserResponseDto>> createUser(@RequestBody UserRequestDto user) {

        try {
            UserResponseDto userResponseDto = userService.createUser(user);
            return ResponseEntity.ok(
                    GenericDto.<UserResponseDto>builder()
                            .data(userResponseDto)
                            .code(HttpStatusCode.valueOf(201))
                            .status("Created")
                            .error(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    GenericDto.<UserResponseDto>builder()
                            .data(null)
                            .code(HttpStatusCode.valueOf(400))
                            .status("Bad Request")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID id, @RequestBody UserRequestDto user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
