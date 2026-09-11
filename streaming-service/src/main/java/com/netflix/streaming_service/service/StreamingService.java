package com.netflix.streaming_service.service;

import com.netflix.common_lib.dto.exception.CommonException;
import com.netflix.common_lib.dto.response.StreamingResponse;
import com.netflix.common_lib.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {
    private final S3Service s3Service;
    private final StringRedisTemplate redisTemplate;

    @Value("${aws.s3.presigned-url-expiry}")
    private Long presignedUrlExpiryTimeInMinutes;

    public StreamingResponse getStreamingUrl(UUID movieId) {
        log.info("getting streaming url for the movie");
        String streamingUrlCacheKey = RedisUtils.STREAMING_URL_CACHE_PREFIX + movieId;
        String masterPlaylistCacheKey = RedisUtils.MASTER_PLAYLIST_KEY_PREFIX + movieId;

        String streamingUrl = redisTemplate.opsForValue().get(streamingUrlCacheKey);
        if (streamingUrl != null) {
            log.info("streaming url for movie: {} found in the redis cache", movieId);
            return StreamingResponse.builder()
                    .movieId(movieId)
                    .StreamingUrl(streamingUrl)
                    .quality("1080p, 720p, 480p, 360p")
                    .expiresInMinutes(presignedUrlExpiryTimeInMinutes)
                    .build();
        }
        String masterPlaylistKey = redisTemplate.opsForValue().get(masterPlaylistCacheKey);
        if (masterPlaylistKey == null) {
            throw new CommonException(
                    HttpStatus.NOT_FOUND,
                    "masterPlaylistKey not found, movie not ready for streaming"
            );
        }

        String preSignedMasterPlaylistUrl = s3Service.generatePreSignedUrl(masterPlaylistKey);
        redisTemplate.opsForValue()
                .set(streamingUrlCacheKey, preSignedMasterPlaylistUrl, 55, TimeUnit.MINUTES);
        log.info("streaming url cached for movieId: {} for 55 mins", movieId);
        return StreamingResponse.builder()
                .movieId(movieId)
                .StreamingUrl(preSignedMasterPlaylistUrl)
                .quality("1080p, 720p, 480p, 360p")
                .expiresInMinutes(presignedUrlExpiryTimeInMinutes)
                .build();
    }

    public String getSignedPlaylist(UUID movieId, String playlistPath) {
        log.info("getting signed playlist - movieId: {}, playlistPath: {}", movieId, playlistPath);
        String basePath = playlistPath.substring(0, playlistPath.lastIndexOf("/") + 1);
        String m3u8Content = s3Service.readFromS3(playlistPath);
        return rewriteM3u8WithSignedUrls(m3u8Content, basePath);
    }

    private String rewriteM3u8WithSignedUrls(String m3u8Content, String basePath) {
        StringBuilder rewritten = new StringBuilder();
        for(String line : m3u8Content.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                rewritten.append(line).append('\n');
                continue;
            }
            String fullKey = basePath + line;
            String signedUrl = s3Service.generatePreSignedUrl(fullKey);
            rewritten.append(signedUrl).append('\n');
        }
        return rewritten.toString();
    }
}
