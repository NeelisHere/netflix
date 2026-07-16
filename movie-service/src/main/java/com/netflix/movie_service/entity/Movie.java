package com.netflix.movie_service.entity;

import com.netflix.common_lib.dto.Genre;
import com.netflix.common_lib.dto.VideoStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "netflix_movies")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    String title;
    @Enumerated(EnumType.STRING)
    Genre genre;
    Integer durationMinutes;
    String videoKey;
    String hlsUrl;
    @Enumerated(EnumType.STRING)
    VideoStatus videoStatus;
    @CreationTimestamp
    LocalDateTime createdAt;
    @UpdateTimestamp
    LocalDateTime updatedAt;
}
