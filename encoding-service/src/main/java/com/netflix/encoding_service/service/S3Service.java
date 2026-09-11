package com.netflix.encoding_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public GetObjectRequest buildDownloadRequest(String s3Key) {
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
    }

    public PutObjectRequest buildUploadRequest(String s3Key, String contentType) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();
    }

    public void downloadFileFromS3(String s3Key, String localDestinationPath) {
        GetObjectRequest request = buildDownloadRequest(s3Key);
        s3Client.getObject(request, Paths.get(localDestinationPath));

        log.info("Raw video file downloaded from S3 to local path: {}", localDestinationPath);
    }

    public void uploadEncodedFilesToS3(String localDir, String prefix) {
        File dir = new File(localDir);
        uploadDirectoryRecursivelyToS3(dir, localDir, prefix);
    }

    public void uploadDirectoryRecursivelyToS3(File dir, String localDir, String prefix) {
        for (File currentPath : Objects.requireNonNull(dir.listFiles())) {
            if (currentPath.isDirectory()) {
                uploadDirectoryRecursivelyToS3(currentPath, localDir, prefix);
            } else {
                /*
                * C:\temp\encoding\abc123\encoded\1080p\playlist.m3u8 -> 1080p/playlist.m3u8
                * */
                String relativePath = currentPath.getAbsolutePath()
                        .substring(localDir.length() + 1)
                        .replace("\\", "/");
                /*
                * 1080p/playlist.m3u8 -> encoded/movie123/1080p/playlist.m3u8
                * */
                String s3Key = prefix + relativePath;
                String contentType = currentPath.getName().endsWith(".m3u8")
                        ? "application/x-mpegURL"
                        : "video/MP2T";
                PutObjectRequest request = buildUploadRequest(s3Key, contentType);
                s3Client.putObject(request, RequestBody.fromFile(currentPath));
                log.info("All encoded files uploaded to S3: {}", s3Key);
            }
        }
    }
}
