package com.pagatu.auth.config;

import com.pagatu.auth.repository.DevRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@Profile("dev")
public class DevDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.dev-datasource")
    public DataSourceProperties devDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "devDataSource")
    public DataSource devDataSource() {
        return devDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    // Create the main EntityManagerFactory with multiple names (aliases)
    @Primary
    @Bean(name = {"devEntityManagerFactory", "firstEntityManagerFactory", "secondEntityManagerFactory"})
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("devDataSource") DataSource dataSource,
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.pagatu.auth")
                .persistenceUnit("dev")
                .build();
    }

    // Create the main TransactionManager with multiple names (aliases)
    @Primary
    @Bean(name = {"devTransactionManager", "firstTransactionManager", "secondTransactionManager"})
    public PlatformTransactionManager transactionManager(
            @Qualifier("devEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.pagatu.auth",
            includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION,
                    classes = DevRepository.class),
            entityManagerFactoryRef = "devEntityManagerFactory",
            transactionManagerRef = "devTransactionManager"
    )
    static class DevJpaRepositoriesConfig {
    }
}