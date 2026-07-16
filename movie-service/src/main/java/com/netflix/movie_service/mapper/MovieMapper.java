package com.netflix.movie_service.mapper;

import com.netflix.common_lib.dto.request.MovieRequest;
import com.netflix.common_lib.dto.response.MovieResponse;
import com.netflix.movie_service.entity.Movie;

public class MovieMapper {
    public static MovieResponse toResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .genre(movie.getGenre())
                .durationMinutes(movie.getDurationMinutes())
                .videoKey(movie.getVideoKey())
                .hlsUrl(movie.getHlsUrl())
                .videoStatus(movie.getVideoStatus())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }

    public static Movie toEntity(MovieRequest movieRequest) {
        return Movie.builder()
                .title(movieRequest.getTitle())
                .durationMinutes(movieRequest.getDurationMinutes())
                .genre(movieRequest.getGenre())
                .build();
    }
}
