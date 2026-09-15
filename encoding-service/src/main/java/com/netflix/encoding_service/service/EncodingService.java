package com.netflix.encoding_service.service;

import com.netflix.common_lib.dto.event.VideoEncodeEvent;
import com.netflix.common_lib.dto.event.VideoUploadEvent;
import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.utils.VideoQuality;
import com.netflix.common_lib.utils.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*****************************
 tempDir=C:/temp/encoding
jobDir=C:/temp/encoding/abc123/

C:/temp/encoding/abc123/
│
├── raw_video.mp4
│
└── encoded/
     ├── master.m3u8
     │
     ├── 1080p/
     │   ├── playlist.m3u8
     │   ├── segment_000.ts
     │   └── segment_001.ts
     │
     └── 720p/
         ├── playlist.m3u8
         ├── segment_000.ts
         └── segment_001.ts
...
******************************/


@Slf4j
@Service
@RequiredArgsConstructor
public class EncodingService {

    @Value("${encoding.temp-dir}")
    private String tempDir;

    private final KafkaTemplate<String, VideoEncodeEvent> kafkaVideoEncodeTemplate;
    private final FilesAndDirectoryService filesAndDirectoryService;
    private final FfmpegService ffmpegService;
    private final S3Service s3Service;

    public void encodeVideo(VideoUploadEvent videoUploadEvent) {
        log.info("Starting the encoding pipeline for movie: {}", videoUploadEvent.getMovieId());
        String jobDir = tempDir + "/" + videoUploadEvent.getMovieId();
        try {
            Files.createDirectories(Paths.get(jobDir));
            Files.createDirectories(Paths.get(jobDir + "/encoded"));

            // Download raw video from S3
            String rawVideoFilePath = jobDir + "/raw_video.mp4";
            s3Service.downloadFileFromS3(videoUploadEvent.getVideoKey(), rawVideoFilePath);

            // Encode to multiple qualities + generate HLS
            for (VideoQuality videoQuality : VideoUtils.VIDEO_QUALITIES) {
                String qualityDir = jobDir + "/encoded/" + videoQuality.height() + "p";
                Files.createDirectories(Paths.get(qualityDir));
                ffmpegService.encodeToHls(rawVideoFilePath, qualityDir, videoQuality);
            }

            // generate master playlist
            String masterPlaylistPath = jobDir + "/encoded/master.m3u8";
            ffmpegService.generateMasterPlaylist(masterPlaylistPath);

            // Upload all encoded files to S3
            String prefix = "encoded/" + videoUploadEvent.getMovieId() + "/";
            String localDir = jobDir + "/encoded";
            filesAndDirectoryService.uploadEncodedFilesToS3(localDir, prefix);

            // Publish VideoEncodedEvent
            String masterPlaylistKey = prefix + "master.m3u8";
            String hlsUrl = s3Service.getHlsS3Url(masterPlaylistKey);
            VideoEncodeEvent videoEncodeEvent = VideoEncodeEvent.builder()
                    .movieId(videoUploadEvent.getMovieId())
                    .hlsUrl(hlsUrl)
                    .masterPlaylistKey(masterPlaylistKey)
                    .success(true)
                    .errorMessage(null)
                    .build();
            kafkaVideoEncodeTemplate.sendDefault(String.valueOf(videoUploadEvent.getMovieId()), videoEncodeEvent);
            log.info("VideoEncodeEvent published: {}", videoEncodeEvent);
        } catch (Exception e) {
            log.error(e.getMessage());
            VideoEncodeEvent videoEncodeEvent = VideoEncodeEvent.builder()
                    .movieId(videoUploadEvent.getMovieId())
                    .hlsUrl(null)
                    .masterPlaylistKey(null)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            kafkaVideoEncodeTemplate.sendDefault(String.valueOf(videoUploadEvent.getMovieId()), videoEncodeEvent);
        } finally {
            filesAndDirectoryService.cleanupJobDirectory(jobDir);
        }
    }
}
