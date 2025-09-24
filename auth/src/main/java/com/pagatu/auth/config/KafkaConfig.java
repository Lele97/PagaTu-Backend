package com.pagatu.auth.config;

import com.pagatu.auth.event.ResetPasswordMailEvent;
import com.pagatu.auth.event.TokenForgotPswUserEvent;
import com.pagatu.auth.event.UserEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for setting up Kafka producers and consumers.
 * Defines beans for producer factories, consumer factories, and Kafka templates
 * for different event types used in the authentication service.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates base configuration properties for Kafka producers.
     * Configures bootstrap servers and serializers for keys and values.
     *
     * @return Map of producer configuration properties
     */
    private Map<String, Object> getProducerConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }

    /**
     * Creates base configuration properties for Kafka consumers.
     * Configures bootstrap servers, deserializers, and consumer group ID.
     *
     * @param deserializer the JsonDeserializer to use for value deserialization
     * @return Map of consumer configuration properties
     */
    private Map<String, Object> getConsumerConfigProps(JsonDeserializer<?> deserializer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-service");
        return props;
    }

    /**
     * Creates a producer factory for ResetPasswordMailEvent messages.
     *
     * @return ProducerFactory configured for ResetPasswordMailEvent
     */
    @Bean
    public ProducerFactory<String, ResetPasswordMailEvent> producerFactoryResetPswMail() {
        return new DefaultKafkaProducerFactory<>(getProducerConfigProps());
    }

    /**
     * Creates a consumer factory for ResetPasswordMailEvent messages.
     *
     * @return ConsumerFactory configured for ResetPasswordMailEvent
     */
    @Bean
    public ConsumerFactory<String, ResetPasswordMailEvent> consumerFactoryResetPswMail() {
        JsonDeserializer<ResetPasswordMailEvent> deserializer = new JsonDeserializer<>(ResetPasswordMailEvent.class);
        return new DefaultKafkaConsumerFactory<>(
                getConsumerConfigProps(deserializer),
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Creates a producer factory for UserEvent messages.
     *
     * @return ProducerFactory configured for UserEvent
     */
    @Bean
    public ProducerFactory<String, UserEvent> producerFactoryUserEvent() {
        return new DefaultKafkaProducerFactory<>(getProducerConfigProps());
    }

    /**
     * Creates a consumer factory for UserEvent messages.
     *
     * @return ConsumerFactory configured for UserEvent
     */
    @Bean
    public ConsumerFactory<String, UserEvent> consumerFactoryUserEvent() {
        JsonDeserializer<UserEvent> deserializer = new JsonDeserializer<>(UserEvent.class);
        return new DefaultKafkaConsumerFactory<>(
                getConsumerConfigProps(deserializer),
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Creates a producer factory for TokenForgotPswUserEvent messages.
     *
     * @return ProducerFactory configured for TokenForgotPswUserEvent
     */
    @Bean
    public ProducerFactory<String, TokenForgotPswUserEvent> producerFactoryTokenForgotUserPassword() {
        return new DefaultKafkaProducerFactory<>(getProducerConfigProps());
    }

    /**
     * Creates a consumer factory for TokenForgotPswUserEvent messages.
     *
     * @return ConsumerFactory configured for TokenForgotPswUserEvent
     */
    @Bean
    public ConsumerFactory<String, TokenForgotPswUserEvent> consumerFactoryTokenForgotUserPassword() {
        JsonDeserializer<TokenForgotPswUserEvent> deserializer = new JsonDeserializer<>(TokenForgotPswUserEvent.class);
        return new DefaultKafkaConsumerFactory<>(
                getConsumerConfigProps(deserializer),
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * Creates a Kafka template for sending UserEvent messages.
     *
     * @return KafkaTemplate configured for UserEvent
     */
    @Bean
    public KafkaTemplate<String, UserEvent> userEventKafkaTemplate() {
        return new KafkaTemplate<>(producerFactoryUserEvent());
    }

    /**
     * Creates a Kafka template for sending ResetPasswordMailEvent messages.
     *
     * @return KafkaTemplate configured for ResetPasswordMailEvent
     */
    @Bean
    public KafkaTemplate<String, ResetPasswordMailEvent> resetPasswordMailKafkaTemplate() {
        return new KafkaTemplate<>(producerFactoryResetPswMail());
    }

    /**
     * Creates a Kafka template for sending TokenForgotPswUserEvent messages.
     *
     * @return KafkaTemplate configured for TokenForgotPswUserEvent
     */
    @Bean
    public KafkaTemplate<String, TokenForgotPswUserEvent> tokenForgotUserPasswordKafkaTemplate() {
        return new KafkaTemplate<>(producerFactoryTokenForgotUserPassword());
    }
}