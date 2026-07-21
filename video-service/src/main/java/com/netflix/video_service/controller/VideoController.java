package com.netflix.video_service.controller;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.video_service.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/video")
public class VideoController {
    private final VideoService videoService;

    @PostMapping(
            path = "/upload/{movieId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable(name = "movieId") UUID movieId,
            @RequestPart(name = "file") MultipartFile file) {
        log.info("video upload request for file: {}, size: {}MB", file.getName(), file.getSize() / 1024 * 1024);
        if (file.isEmpty()) {
            throw new CommonException(HttpStatus.BAD_REQUEST, "No File found!");
        }
        String videoKey = videoService.uploadVideo(movieId, file);
        return ResponseEntity.ok(
                Map.of("message", String.format("Video uploaded successfully! key: %s", videoKey))
        );
    }
}
