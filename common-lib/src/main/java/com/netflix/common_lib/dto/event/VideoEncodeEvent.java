package com.netflix.common_lib.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoEncodeEvent {
    UUID movieId;
    String hlsUrl;
    String masterPlaylistKey;
    boolean success;
    String errorMessage;
}
