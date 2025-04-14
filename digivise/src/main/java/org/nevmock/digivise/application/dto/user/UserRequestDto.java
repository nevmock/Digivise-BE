package org.nevmock.digivise.application.dto.user;

import lombok.*;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {
    private String name;

    private String username;

    private String email;

    private String password;
}
