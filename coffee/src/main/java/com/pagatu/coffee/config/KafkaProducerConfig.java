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
 * Configuration class for Kafka producers in the coffee payment system.
 * <p>
 * This configuration sets up Kafka producers for different event types:
 * <ul>
 *   <li>ProssimoPagamentoEvent - for payment notifications</li>
 *   <li>InvitaionEvent - for group invitation notifications</li>
 *   <li>SaltaPagamentoEvent - for payment skip notifications</li>
 * </ul>
 * </p>
 * <p>
 * Each producer is configured with JSON serialization for event objects
 * and string serialization for keys, enabling event-driven communication
 * with other microservices.
 * </p>
 */
@Configuration
public class KafkaProducerConfig {

    /**
     * Kafka bootstrap servers configuration from application properties.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates a producer factory for payment event messages.
     * <p>
     * Configures the producer with JSON serialization for ProssimoPagamentoEvent
     * objects and string serialization for message keys.
     * </p>
     *
     * @return ProducerFactory configured for payment events
     */
    @Bean
    public ProducerFactory<String, ProssimoPagamentoEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(getConfigProps());
    }

    /**
     * Creates a Kafka template for sending payment event messages.
     * <p>
     * This template is used by services to publish payment-related events
     * to Kafka topics for downstream processing.
     * </p>
     *
     * @return KafkaTemplate configured for payment events
     */
    @Bean
    public KafkaTemplate<String, ProssimoPagamentoEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Creates a producer factory for group invitation event messages.
     * <p>
     * Configures the producer with JSON serialization for InvitaionEvent
     * objects, used when users invite others to join groups.
     * </p>
     *
     * @return ProducerFactory configured for invitation events
     */
    @Bean
    public ProducerFactory<String, InvitaionEvent> producerFactoryInvitation() {
        return new DefaultKafkaProducerFactory<>(getConfigProps());
    }

    /**
     * Creates a Kafka template for sending group invitation messages.
     * <p>
     * This template is used to publish invitation events when users
     * are invited to join coffee payment groups.
     * </p>
     *
     * @return KafkaTemplate configured for invitation events
     */
    @Bean
    public KafkaTemplate<String, InvitaionEvent> kafkaTemplateInvitation(){
        return new KafkaTemplate<>(producerFactoryInvitation());
    }

    /**
     * Creates a producer factory for payment skip event messages.
     * <p>
     * Configures the producer with JSON serialization for SaltaPagamentoEvent
     * objects, used when users skip their payment turns.
     * </p>
     *
     * @return ProducerFactory configured for payment skip events
     */
    @Bean
    public ProducerFactory<String, SaltaPagamentoEvent> producerFactorySaltaPagamenti() {
        return new DefaultKafkaProducerFactory<>(getConfigProps());
    }

    /**
     * Creates a Kafka template for sending payment skip messages.
     * <p>
     * This template is used to publish events when users skip their
     * turn to pay in the coffee payment rotation.
     * </p>
     *
     * @return KafkaTemplate configured for payment skip events
     */
    @Bean
    public KafkaTemplate<String, SaltaPagamentoEvent> kafkaTemplateSaltaPagamenti() {
        return new KafkaTemplate<>(producerFactorySaltaPagamenti());
    }

    /**
     * Creates the base configuration properties for all Kafka producers.
     *
     * @return Map containing the base Kafka producer configuration
     */
    public Map<String, Object> getConfigProps(){
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }
}
