package org.geoserver.eumetsat.pinning.config;

import java.io.Serializable;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class PinningServiceConfig implements Serializable, Cloneable {

    @Value("${batch.size}")
    private String batchSize;

    @Value("${pinning.minutes}")
    private String pinningMinutes;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${jdbc.url}")
    private String jdbcUrl;

    @Value("${jdbc.username}")
    private String jdbcUsername;

    @Value("${jdbc.password}")
    private String jdbcPassword;

    @Bean
    public String apiUrl() {
        return "http://localhost:8080/userPreferences/preferences/";
        // return apiUrl; // Injected into Services
    }

    @Bean
    public String batchSize() {
        // return batchSize
        return "50";
    }

    @Bean
    public String pinningMinutes() {
        // return pinningMinutes;
        return "300";
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        //TODO: NEED to parse these props
        dataSource.setDriverClassName("org.postgresql.Driver");
        /*dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);*/
        dataSource.setUrl("jdbc:postgresql://localhost:5432/pinning");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        return dataSource;
    }
}
