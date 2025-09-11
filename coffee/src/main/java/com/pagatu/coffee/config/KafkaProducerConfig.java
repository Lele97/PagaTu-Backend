package com.pagatu.coffee.config;

import com.pagatu.coffee.event.InvitaionEvent;
import com.pagatu.coffee.event.ProssimoPagamentoEvent;
import com.pagatu.coffee.event.SaltaPagamentoEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     *
     * @return
     */
    @Bean
    public ProducerFactory<String, ProssimoPagamentoEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     *
     * @return
     */
    @Bean
    public KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     *
     * @return
     */
    @Bean
    public ProducerFactory<String, InvitaionEvent> producerFactory_invitation() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     *
     * @return
     */
    @Bean
    public KafkaTemplate<String, InvitaionEvent> kafkaTemplate_invitation(){
        return new KafkaTemplate<>(producerFactory_invitation());
    }

    /**
     *
     * @return
     */
    @Bean
    public ProducerFactory<String, SaltaPagamentoEvent> producerFactory_salta_pagamenti() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     *
     * @return
     */
    @Bean
    public KafkaTemplate<String, SaltaPagamentoEvent> kafkaTemplate_salta_pagamenti() {
        return new KafkaTemplate<>(producerFactory_salta_pagamenti());
    }
}
