package com.netflix.video_service.config.kafka;

import com.netflix.common_lib.utils.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;


/*
* One service having this configuration is sufficient, as long as that service can connect to the Kafka broker.
* */
@Configuration
public class KafkaTopicConfig {
    @Bean
    public KafkaAdmin.NewTopics createTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(KafkaTopics.VIDEO_UPLOAD_TOPIC)
                        .replicas(1)
                        .partitions(1)
                        .build(),
                TopicBuilder.name(KafkaTopics.VIDEO_ENCODE_TOPIC)
                        .replicas(1)
                        .partitions(1)
                        .build()
        );
    }
}
