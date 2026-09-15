package com.netflix.encoding_service.service;

import com.netflix.common_lib.dto.event.VideoUploadEvent;
import com.netflix.common_lib.utils.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadEventConsumer {
    private final EncodingService encodingService;

    @KafkaListener(topics = {KafkaTopics.VIDEO_UPLOAD_TOPIC}, groupId = "${spring.kafka.consumer.group}")
    public void consumeVideoUploadEvent(VideoUploadEvent videoUploadEvent) {
        log.info("video upload event received: {}", videoUploadEvent);
        encodingService.encodeVideo(videoUploadEvent);
    }
}
