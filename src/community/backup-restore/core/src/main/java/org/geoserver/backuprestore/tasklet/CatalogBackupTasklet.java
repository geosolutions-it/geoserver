/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerPluginConfigurator;
import org.geoserver.config.GeoServerPropertyConfigurer;
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
import org.springframework.batch.core.UnexpectedJobExecutionException;
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
public class CatalogBackupTasklet implements Tasklet, InitializingBean {

    /*
     * 
     */
    static Map<String, Filter<Resource>> resources = new HashMap<String, Filter<Resource>>();

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
    
    private Backup backupFacade;

    private Catalog catalog;

    private XStreamPersisterFactory xStreamPersisterFactory;

    private XStreamPersister xstream;

    private XStream xp;
    
    public CatalogBackupTasklet(Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        this.backupFacade = backupFacade;
        
        this.xStreamPersisterFactory = xStreamPersisterFactory;
        this.xstream = xStreamPersisterFactory.createXMLPersister();
        this.xstream.setCatalog(catalog);
        this.xstream.setReferenceByName(true);
        this.xstream.setExcludeIds();
        this.xp = this.xstream.getXStream();
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
        } else {
            this.catalog = backupFacade.getCatalog();
        }

        Assert.notNull(catalog, "catalog must be set");

        final GeoServer geoserver = backupFacade.getGeoServer();
        final GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();
        final ResourceStore resourceStore = dd.getResourceStore();
        
        try {
            final String outputFolderURL = jobExecution.getJobParameters().getString(Backup.PARAM_OUTPUT_FILE_PATH);
            Resource targetBackupFolder = Resources.fromURL(outputFolderURL);
            
            // Store GeoServer Global Info
            doWrite(geoserver.getGlobal(), targetBackupFolder, "global.xml");
            
            // Store GeoServer Global Settings
            doWrite(geoserver.getSettings(), targetBackupFolder, "settings.xml");
    
            // Store GeoServer Global Logging Settings
            doWrite(geoserver.getLogging(), targetBackupFolder, "logging.xml");
            
            // Store GeoServer Global Services
            for(ServiceInfo service : geoserver.getServices()) {
                // Local Services will be saved later on ...
                if (service.getWorkspace() == null) {
                    doWrite(service, targetBackupFolder, "services");
                }
            }
            
            // Save Workspace specific settings
            Resource targetWorkspacesFolder = BackupUtils.dir(targetBackupFolder, "workspaces");
            
            // Store Default Workspace
            doWrite(catalog.getDefaultNamespace(), targetWorkspacesFolder, "defaultnamespace.xml");
            doWrite(catalog.getDefaultWorkspace(), targetWorkspacesFolder, "default.xml");
            
            // Store Workspace Specific Settings and Services
            for (WorkspaceInfo ws : catalog.getWorkspaces()) {
                if (geoserver.getSettings(ws) != null) {
                    doWrite(geoserver.getSettings(ws), BackupUtils.dir(targetWorkspacesFolder, ws.getName()), "settings.xml");
                }
                
                if (geoserver.getServices(ws) != null) {
                    for (ServiceInfo service : geoserver.getServices(ws)) {
                        doWrite(service, targetWorkspacesFolder, ws.getName());
                    }
                }
            }
            
            // Backup GeoServer Plugins
            final GeoServerResourceLoader targetGeoServerResourceLoader = new GeoServerResourceLoader(targetBackupFolder.dir());
            for (GeoServerPluginConfigurator pluginConfig : GeoServerExtensions.extensions(GeoServerPluginConfigurator.class)) {
                pluginConfig.saveConfiguration(targetGeoServerResourceLoader);
            }
            
            for (GeoServerPropertyConfigurer props : GeoServerExtensions.extensions(GeoServerPropertyConfigurer.class)) {
                // On restore invoke 'props.reload();' after having replaced the configuration file.
                Resource configFile = props.getConfigFile();
                
                if (configFile != null && Resources.exists(configFile)) {
                    Resource targetDir = 
                        Files.asResource(targetGeoServerResourceLoader.findOrCreateDirectory(
                                Paths.convert(dd.getResourceLoader().getBaseDirectory(), configFile.parent().dir())));
                
                    Resources.copy(configFile.file(), targetDir);
                }
            }
            
            // Backup other configuration bits, like images, palettes, user projections and so on...
            backupAdditionalResources(resourceStore, targetBackupFolder);
            
            // Backup GWC Configuration bits
            if (GeoServerExtensions.bean("gwcGeoServervConfigPersister") != null) {
                backupGWCSettings(targetBackupFolder);
            }
            
        } catch (Exception e) {
            throw new UnexpectedJobExecutionException("Exception occurred while storing GeoServer globals and services settings!", e);            
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * @param targetBackupFolder
     * @throws IOException
     */
    public void backupGWCSettings(Resource targetBackupFolder) throws IOException {
        GWCConfigPersister gwcGeoServervConfigPersister = 
                (GWCConfigPersister) GeoServerExtensions.bean("gwcGeoServervConfigPersister");
        
        GWCConfigPersister testGWCCP = 
                new GWCConfigPersister(xStreamPersisterFactory, new GeoServerResourceLoader(targetBackupFolder.dir()));
        
        testGWCCP.save(gwcGeoServervConfigPersister.getConfig());
        
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
                Resources.copy(gwcProvider.in(), targetGWCProviderBackupDir, providerConfigFile.name());
            }
            
            /**
             * TODO: When Restoring
             * 
             * 1. the securityManager should issue the listeners
             * 2. the GWCInitializer  should be re-initialized
             */
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
                xp.toXML(item, out);
            } finally {
                out.close();
            }
        }
    }
    
    XStreamServiceLoader findServiceLoader(ServiceInfo service) {
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
