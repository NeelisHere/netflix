package com.netflix.movie_service.controller;

import com.netflix.common_lib.dto.Genre;
import com.netflix.common_lib.dto.request.MovieRequest;
import com.netflix.common_lib.dto.response.MovieResponse;
import com.netflix.movie_service.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/movies")
public class MovieController {
    
    private final MovieService movieService;

    @GetMapping(path = "/{movieId}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable UUID movieId) {
        MovieResponse movieResponse = movieService.findById(movieId);
        return ResponseEntity.ok(movieResponse);
    }

    @GetMapping(path = "/search")
    public ResponseEntity<List<MovieResponse>> searchByGenre(@RequestParam(name = "genre") Genre genre) {
        List<MovieResponse> movieResponses = movieService.searchByGenre(genre);
        return ResponseEntity.ok(movieResponses);
    }

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody MovieRequest movieRequest) {
        MovieResponse movieResponse = movieService.create(movieRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(movieResponse);
    }

    @PutMapping(path = "/{movieId}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable UUID movieId,
            @RequestBody MovieRequest movieRequest) {
        MovieResponse movieResponse = movieService.updateById(movieId, movieRequest);
        return ResponseEntity.ok(movieResponse);
    }

    @DeleteMapping(path = "/{movieId}")
    public ResponseEntity<Map<String, String>> deleteMovie(@PathVariable UUID movieId) {
        movieService.deleteById(movieId);
        return ResponseEntity.ok(
                Map.of("message", String.format("user: %s have been deleted successfully!", movieId))
        );
    }
}
