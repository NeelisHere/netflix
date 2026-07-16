package com.netflix.common_lib.dto.request;

import com.netflix.common_lib.dto.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {
    private String title;
    private Integer durationMinutes;
    private Genre genre;
}

// Made with Bob
