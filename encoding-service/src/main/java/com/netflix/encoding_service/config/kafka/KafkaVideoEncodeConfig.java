package com.netflix.encoding_service.config.kafka;

import com.netflix.common_lib.dto.event.VideoEncodeEvent;
import com.netflix.common_lib.utils.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaVideoEncodeConfig {
    @Bean
    public KafkaTemplate<String, VideoEncodeEvent> videoEncodeEventKafkaTemplate(
            ProducerFactory<String, VideoEncodeEvent> producerFactory) {
        KafkaTemplate<String, VideoEncodeEvent> template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic(KafkaTopics.VIDEO_ENCODE_TOPIC);
        return template;
    }
}
