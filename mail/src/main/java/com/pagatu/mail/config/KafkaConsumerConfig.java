package com.pagatu.mail.config;

import com.pagatu.mail.event.InvitationEvent;
import com.pagatu.mail.event.ProssimoPagamentoEvent;
import com.pagatu.mail.event.ResetPasswordMailEvent;
import com.pagatu.mail.event.SaltaPagamentoEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

import static com.pagatu.mail.util.Constants.*;

/**
 * Configuration class for setting up Kafka consumers for various email notification events.
 * Provides consumer factories and listener container factories for different event types
 * including payment notifications, invitations, and password reset events.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates a consumer factory for ProssimoPagamentoEvent messages.
     * Configures Kafka consumer properties including bootstrap servers, group ID,
     * and offset reset policy. Uses JSON deserializer for message values.
     *
     * @return ConsumerFactory configured for ProssimoPagamentoEvent messages
     */
    @Bean
    public ConsumerFactory<String, ProssimoPagamentoEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_EARLIEST);

        JsonDeserializer<ProssimoPagamentoEvent> deserializer = new JsonDeserializer<>(ProssimoPagamentoEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages(TRUSTED_PACKAGE_COFFEE_EVENT);
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    /**
     * Creates a Kafka listener container factory for ProssimoPagamentoEvent messages.
     * Uses the consumerFactory bean for consumer configuration.
     *
     * @return ConcurrentKafkaListenerContainerFactory for ProssimoPagamentoEvent messages
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProssimoPagamentoEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProssimoPagamentoEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    /**
     * Creates a consumer factory for InvitationEvent messages.
     * Configures Kafka consumer properties including bootstrap servers, group ID,
     * and offset reset policy. Uses JSON deserializer for message values.
     *
     * @return ConsumerFactory configured for InvitationEvent messages
     */
    @Bean
    public ConsumerFactory<String, InvitationEvent> consumerFactoryInvitation() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_EARLIEST);

        JsonDeserializer<InvitationEvent> deserializer = new JsonDeserializer<>(InvitationEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages(TRUSTED_PACKAGE_COFFEE_EVENT);
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    /**
     * Creates a Kafka listener container factory for InvitationEvent messages.
     * Uses the consumerFactoryInvitation bean for consumer configuration.
     *
     * @return ConcurrentKafkaListenerContainerFactory for InvitationEvent messages
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InvitationEvent> kafkaListenerContainerFactoryInvitation() {
        ConcurrentKafkaListenerContainerFactory<String, InvitationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryInvitation());
        return factory;
    }

    /**
     * Creates a consumer factory for SaltaPagamentoEvent messages.
     * Configures Kafka consumer properties including bootstrap servers, group ID,
     * and offset reset policy. Uses JSON deserializer for message values.
     *
     * @return ConsumerFactory configured for SaltaPagamentoEvent messages
     */
    @Bean
    public ConsumerFactory<String, SaltaPagamentoEvent> consumerFactorySaltaPagamento() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_EARLIEST);

        JsonDeserializer<SaltaPagamentoEvent> deserializer = new JsonDeserializer<>(SaltaPagamentoEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages(TRUSTED_PACKAGE_COFFEE_EVENT);
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    /**
     * Creates a Kafka listener container factory for SaltaPagamentoEvent messages.
     * Uses the consumerFactorySaltaPagamento bean for consumer configuration.
     *
     * @return ConcurrentKafkaListenerContainerFactory for SaltaPagamentoEvent messages
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SaltaPagamentoEvent> kafkaListenerContainerFactorySaltaPagamento() {
        ConcurrentKafkaListenerContainerFactory<String, SaltaPagamentoEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactorySaltaPagamento());
        return factory;
    }

    /**
     * Creates a consumer factory for ResetPasswordMailEvent messages.
     * Configures Kafka consumer properties including bootstrap servers, group ID,
     * and offset reset policy. Uses JSON deserializer for message values.
     * Uses a different trusted package (TRUSTED_PACKAGE_AUTH_EVENT) than other events.
     *
     * @return ConsumerFactory configured for ResetPasswordMailEvent messages
     */
    @Bean
    public ConsumerFactory<String, ResetPasswordMailEvent> consumerFactoryResetPswMail() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KAFKA_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET_EARLIEST);

        JsonDeserializer<ResetPasswordMailEvent> deserializer = new JsonDeserializer<>(ResetPasswordMailEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages(TRUSTED_PACKAGE_AUTH_EVENT);
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    /**
     * Creates a Kafka listener container factory for ResetPasswordMailEvent messages.
     * Uses the consumerFactoryResetPswMail bean for consumer configuration.
     *
     * @return ConcurrentKafkaListenerContainerFactory for ResetPasswordMailEvent messages
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ResetPasswordMailEvent> kafkaListenerContainerFactoryResetPswMail() {
        ConcurrentKafkaListenerContainerFactory<String, ResetPasswordMailEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryResetPswMail());
        return factory;
    }
}