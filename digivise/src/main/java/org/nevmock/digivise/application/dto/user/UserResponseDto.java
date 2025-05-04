package org.nevmock.digivise.application.dto.user;

import lombok.*;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;
import org.nevmock.digivise.domain.model.Merchant;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto implements Serializable {

    @Getter
    private UUID id;

    @Getter
    private String username;

    @Getter
    private String name;

    @Getter
    private String email;

    @Getter
    private Timestamp createdAt;

    @Getter
    private Timestamp updatedAt;

    private List<Merchant> merchants;
}
