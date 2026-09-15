package com.netflix.streaming_service.service;

import com.netflix.common_lib.dto.event.VideoEncodeEvent;
import com.netflix.common_lib.utils.KafkaTopics;
import com.netflix.common_lib.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoEncodeEventConsumer {
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = KafkaTopics.VIDEO_ENCODE_TOPIC, groupId = "${spring.kafka.consumer.group}")
    public void videoEncodeEventConsumer(VideoEncodeEvent videoEncodeEvent) {
        log.info("Consumed VideoEncodeEvent: {}", videoEncodeEvent);
        if (videoEncodeEvent.isSuccess()) {
            String cacheKey = RedisUtils.MASTER_PLAYLIST_KEY_PREFIX + videoEncodeEvent.getMovieId().toString();
            redisTemplate.opsForValue().set(cacheKey, videoEncodeEvent.getMasterPlaylistKey());
            log.info(
                    "Master playlist key cached.\nmovieId: {}\nmaster playlist key: {}",
                    videoEncodeEvent.getMovieId().toString(),
                    videoEncodeEvent.getMasterPlaylistKey()
            );
        } else {
            log.info("VideoEncodeEvent failed to process!");
        }
    }
}
