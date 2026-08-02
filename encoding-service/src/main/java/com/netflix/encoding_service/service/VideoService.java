package com.netflix.encoding_service.service;

import com.netflix.common_lib.dto.event.VideoUploadEvent;
import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.utils.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    private final EncodingService encodingService;

    @KafkaListener(topics = {KafkaTopics.VIDEO_UPLOAD_TOPIC}, groupId = "${spring.kafka.consumer.group}")
    public void consumeVideoUploadEvent(VideoUploadEvent videoUploadEvent) {
        log.info("video upload event received: {}", videoUploadEvent);
        try {
            encodingService.encodeVideo(videoUploadEvent);
        } catch (RuntimeException e) {
            String errorMessage = String.format(
                    "Failed to encode!\nmovieId: %s\nreason: %s",
                    videoUploadEvent.getMovieId(),
                    e.getMessage()
            );
            log.info(errorMessage);
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }
    }
}
