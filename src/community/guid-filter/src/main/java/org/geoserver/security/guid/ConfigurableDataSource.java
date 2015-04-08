/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.logging.Logging;

/**
 * A data source delegating to one dynamically configured watching a
 * <code>$GEOSERVER_DATA_DIR/guid.properties</code> config file
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class ConfigurableDataSource implements DataSource {

    DataSource delegate;

    static final Logger LOGGER = Logging.getLogger(ConfigurableDataSource.class);

    static final String GUID_PROPERTIES = "guid.properties";

    static final String GUID_PROPERTIES_SAMPLE = "guid.properties.sample";

    PropertyFileWatcher watcher;

    public ConfigurableDataSource(GeoServerDataDirectory dd) throws IOException {
        Resource resource = dd.get(GUID_PROPERTIES);
        if(resource.getType() == Type.UNDEFINED) {
            Resource sample = dd.get(GUID_PROPERTIES_SAMPLE);
            if (sample.getType() == Type.UNDEFINED) {
                LOGGER.warning("Could not locate " + resource.path()
                        + ", creating a sample file at " + sample.path());
                try (InputStream is = getClass().getResourceAsStream(GUID_PROPERTIES);
                        OutputStream os = sample.out()) {
                    IOUtils.copy(is, os);
                }
            }
        }
        watcher = new PropertyFileWatcher(resource);
    }

    public Connection getConnection() throws SQLException {
        checkConfiguration();
        return delegate.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        checkConfiguration();
        return delegate.getConnection(username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
        checkConfiguration();
        return delegate.getLogWriter();
    }

    public int getLoginTimeout() throws SQLException {
        checkConfiguration();
        return delegate.getLoginTimeout();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        checkConfiguration();
        return delegate.isWrapperFor(arg0);
    }

    public void setLogWriter(PrintWriter arg0) throws SQLException {
        checkConfiguration();
        delegate.setLogWriter(arg0);
    }

    public void setLoginTimeout(int arg0) throws SQLException {
        checkConfiguration();
        delegate.setLoginTimeout(arg0);
    }

    public <T> T unwrap(Class<T> arg0) throws SQLException {
        checkConfiguration();
        return delegate.unwrap(arg0);
    }

    private void checkConfiguration() throws SQLException {
        if (watcher.isModified()) {
            try {
                Properties properties = watcher.getProperties();
                DataSource dataSource = getDataSource(properties);

                // close the old data source, if it was dynamically setup
                if (this.delegate != null && this.delegate instanceof BasicDataSource) {
                    BasicDataSource basic = (BasicDataSource) this.delegate;
                    basic.close();
                }

                // create the new transaction template
                this.delegate = dataSource;
            } catch (Exception e) {
                throw new SQLException("Failed to setup data source", e);
            }
        } else if (delegate == null) {
            throw new SQLException(
                    "Could not locate the configuraton file, "
                    + "add a guid.properties one in the data directory "
                    + "(you'll find a guid.properties.sample one to start from");
        }

    }

    private DataSource getDataSource(Properties properties) throws SQLException {
        DataSource ds = null;

        if (properties.get("jndi") != null) {
            try {
                InitialContext context = new InitialContext();
                ds = (DataSource) context.lookup(properties.getProperty("jndi"));
            } catch (NamingException e) {
                throw new SQLException("Failed to locate the data source in JNDI", e);
            }
        } else {
            BasicDataSource bds = new BasicDataSource();
            bds.setDriverClassName(properties.getProperty("driver"));
            bds.setUrl(properties.getProperty("url"));
            bds.setUsername(properties.getProperty("username"));
            bds.setPassword(properties.getProperty("password"));
            bds.setPoolPreparedStatements(true);
            bds.setMaxOpenPreparedStatements(Integer.parseInt(getProperty(properties,
                    "maxOpenPreparedStatements", "50")));
            bds.setMinIdle(Integer.parseInt(getProperty(properties, "minConnections", "1")));
            bds.setMaxActive(Integer.parseInt(getProperty(properties, "maxConnections", "10")));
            bds.setMaxWait(Integer.parseInt(getProperty(properties, "connectionTimeout", "10")) * 1000);
            bds.setValidationQuery(properties.getProperty("validationQuery"));

            ds = bds;
        }

        // verify the datasource works
        try (Connection c = ds.getConnection()) {

            // check we have the table, if not, try creating it
            try (Statement st = c.createStatement()) {
                st.execute("select * from guids");
            } catch (SQLException e) {
                try (Statement st = c.createStatement()) {
                    LOGGER.info("Could not locate the 'guids' table, attempting to create it");
                    st.execute("create table guids (guid varchar(128), user_id varchar(64), layer_name varchar(128), filter varchar(1024))");
                    LOGGER.info("Succesfully added a guids table to the database, adding an index as well");
                    st.execute("create index guids_idx on guids(guid)");
                    LOGGER.info("Search index on guids.guid succesfully added");
                }
            }

        }

        return ds;

    }

    private String getProperty(Properties properties, String key, String defaultValue) {
        String result = properties.getProperty(key);
        if (result == null) {
            return defaultValue;
        } else {
            return result;
        }
    }

}
