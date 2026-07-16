package com.netflix.movie_service.repository;


import com.netflix.common_lib.dto.Genre;
import com.netflix.movie_service.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    List<Movie> findByGenre(Genre genre);
}
