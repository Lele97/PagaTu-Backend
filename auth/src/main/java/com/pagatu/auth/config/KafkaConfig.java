package com.pagatu.auth.config;



import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    //@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public ProducerFactory<String, Map<String, Object>> mockProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // Configurazioni minime necessarie
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Map<String, Object>> kafkaTemplate(ProducerFactory<String, Map<String, Object>> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

//    @Bean
//    @ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "false", matchIfMissing = true)
//    public KafkaTemplate<String, Map<String, Object>> kafkaTemplate(ProducerFactory<String, Map<String, Object>> mockProducerFactory) {
//        KafkaTemplate<String, Map<String, Object>> template = new KafkaTemplate<>(mockProducerFactory);
//        // Sovrascrivi il metodo send per evitare connessioni reali
//        return new KafkaTemplate<String, Map<String, Object>>(mockProducerFactory) {
//            @Override
//            public void flush() {
//                // Non fare nulla
//            }
//
//            @Override
//            public java.util.concurrent.CompletableFuture<org.springframework.kafka.support.SendResult<String, Map<String, Object>>> send(String topic, String key, Map<String, Object> data) {
//                System.out.println("Mock Kafka: avrei inviato al topic [" + topic + "] con chiave [" + key + "] e dati: " + data);
//                return null;
//            }
//        };
//    }
}