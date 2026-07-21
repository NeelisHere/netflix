package com.netflix.video_service.config.kafka;

import com.netflix.common_lib.dto.event.VideoUploadEvent;
import com.netflix.common_lib.utils.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaVideoUploadConfig {
    @Bean
    public KafkaTemplate<String, VideoUploadEvent> videoUploadKafkaTemplate(
            ProducerFactory<String, VideoUploadEvent> producerFactory) {
        KafkaTemplate<String, VideoUploadEvent> template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic(KafkaTopics.VIDEO_UPLOAD_TOPIC);
        return template;
    }
}
