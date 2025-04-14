package org.nevmock.digivise.application.dto.generic;

import lombok.*;
import org.springframework.http.HttpStatusCode;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericDto<T extends Serializable> {
    private String error;
    private HttpStatusCode code;
    private String status;
    private T data;
}
