package com.netflix.common_lib.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VideoUploadEvent {
    UUID movieId;
    String bucketName;
    String videoKey;
    String originalFileName;
    Long fileSizeInBytes;
}
