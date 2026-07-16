package com.netflix.common_lib.dto.response;

import com.netflix.common_lib.dto.Genre;
import com.netflix.common_lib.dto.VideoStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MovieResponse {
    UUID id;
    String title;
    Genre genre;
    Integer durationMinutes;
    String videoKey;
    String hlsUrl;
    VideoStatus videoStatus;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
