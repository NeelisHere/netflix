package com.netflix.encoding_service.service;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.utils.VideoQuality;
import com.netflix.common_lib.utils.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilesAndDirectoryService {
    private final S3Service s3Service;

    public void cleanupJobDirectory(String jobDir) {
        Path dirPath = Paths.get(jobDir);
        if (!Files.exists(dirPath)) {
            log.info("No content on {}", jobDir);
            return;
        }
        try (Stream<Path> filesStream = Files.walk(dirPath)) {
            List<Path> paths = filesStream.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                log.info("deleting file: {}", p.getFileName());
                Files.delete(p);
            }
            log.info("All files deleted from path: {}", jobDir);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void uploadEncodedFilesToS3(String localDir, String prefix) {
        /*
         * localDir=C:\temp\encoding\movie123\encoded
         * prefix=encoded/movie123
         * */
        File dir = new File(localDir);
        uploadDirectoryRecursivelyToS3(dir, localDir, prefix);
        log.info("All encoded files uploaded to S3 at: {}", prefix);
    }

    public void uploadDirectoryRecursivelyToS3(File dir, String localDir, String prefix) {
        for (File currentPath : Objects.requireNonNull(dir.listFiles())) {
            if (currentPath.isDirectory()) {
                uploadDirectoryRecursivelyToS3(currentPath, localDir, prefix);
            } else {
                /*
                 * localDir=C:\temp\encoding\movie123\encoded
                 * prefix=encoded/movie123
                 * relative path:
                 * C:\temp\encoding\movie123\encoded\1080p\playlist.m3u8 -> 1080p/playlist.m3u8
                 * C:\temp\encoding\movie123\encoded\playlist.m3u8 -> playlist.m3u8
                 * */
                String relativePath = currentPath.getAbsolutePath()
                        .substring(localDir.length() + 1)
                        .replace("\\", "/");
                /*
                 * prefix=encoded/movie123
                 * relative path:
                 * C:\temp\encoding\movie123\encoded\1080p\playlist.m3u8 -> 1080p/playlist.m3u8
                 * C:\temp\encoding\movie123\encoded\playlist.m3u8 -> playlist.m3u8
                 * s3Key:
                 * 1080p/playlist.m3u8 -> encoded/movie123/1080p/playlist.m3u8
                 * playlist.m3u8 -> encoded/movie123/playlist.m3u8
                 * */
                String s3Key = prefix + relativePath;
                String contentType = currentPath.getName().endsWith(".m3u8") ? "application/x-mpegURL" : "video/MP2T";
                s3Service.uploadToS3(s3Key, contentType, currentPath);
            }
        }
    }
}
