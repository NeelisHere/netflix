package com.netflix.movie_service.consumer;

import com.netflix.common_lib.dto.VideoStatus;
import com.netflix.common_lib.dto.event.VideoEncodeEvent;
import com.netflix.common_lib.dto.event.VideoUploadEvent;
import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.utils.KafkaTopics;
import com.netflix.movie_service.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEncodeEventConsumer {
    private final MovieService movieService;

    @KafkaListener(topics = {KafkaTopics.VIDEO_ENCODE_TOPIC}, groupId = "${spring.kafka.consumer.group}")
    public void consumeVideoEncodeEvent(VideoEncodeEvent videoEncodeEvent) {
        log.info("VideoEncodeEvent received: {}", videoEncodeEvent);
        UUID movieId = videoEncodeEvent.getMovieId();
        if (videoEncodeEvent.isSuccess()) {
            movieService.updateHlsUrlById(movieId, videoEncodeEvent.getHlsUrl());
            movieService.updateVideoStatusById(movieId, VideoStatus.ENCODED);
        } else {
            movieService.updateVideoStatusById(movieId, VideoStatus.FAILED);
        }
    }

    @KafkaListener(topics = {KafkaTopics.VIDEO_UPLOAD_TOPIC}, groupId = "${spring.kafka.consumer.group}")
    public void consumeVideoUploadEvent(VideoUploadEvent videoUploadEvent) {
        log.info("VideoUploadEvent received: {}", videoUploadEvent);
        UUID movieId = videoUploadEvent.getMovieId();
        movieService.updateVideoKeyById(movieId, videoUploadEvent.getVideoKey());
        movieService.updateVideoStatusById(movieId, VideoStatus.UPLOADED);
    }
}
