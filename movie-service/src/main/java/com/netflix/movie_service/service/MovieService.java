package com.netflix.movie_service.service;

import com.netflix.common_lib.dto.Genre;
import com.netflix.common_lib.dto.VideoStatus;
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
        Movie movie = movieRepository
                .findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
        log.info("movie: {}", movie);
        return MovieMapper.toResponse(movie);
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
        Movie movie = movieRepository
                .findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
        movie.setTitle(movieRequest.getTitle());
        movie.setDurationMinutes(movieRequest.getDurationMinutes());
        movie.setGenre(movieRequest.getGenre());
        try {
            Movie updatedMovie = movieRepository.save(movie);
            return MovieMapper.toResponse(updatedMovie);
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public void deleteById(UUID movieId) {
        movieRepository
                .findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
        try {
            movieRepository.deleteById(movieId);
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public void updateHlsUrlById(UUID movieId, String hlsUrl) {
        Movie movie = movieRepository
                .findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
        try {
            movie.setHlsUrl(hlsUrl);
            movieRepository.saveAndFlush(movie);
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public void updateVideoStatusById(UUID movieId, VideoStatus videoStatus) {
        Movie movie = movieRepository
                .findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
        try {
            movie.setVideoStatus(videoStatus);
            movieRepository.saveAndFlush(movie);
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }

    public void updateVideoKeyById(UUID movieId, String videoKey) {
        Movie movie = movieRepository
                .findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(HttpStatus.NOT_FOUND, movieId));
        try {
            movie.setVideoKey(videoKey);
            movieRepository.saveAndFlush(movie);
        } catch (RuntimeException e) {
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, "Some error happened internally!");
        }
    }
}
