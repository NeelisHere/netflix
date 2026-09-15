package com.netflix.video_service.service;

import com.netflix.common_lib.dto.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    private final S3Client s3Client;

    public void uploadToS3(String videoKey, MultipartFile file) {
        log.info("Uploading to S3 with key: {}...", videoKey);
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(videoKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            RequestBody requestBody = RequestBody.fromInputStream(file.getInputStream(), file.getSize());
            s3Client.putObject(putObjectRequest, requestBody);
            log.info("Uploaded to S3 with key: {} successfully!", videoKey);
        } catch (Exception e) {
            log.info("failed to upload: {}", e.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
