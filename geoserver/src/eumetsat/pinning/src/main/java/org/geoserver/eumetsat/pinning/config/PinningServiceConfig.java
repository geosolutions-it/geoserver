/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
    private Integer batchSize;

    @Value("${pinning.minutes}")
    private Integer pinningMinutes;

    @Value("${preferences.url}")
    private String preferencesUrl;

    @Value("${jndi.datasource.name}")
    private String jndiDatasourceName;

    public String preferencesUrl() {
        return preferencesUrl;
    }

    public Integer batchSize() {
        return batchSize;
    }

    public Integer pinningMinutes() {
        return pinningMinutes;
    }

    @Bean
    public DataSource dataSource() throws NamingException {
        Context ctx = new InitialContext();
        // Search for the eumetsat datasource configured using JNDI
        DataSource dataSource =
                (DataSource) ctx.lookup("java:/comp/env/jdbc/" + jndiDatasourceName);
        return dataSource;
    }
}
