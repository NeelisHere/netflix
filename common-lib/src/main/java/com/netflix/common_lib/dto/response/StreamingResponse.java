package com.netflix.common_lib.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StreamingResponse {
    UUID movieId;
    String StreamingUrl;
    String quality;
    Long expiresInMinutes;
}
