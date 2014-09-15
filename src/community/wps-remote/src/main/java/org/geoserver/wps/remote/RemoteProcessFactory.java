/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.awt.RenderingHints.Key;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.text.Text;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * A process factory that wraps a Remote Client and can be used to get information about it and create the corresponding process
 * 
 * @author alessio.fabiani
 * 
 */
public class RemoteProcessFactory implements ProcessFactory, RemoteProcessClientListener {

    public static final Logger LOGGER = Logging.getLogger(RemoteProcessFactory.class.getPackage()
            .getName());

    private Set<Name> names = Collections.synchronizedSet(new HashSet<Name>());

    private Map<Name, RemoteServiceDescriptor> descriptors = Collections
            .synchronizedMap(new HashMap<Name, RemoteServiceDescriptor>());

    private Map<Name, RemoteProcess> remoteInstances = Collections
            .synchronizedMap(new HashMap<Name, RemoteProcess>());
    
    private RemoteProcessClient remoteClient;

    /**
     * Constructs a process factory able to create stubs for the remote communication
     */
    public RemoteProcessFactory() {
        try {
            for (RemoteProcessClient ext : GeoServerExtensions
                    .extensions(RemoteProcessClient.class)) {
                if (ext.isEnabled()) {
                    remoteClient = ext;
                    remoteClient.init();
                    remoteClient.registerListener(this);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public InternationalString getTitle() {
        return new SimpleInternationalString("Remote");
    }

    public Set<Name> getNames() {
        return names;
    }

    boolean checkName(Name name) {
        if (name == null)
            throw new NullPointerException("Process name cannot be null");
        if (!names.contains(name)) {
            LOGGER.warning("Unknown process '" + name + "'");
            return false;
        }

        return true;
    }

    /**
     * Creates a Remote Process and registers it as listener
     * 
     * @throws IllegalArgumentException
     */
    public Process create(Name name) throws IllegalArgumentException {
        if ( checkName(name) ) {
            try {
                RemoteProcess process = new RemoteProcess(name, remoteClient, descriptors.get(name).getMetadata());
                remoteInstances.put(name, process);
                return process;
            } catch (Exception e) {
                throw new RuntimeException("Error occurred cloning the prototype "
                        + "algorithm... this should not happen", e);
            }
        }
        return null;
    }

    public InternationalString getDescription(Name name) {
        if ( checkName(name) )
            return Text.text(descriptors.get(name).getDescription());
        return null;
    }

    public InternationalString getTitle(Name name) {
        if ( checkName(name) )
            return Text.text(descriptors.get(name).getTitle());
        return null;
    }

    public String getName(Name name) {
        if ( checkName(name) )
            return name.getLocalPart();
        return null;
    }

    public boolean supportsProgress(Name name) {
        return true;
    }

    public String getVersion(Name name) {
        checkName(name);
        return "1.0.0";
    }

    public Map<String, Parameter<?>> getParameterInfo(Name name) {
        if ( checkName(name) )
            return descriptors.get(name).getParamInfo();
        return null;
    }

    public Map<String, Parameter<?>> getResultInfo(Name name, Map<String, Object> inputs)
            throws IllegalArgumentException {
        if ( checkName(name) )
            return descriptors.get(name).getOutputInfo();
        return null;
    }

    @Override
    public String toString() {
        return "RemoteProcessFactory";
    }

    public boolean isAvailable() {
        return true;
    }

    public Map<Key, ?> getImplementationHints() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public void registerService(Name name, String title, String description,
            Map<String, Parameter<?>> paramInfo, Map<String, Parameter<?>> outputInfo,
            Map<String, Object> metadata) {
        names.add(name);
        descriptors.put(name, new RemoteServiceDescriptor(name, title, description, paramInfo, outputInfo, metadata));
        create(name);
        System.out.println("Registered Service [" + name + "]");
    }

    @Override
    public void deregisterService(Name name) {
        if ( checkName(name) ) {
            names.remove(name);
            descriptors.remove(name);
            remoteInstances.remove(name);
            System.out.println("Deregistered Service [" + name + "]");
        }
    }

    @Override
    public void progress(final String executionId, final Double progress) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void complete(String pId, Object outputs) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void exceptionOccurred(String executionId, Exception cause, Map<String, Object> metadata) {
        // TODO Auto-generated method stub
        
    }
}
