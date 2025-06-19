package org.nevmock.digivise.application.dto.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpData {
    private int code;
    private String message;
    @JsonProperty("debug_message")
    private String debugMessage;
    private MerchantInfo data;
}