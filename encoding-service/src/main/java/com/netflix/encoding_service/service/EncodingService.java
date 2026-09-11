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
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.temp-dir}")
    private String tempDir;

    private final KafkaTemplate<String, VideoEncodeEvent> kafkaVideoEncodeTemplate;
    private final FilesAndDirectoryService filesAndDirectoryService;
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
                encodeToHls(rawVideoFilePath, qualityDir, videoQuality);
            }

            // generate master playlist
            String masterPlaylistPath = jobDir + "/encoded/master.m3u8";
            filesAndDirectoryService.generateMasterPlaylist(masterPlaylistPath);

            // Upload all encoded files to S3
            String prefix = "encoded/" + videoUploadEvent.getMovieId() + "/";
            s3Service.uploadEncodedFilesToS3(jobDir + "/encoded", prefix);

            // Publish VideoEncodedEvent
            String masterPlaylistKey = prefix + "master.m3u8";
            String hlsUrl = "https://" + bucketName + ".s3.amazonaws.com/" + masterPlaylistKey;
            VideoEncodeEvent encodeEvent = VideoEncodeEvent.builder()
                    .movieId(videoUploadEvent.getMovieId())
                    .hlsUrl(hlsUrl)
                    .masterPlaylistKey(masterPlaylistKey)
                    .success(true)
                    .errorMessage(null)
                    .build();
            kafkaVideoEncodeTemplate.sendDefault(String.valueOf(videoUploadEvent.getMovieId()), encodeEvent)
                    .whenComplete((result, e) -> {
                        if (e != null) {
                            log.error("Failed to publish video upload event: {}", e.getMessage());
                        } else {
                            RecordMetadata metadata = result.getRecordMetadata();
                            log.info("Message published to topic={}, partition={}, offset={}",
                                    metadata.topic(),
                                    metadata.partition(),
                                    metadata.offset()
                            );
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            filesAndDirectoryService.cleanupJobDirectory(jobDir);
        }
    }

    public void encodeToHls(String inputPath, String outputPath, VideoQuality videoQuality) {
        log.info("Encoding to HLS...");
        List<String> command = createFfmpegCommandForHlsEncoding(inputPath, outputPath, videoQuality);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exit_code = process.waitFor();
            if (exit_code != 0) {
                String message = String.format("ffmpeg encoding filed with exit_code=%s", exit_code);
                throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }
            log.info("Encoded {}p successfully!", videoQuality.height());
        } catch (IOException | InterruptedException e) {
            log.info("encoding failed, reason: {}", e.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public List<String> createFfmpegCommandForHlsEncoding(
            String inputPath, String outputPath, VideoQuality videoQuality) {

        String playlistPath = outputPath + "/playlist.m3u8";
        String segmentPattern = outputPath + "/segment_%03d.ts";
        return Arrays.asList(
                ffmpegPath,
                "-i", inputPath,
                "-vf", "scale=" + videoQuality.width() + ":" + videoQuality.height(),
                "-c:v", "libx264",
                "-b:v", videoQuality.bitrate() + "k",
                "-c:a", "aac",
                "-b:a", "128k",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                "-f", "hls",
                playlistPath
        );
    }
}
