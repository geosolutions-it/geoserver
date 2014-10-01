/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.utils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.geoserver.cluster.configuration.EmbeddedBrokerConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.data.util.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertiesPersister;

public class JMSPropertyPlaceholderConfigurer extends
		PropertyPlaceholderConfigurer implements InitializingBean {

	private final JMSConfiguration config;
	private final Resource defaults;

	public JMSPropertyPlaceholderConfigurer(Resource defaultFile,
			JMSConfiguration config) throws IOException {
		if (!defaultFile.exists()) {
			throw new IOException(
					"Unable to locate the default properties file at:"
							+ defaultFile);
		}
		this.defaults = defaultFile;
		this.config = config;
	}

	public Properties[] getProperties() {
		return localProperties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		File properties = new File(config.getConfiguration(EmbeddedBrokerConfiguration.EMBEDDED_BROKER_PROPERTIES_KEY).toString());
		if (!properties.isAbsolute() && !properties.isFile()) {
			// try to resolve as absolute
			properties = new File(JMSConfiguration.getConfigPathDir(),properties.getPath());
			if (!properties.isFile()) {
				// copy the defaults
				IOUtils.copy(defaults.getFile(), properties);
			}
		}
		final Resource res = new FileSystemResource(properties);
		super.setLocation(res);
		
		// make sure the activemq.base is set to a valuable default 
		final Properties props=new Properties();
		props.setProperty("activemq.base", (String)config.getConfiguration("CLUSTER_CONFIG_DIR"));
		props.setProperty("instanceName", (String)config.getConfiguration("instanceName"));
		setProperties(props);
	}

    @Override
    protected String resolvePlaceholder(String placeholder, Properties props,
            int systemPropertiesMode) {
        // TODO Auto-generated method stub
        return super.resolvePlaceholder(placeholder, props, systemPropertiesMode);
    }

    @Override
    protected String resolvePlaceholder(String placeholder, Properties props) {
        // TODO Auto-generated method stub
        return super.resolvePlaceholder(placeholder, props);
    }

    @Override
    protected String resolveSystemProperty(String key) {
        // TODO Auto-generated method stub
        return super.resolveSystemProperty(key);
    }

    @Override
    protected void convertProperties(Properties props) {
        // TODO Auto-generated method stub
        super.convertProperties(props);
    }

    @Override
    protected String convertProperty(String propertyName, String propertyValue) {
        // TODO Auto-generated method stub
        return super.convertProperty(propertyName, propertyValue);
    }

    @Override
    protected String convertPropertyValue(String originalValue) {
        // TODO Auto-generated method stub
        return super.convertPropertyValue(originalValue);
    }

    @Override
    protected void loadProperties(Properties props) throws IOException {
        // TODO Auto-generated method stub
        super.loadProperties(props);
    }

    @Override
    protected Properties mergeProperties() throws IOException {
        // TODO Auto-generated method stub
        return super.mergeProperties();
    }

    @Override
    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub
        super.setProperties(properties);
    }

    @Override
    public void setPropertiesArray(Properties[] propertiesArray) {
        // TODO Auto-generated method stub
        super.setPropertiesArray(propertiesArray);
    }

    @Override
    public void setPropertiesPersister(PropertiesPersister propertiesPersister) {
        // TODO Auto-generated method stub
        super.setPropertiesPersister(propertiesPersister);
    }

   
}
