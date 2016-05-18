/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resources.AnyFilter;
import org.geoserver.util.Filter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.thoughtworks.xstream.XStream;

/**
 * TODO
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public abstract class AbstractCatalogBackupRestoreTasklet implements Tasklet, InitializingBean {

    /*
     * 
     */
    protected static Map<String, Filter<Resource>> resources = new HashMap<String, Filter<Resource>>();

    /*
     * 
     */
    static {
        resources.put("demo", AnyFilter.INSTANCE);
        resources.put("images", AnyFilter.INSTANCE);
        resources.put("logs", new Filter<Resource>() {

            @Override
            public boolean accept(Resource res) {
                if (res.name().endsWith(".properties")) {
                    return true;
                }
                return false;
            }
            
        });
        resources.put("palettes", AnyFilter.INSTANCE);
        resources.put("plugIns", AnyFilter.INSTANCE);
        resources.put("styles", new Filter<Resource>() {

            @Override
            public boolean accept(Resource res) {
                if (res.name().endsWith(".sld") || res.name().endsWith(".xml")) {
                    return false;
                }
                return true;
            }
            
        });
        resources.put("user_projections", AnyFilter.INSTANCE);
        resources.put("validation", AnyFilter.INSTANCE);
        resources.put("www", AnyFilter.INSTANCE);
    }
    
    protected Backup backupFacade;

    protected Catalog catalog;

    protected XStreamPersisterFactory xStreamPersisterFactory;

    protected XStreamPersister xstream;

    protected XStream xp;

    private boolean restoringCatalog;

    public AbstractCatalogBackupRestoreTasklet(Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        this.backupFacade = backupFacade;
        
        this.xStreamPersisterFactory = xStreamPersisterFactory;
        this.xstream = xStreamPersisterFactory.createXMLPersister();
    }
    
    /**
     * @return the restoringCatalog
     */
    public boolean isRestoringCatalog() {
        return restoringCatalog;
    }

    /**
     * @param restoringCatalog the restoringCatalog to set
     */
    public void setRestoringCatalog(boolean restoringCatalog) {
        this.restoringCatalog = restoringCatalog;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) 
            throws Exception {
        // Accordingly to the running execution type (Backup or Restore) we
        // need to validate resources against the official GeoServer Catalog (Backup)
        // or the temporary one (Restore).
        //
        // For restore operations the order matters.
        JobExecution jobExecution = chunkContext.getStepContext().getStepExecution()
                .getJobExecution();
        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {
            this.catalog = backupFacade.getRestoreExecutions().get(jobExecution.getId())
                    .getRestoreCatalog();
            this.restoringCatalog = true;
        } else {
            this.catalog = backupFacade.getCatalog();
            this.restoringCatalog = false;
            this.xstream.setExcludeIds();
        }

        Assert.notNull(catalog, "catalog must be set");

        this.xstream.setCatalog(this.catalog);
        this.xstream.setReferenceByName(true);
        this.xp = this.xstream.getXStream();

        return doExecute(contribution, chunkContext, jobExecution);
    }
    
    /**
     * 
     * @param contribution
     * @param chunkContext
     * @param jobExecution 
     * @return
     * @throws Exception
     */
    abstract RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception;

    /**
     * @param targetBackupFolder
     * @throws IOException
     */
    public void backupGWCSettings(Resource targetBackupFolder) throws IOException {
        GWCConfigPersister gwcGeoServerConfigPersister = 
                (GWCConfigPersister) GeoServerExtensions.bean("gwcGeoServervConfigPersister");
        
        GWCConfigPersister testGWCCP = 
                new GWCConfigPersister(xStreamPersisterFactory, new GeoServerResourceLoader(targetBackupFolder.dir()));
        
        testGWCCP.save(gwcGeoServerConfigPersister.getConfig());
        
        // Test that everything went well
        try {
            GWCConfig gwcConfig = testGWCCP.getConfig();
            
            Assert.notNull(gwcConfig);
            
            // TODO: perform more tests and integrity checks on reloaded configuration
            
            
            // Store GWC Providers Configurations
            Resource targetGWCProviderBackupDir = 
                    BackupUtils.dir(targetBackupFolder, GeoserverXMLResourceProvider.DEFAULT_CONFIGURATION_DIR_NAME);

            for(GeoserverXMLResourceProvider gwcProvider : GeoServerExtensions.extensions(GeoserverXMLResourceProvider.class)) {
                Resource providerConfigFile = Resources.fromPath(gwcProvider.getLocation());
                if (Resources.exists(providerConfigFile) && FileUtils.sizeOf(providerConfigFile.file()) > 0) {
                    Resources.copy(gwcProvider.in(), targetGWCProviderBackupDir, providerConfigFile.name());
                }
            }
        } catch (Exception e) {
            // TODO: collect warnings and errors
        }
    }

    /**
     * TODO: When Restoring
     * 
     * 1. the securityManager should issue the listeners
     * 2. the GWCInitializer  should be re-initialized
     * 
     * @param sourceRestoreFolder
     * @param baseDir
     * @throws IOException
     */
    public void restoreGWCSettings(Resource sourceRestoreFolder, Resource baseDir) throws IOException {
        // Restore configuration files form source and Test that everything went well
        try {
            // - Prepare folder
            Files.delete(baseDir.get(GeoserverXMLResourceProvider.DEFAULT_CONFIGURATION_DIR_NAME).dir());
            
            // Store GWC Providers Configurations
            Resource targetGWCProviderRestoreDir = 
                    BackupUtils.dir(baseDir, GeoserverXMLResourceProvider.DEFAULT_CONFIGURATION_DIR_NAME);
            
            for(GeoserverXMLResourceProvider gwcProvider : GeoServerExtensions.extensions(GeoserverXMLResourceProvider.class)) {
                final File gwcProviderConfigFile = new File(gwcProvider.getLocation());
                Resource providerConfigFile = sourceRestoreFolder.get(Paths.path(gwcProviderConfigFile.getParent(), gwcProviderConfigFile.getName()));
                if (Resources.exists(providerConfigFile) && FileUtils.sizeOf(gwcProviderConfigFile) > 0) {
                    Resources.copy(providerConfigFile.in(), targetGWCProviderRestoreDir, providerConfigFile.name());
                }
            }
        } catch (Exception e) {
            // TODO: collect warnings and errors
        }
    }
    
    /**
     * @param resourceStore
     * @param baseDir 
     * @throws IOException 
     */
    public void backupAdditionalResources(ResourceStore resourceStore, Resource baseDir) throws IOException {
        for (Entry<String, Filter<Resource>> entry : resources.entrySet()){
            Resource resource = resourceStore.get(entry.getKey());
            if (resource != null && Resources.exists(resource)) {
                
                List<Resource> resources = Resources.list(resource, entry.getValue(), false);
                
                Resource targetDir = BackupUtils.dir(baseDir, resource.name());
                for (Resource res : resources) {
                    if (res.getType() != Type.DIRECTORY) {
                        Resources.copy(res.file(), targetDir);
                    } else {
                        Resources.copy(res, BackupUtils.dir(targetDir, res.name()));
                    }
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(backupFacade, "backupFacade must be set");
        Assert.notNull(xstream, "xstream must be set");
    }

    //
    public void doWrite(Object item, Resource directory, String fileName) throws IOException {
        if (item instanceof ServiceInfo) {
            ServiceInfo service = (ServiceInfo) item;
            XStreamServiceLoader loader = findServiceLoader(service);

            try {
                loader.save(service, backupFacade.getGeoServer(), BackupUtils.dir(directory, fileName));
            } catch (Throwable t) {
                throw new RuntimeException( t );
                //LOGGER.log(Level.SEVERE, "Error occurred while saving configuration", t);
            }
        } else {
            // unwrap dynamic proxies
            OutputStream out = Resources.fromPath(fileName, directory).out();
            try {
                item = xstream.unwrapProxies(item);
                if (xp == null) {
                    xp = xstream.getXStream(); 
                }
                xp.toXML(item, out);
            } finally {
                out.close();
            }
        }
    }

    //
    public Object doRead(Resource directory, String fileName) throws IOException {
        Object item = null;
        InputStream in = Resources.fromPath(fileName, directory).in();
        
        // Try first using the Services Loaders
        final List<XStreamServiceLoader> loaders = 
                GeoServerExtensions.extensions( XStreamServiceLoader.class );
        for ( XStreamServiceLoader<ServiceInfo> l : loaders  ) {
            try {
                if (l.getFilename().equals(fileName)) {
                    item = l.load(backupFacade.getGeoServer(), Resources.fromPath(fileName, directory));
                    
                    if (item != null && item instanceof ServiceInfo) {
                        return item;
                    }
                }
            } catch (Exception e) {
                // Just skip and try with another loader
                item = null;
            }
        }
        
        try {
            if (item == null) {
                try {
                    if (xp == null) {
                        xp = xstream.getXStream(); 
                    }
                    item = xp.fromXML(in);
                } finally {
                    in.close();
                }
            }
        } catch (Exception e) {
            // TODO: Collect warnings
            item = null;
        }
        
        return item;
    }

    protected XStreamServiceLoader findServiceLoader(ServiceInfo service) {
        XStreamServiceLoader loader = null;
        
        final List<XStreamServiceLoader> loaders = 
                GeoServerExtensions.extensions( XStreamServiceLoader.class );
        for ( XStreamServiceLoader<ServiceInfo> l : loaders  ) {
            if ( l.getServiceClass().isInstance( service ) ) {
                loader = l;
                break;
            }
        }

        if (loader == null) {
            throw new IllegalArgumentException("No loader for " + service.getName());
        }
        return loader;
    }
}
