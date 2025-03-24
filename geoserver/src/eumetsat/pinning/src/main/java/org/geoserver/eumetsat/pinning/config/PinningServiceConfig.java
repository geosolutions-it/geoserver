package org.geoserver.eumetsat.pinning.config;

import java.io.Serializable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PinningServiceConfig implements Serializable, Cloneable {

    @Value("${batch.size}")
    private String batchSize;

    @Value("${pinning.minutes}")
    private String pinningMinutes;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${jndi.datasource.name}")
    private String jndiDatasourceName

    public String apiUrl() {
        return apiUrl;
    }

    public String batchSize() {
        return batchSize;
    }

    public String pinningMinutes() {
        return pinningMinutes;
    }

    @Bean
    public DataSource dataSource() throws NamingException {
        Context ctx = new InitialContext();
        // Search for the eumetsat datasource configured using JNDI
        DataSource dataSource = (DataSource) ctx.lookup("java:/comp/env/jndi/" + jndiDatasourceName);
        return dataSource;
    }
}
