package org.nevmock.digivise.domain.model.mongo.ads;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public class ProfileWrapper {
    @Field("profile_info")
    private ProfileInfo profileInfo;
}
