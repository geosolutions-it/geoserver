/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerPluginConfigurator;
import org.geoserver.config.GeoServerPropertyConfigurer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * TODO
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogBackupTasklet extends AbstractCatalogBackupRestoreTasklet {
    
    public CatalogBackupTasklet(Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        super(backupFacade, xStreamPersisterFactory);
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {

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
                // On restore invoke 'pluginConfig.loadConfiguration(resourceLoader);' after having replaced the config files.  
                pluginConfig.saveConfiguration(targetGeoServerResourceLoader);
            }
            
            for (GeoServerPropertyConfigurer props : GeoServerExtensions.extensions(GeoServerPropertyConfigurer.class)) {
                // On restore invoke 'props.reload();' after having replaced the properties files.
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

}
