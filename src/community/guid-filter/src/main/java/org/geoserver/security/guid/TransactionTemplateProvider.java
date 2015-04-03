/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.exception.GeoServerException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

class TransactionTemplateProvider {

    static final Logger LOGGER = Logging.getLogger(TransactionTemplateProvider.class);

    static final String GUID_PROPERTIES = "guid.properties";

    PropertyFileWatcher watcher;

    DataSource dataSource;

    GuidTransactionTemplate tt;

    public TransactionTemplateProvider(GeoServerDataDirectory dd) {
        Resource resource = dd.get(GUID_PROPERTIES);
        watcher = new PropertyFileWatcher(resource);
    }

    GuidTransactionTemplate getTransactionTemplate() throws GeoServerException {
        // check if the config changed
        checkConfiguration();
        // did we create the transaction template
        if (tt == null) {
            throw new GeoServerException("Could not find the configuration file " + GUID_PROPERTIES);
        }
        return tt;
    }

    private void checkConfiguration() throws GeoServerException {
        if (watcher.isModified()) {
            try {
                Properties properties = watcher.getProperties();
                DataSource dataSource = getDataSource(properties);

                // close the old data source, if it was dynamically setup
                if (this.dataSource != null && this.dataSource instanceof BasicDataSource) {
                    BasicDataSource basic = (BasicDataSource) this.dataSource;
                    basic.close();
                }

                // create the new transaction template
                this.dataSource = dataSource;
                DataSourceTransactionManager dsTransactionManager = new DataSourceTransactionManager(
                        dataSource);
                JdbcTemplate jt = new JdbcTemplate(dsTransactionManager.getDataSource());
                this.tt = new GuidTransactionTemplate(dsTransactionManager, jt);
            } catch (Exception e) {
                throw new GeoServerException("Failed to setup data source", e);
            }
        }

    }

    private DataSource getDataSource(Properties properties) throws GeoServerException {
        Connection c = null;
        DataSource ds = null;
        try {

            if (properties.get("jndi") != null) {
                InitialContext context = new InitialContext();
                ds = (DataSource) context.lookup("jndi");
            } else {
                BasicDataSource bds = new BasicDataSource();
                bds.setDriverClassName((String) properties.get("driver"));
                bds.setUrl((String) properties.get("url"));
                bds.setUsername((String) properties.get("username"));
                bds.setPassword((String) properties.get("password"));
                bds.setPoolPreparedStatements(true);
                bds.setMaxOpenPreparedStatements(Integer.parseInt(getProperty(properties,
                        "maxOpenPreparedStatements", "50")));
                bds.setMinIdle(Integer.parseInt(getProperty(properties, "minConnections", "1")));
                bds.setMaxActive(Integer.parseInt(getProperty(properties, "maxConnections", "10")));
                bds.setMaxWait(Integer.parseInt(getProperty(properties, "connectionTimeout", "10")) * 1000);
                bds.setValidationQuery((String) properties.get("validationQuery"));

                ds = bds;
            }

            // verify the datasource works
            c = ds.getConnection();

            // check we have the table, if not, try creating it
            try (Statement st = c.createStatement()) {
                st.execute("select * from guids");
            } catch(SQLException e) {
                try (Statement st = c.createStatement()) {
                    LOGGER.info("Could not locate the 'guids' table, attempting to create it");
                    st.execute("create table guids (guid varchar(128), user_id varchar(64), layer_name varchar(128), filter varchar(1024))");
                    LOGGER.info("Succesfully added a guids table to the database, adding an index as well");
                    st.execute("create index guids_idx on guids(guid)");
                    LOGGER.info("Search index on guids.guid succesfully added");
                } catch (SQLException e2) {
                    throw new GeoServerException(
                            "guids table is not available, and could not create it", e2);
                }
            }

        } catch (SQLException e) {
            throw new GeoServerException("Failed to get a database connection: " + e.getMessage(),
                    e);
        } catch (NamingException e) {
            throw new GeoServerException("Failed to locate the data source in JNDI", e);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    // nothing we can do about it
                }
            }
        }

        return ds;

    }

    private String getProperty(Properties properties, String key, String defaultValue) {
        String result = (String) properties.get(key);
        if (result == null) {
            return defaultValue;
        } else {
            return result;
        }
    }
}
