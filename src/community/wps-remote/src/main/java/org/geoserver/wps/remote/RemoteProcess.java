/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.Map;

import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * 
 * @author alessio.fabiani
 * 
 */
public class RemoteProcess implements Process, RemoteProcessClientListener {

    private Name name;

    private RemoteProcessClient remoteClient;

    private Map<String, Object> metadata;

    private Map<String, Object> outputs;

    private boolean running;

    private String pid;

    private ProgressListener listener;
    
    /**
     * Constructs a new stup for the Remote Process Execution
     * 
     * @param name
     * @param remoteClient
     * @param metadata
     */
    public RemoteProcess(Name name, RemoteProcessClient remoteClient, Map<String, Object> metadata) {
        this.name = name;
        this.remoteClient = remoteClient;
        this.metadata = metadata;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor) {

        try {
            // Generate a unique Process ID
            pid = remoteClient.execute(name, input, metadata, monitor);
            listener = monitor;
            running = pid != null;
            if (running) {
                remoteClient.registerListener(this);
                while (running && outputs == null) {
                    Thread.sleep(100);
                }
            }

            return outputs;
        } catch (Exception e) {
            monitor.exceptionOccurred(e);
            throw new ProcessException(e);
        } finally {
            remoteClient.deregisterListener(this);
        }

    }

    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param running the running to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * @return the outputs
     */
    public Map<String, Object> getOutputs() {
        return outputs;
    }

    /**
     * @param outputs the outputs to set
     */
    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }

    @Override
    public void registerService(Name name, String title, String description,
            Map<String, Parameter<?>> paramInfo, Map<String, Parameter<?>> outputInfo,
            Map<String, Object> metadata) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deregisterService(Name name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void progress(final String pId, final Double progress) {
        if (pId.equals(pid)) {
            listener.progress(progress.floatValue());
        }
    }

    @Override
    public void complete(String pId, Object outputs) {
        if (pId.equals(pid)) {
            listener.complete();
            this.outputs = (Map<String, Object>) outputs;
            running = false;
        }        
    }

    @Override
    public void exceptionOccurred(final String pId, Exception cause, Map<String, Object> metadata) {
        // TODO Auto-generated method stub

    }

}
