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

    @Value("${aws.region}")
    private String region;

    public String getHlsS3Url(String masterPlaylistKey) {
        // https://<bucket-name>.s3.<region>.amazonaws.com/<masterPlaylistKey>
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + masterPlaylistKey;
    }

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

    public void uploadToS3(String s3Key, String contentType, File currentPath) {
        PutObjectRequest request = buildUploadRequest(s3Key, contentType);
        s3Client.putObject(request, RequestBody.fromFile(currentPath));
        log.info("file: {} uploaded to S3.", currentPath);
    }
}
