package com.netflix.movie_service.service;

import com.netflix.common_lib.dto.Genre;
import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.dto.exception.MovieNotFoundException;
import com.netflix.common_lib.dto.request.MovieRequest;
import com.netflix.common_lib.dto.response.MovieResponse;
import com.netflix.movie_service.entity.Movie;
import com.netflix.movie_service.mapper.MovieMapper;
import com.netflix.movie_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;

    public MovieResponse findById(UUID movieId) {
        try {
            Movie movie = movieRepository
                    .findById(movieId)
                    .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
            log.info("movie: {}", movie);
            return MovieMapper.toResponse(movie);
        } catch (MovieNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public List<MovieResponse> searchByGenre(Genre genre) {
        try {
            List<Movie> movies = movieRepository.findByGenre(genre);
            return movies.stream()
                    .map(MovieMapper::toResponse)
                    .toList();
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public MovieResponse create(MovieRequest movieRequest) {
        try {
            Movie createdMovie = movieRepository.save(MovieMapper.toEntity(movieRequest));
            return MovieMapper.toResponse(createdMovie);
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public MovieResponse updateById(UUID movieId, MovieRequest movieRequest) {
        try {
            Movie movie = movieRepository
                    .findById(movieId)
                    .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
            movie.setTitle(movieRequest.getTitle());
            movie.setDurationMinutes(movieRequest.getDurationMinutes());
            movie.setGenre(movieRequest.getGenre());
            Movie updatedMovie = movieRepository.save(movie);

            return MovieMapper.toResponse(updatedMovie);
        } catch (MovieNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public void deleteById(UUID movieId) {
        try {
            movieRepository
                    .findById(movieId)
                    .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
            movieRepository.deleteById(movieId);
        } catch (MovieNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }
}
