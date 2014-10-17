/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * Base class for the remote clients implementations. Those implementations will be plugged into GeoServer through the Spring app-context.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public abstract class RemoteProcessClient {

    /** Whether this client is enabled or not from configuration */
    private boolean enabled;

    /** Whenever more instances of the client are available, they should be ordered by ascending priority */
    private int priority;
    
    /** The {@link RemoteProcessFactoryConfigurationWatcher} implementation */
    private final RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher;

    /** The registered {@link RemoteProcessFactoryListener} */
    private List<RemoteProcessFactoryListener> remoteFactoryListeners = Collections
            .synchronizedList(new ArrayList<RemoteProcessFactoryListener>());

    /** The registered {@link RemoteProcessClientListener} */
    private List<RemoteProcessClientListener> remoteClientListeners = Collections
            .synchronizedList(new ArrayList<RemoteProcessClientListener>());

    /**
     * The default Cosntructor
     * 
     * @param remoteProcessFactory
     */
    public RemoteProcessClient(
            RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher,
            boolean enabled, int priority) {
        this.remoteProcessFactoryConfigurationWatcher = remoteProcessFactoryConfigurationWatcher;
        this.enabled = enabled;
        this.priority = priority;
    }

    /**
     * @return the {@link RemoteProcessFactoryConfiguration} object
     */
    public RemoteProcessFactoryConfiguration getConfiguration() {
        return this.remoteProcessFactoryConfigurationWatcher.getConfiguration();
    }

    /**
     * Initialization method
     * 
     * @throws Exception
     */
    public abstract void init(SSLContext customSSLContext) throws Exception;

    /**
     * Destroy method
     * 
     * @throws Exception
     */
    public abstract void destroy() throws Exception;

    /**
     * @return the remoteFactoryListeners
     */
    public List<RemoteProcessFactoryListener> getRemoteFactoryListeners() {
        return remoteFactoryListeners;
    }

    /**
     * @return the remoteClientListeners
     */
    public List<RemoteProcessClientListener> getRemoteClientListeners() {
        return remoteClientListeners;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Whether the plugin is enabled or not.
     * 
     * @return
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Registers the {@link RemoteProcessFactoryListener} remoteClientListeners
     * 
     * @param listener
     */
    public void registerProcessFactoryListener(RemoteProcessFactoryListener listener) {
        synchronized (remoteFactoryListeners) {
            remoteFactoryListeners.add(listener);
        }
    }

    /**
     * De-registers the {@link RemoteProcessFactoryListener} remoteClientListeners
     * 
     * @param listener
     */
    public void deregisterProcessFactoryListener(RemoteProcessFactoryListener listener) {
        synchronized (remoteFactoryListeners) {
            remoteFactoryListeners.remove(listener);
        }
    }

    /**
     * Registers the {@link RemoteProcessClientListener} remoteClientListeners
     * 
     * @param listener
     */
    public void registerProcessClientListener(RemoteProcessClientListener listener) {
        synchronized (remoteClientListeners) {
            remoteClientListeners.add(listener);
        }
    }

    /**
     * De-registers the {@link RemoteProcessClientListener} remoteClientListeners
     * 
     * @param listener
     */
    public void deregisterProcessClientListener(RemoteProcessClientListener listener) {
        synchronized (remoteClientListeners) {
            remoteClientListeners.remove(listener);
        }
    }

    /**
     * Invoke the {@link RemoteProcessClient} execution
     * 
     * @param name
     * @param input
     * @param metadata
     * @param monitor
     * @return
     * @throws Exception
     */
    public abstract String execute(Name name, Map<String, Object> input,
            Map<String, Object> metadata, ProgressListener monitor) throws Exception;

}
