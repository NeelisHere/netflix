package com.netflix.video_service.service;

import com.netflix.common_lib.dto.event.VideoUploadEvent;
import com.netflix.common_lib.dto.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    private final S3Service s3Service;
    private final KafkaTemplate<String, VideoUploadEvent> kafkaVideoUploadTemplate;

    public String uploadVideo(UUID movieId, MultipartFile file) {
        log.info("movie: {}, file: {} received", movieId, file.getOriginalFilename());
        // key=raw/movie123/08a7sdf09a8sd7f0a9s8d7_uploadedFileName.mp4
        String videoKey = "raw/" + movieId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        s3Service.uploadToS3(videoKey, file);

        VideoUploadEvent videoUploadEvent = VideoUploadEvent.builder()
                .movieId(movieId)
                .bucketName(bucketName)
                .videoKey(videoKey)
                .originalFileName(file.getOriginalFilename())
                .fileSizeInBytes(file.getSize())
                .build();

        kafkaVideoUploadTemplate.sendDefault(videoUploadEvent)
                .whenComplete((result, e) -> {
                    if (e != null) {
                        log.info("Failed to upload VideoUploadEvent: {}", e.getMessage());
                        throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                    } else {
                        RecordMetadata metadata = result.getRecordMetadata();
                        log.info(
                                "Message sent successfully. Topic={}, Partition={}, Offset={}",
                                metadata.topic(),
                                metadata.partition(),
                                metadata.offset()
                        );
                    }
                });

        return videoKey;
    }
}