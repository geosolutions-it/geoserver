/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveJSONFormat;
import org.geoserver.rest.format.ReflectiveXMLFormat;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * REST resource for 
 * 
 * <pre>/br/backup[/&lt;backupId&gt;]?expand=1</pre>
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupResource  extends BaseResource {

    public BackupResource(Backup backupFacade) {
        super(backupFacade);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        
        List<DataFormat> formats = new ArrayList<DataFormat>();
        
        //JSON
        ReflectiveJSONFormat jsonFormat = new ReflectiveJSONFormat();
        intializeXStreamContext(jsonFormat.getXStream());
        formats.add(jsonFormat);
        
        //XML        
        ReflectiveXMLFormat xmlFormat = new ReflectiveXMLFormat();
        intializeXStreamContext(xmlFormat.getXStream());
        formats.add(xmlFormat);

        return formats;
    }

    @Override
    public boolean allowPost() {
        return true;
    }
    
    @Override
    public boolean allowPut() {
        return false;
    }
    
    @Override
    public void handleGet() {
        DataFormat formatGet = getFormatGet(false);
        
        Object lookupContext = lookupContext(true, false);
        if (lookupContext == null) {
            // this means a specific lookup failed
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } else {
            getResponse().setEntity(formatGet.toRepresentation(lookupContext));
        }
    }
    
    @Override
    public void handlePost() {
        Object obj = lookupContext(true, true);
        BackupExecutionAdapter execution = null;
        if (obj instanceof BackupExecutionAdapter) {
            // TODO: restart an existing execution
            /*try {
                restartBackup((BackupExecutionAdapter) obj);
            } catch (Throwable t) {
                if (t instanceof ValidationException) {
                    throw new RestletException(t.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, t);
                } else {
                    throw new RestletException("Error occurred executing backup: ", Status.SERVER_ERROR_INTERNAL, t);
                }
            }*/
        }
        else {
            execution = runBackup();
        }
        if (execution != null) {
            // TODO 
        }
    }
    
    /**
     * 
     * @return
     */
    @SuppressWarnings("unused")
    protected BackupExecutionAdapter runBackup() {
        BackupExecutionAdapter execution = null;
        
        try {
            Form query = getRequest().getResourceRef().getQueryAsForm();
    
            if (MediaType.APPLICATION_JSON.equals(getRequest().getEntity().getMediaType()) ||
                    MediaType.APPLICATION_XML.equals(getRequest().getEntity().getMediaType())) {
                BackupExecutionAdapter newExecution = (BackupExecutionAdapter) getFormatPostOrPut().toObject(getRequest().getEntity());
    
                execution = getBackupFacade().runBackupAsync(
                        newExecution.getArchiveFile(), newExecution.isOverwrite(), asParams(newExecution.getOptions()));
                
                LOGGER.log(Level.INFO, "Backup file generated: " + newExecution.getArchiveFile());
    
                getResponse().redirectSeeOther(getPageInfo().rootURI("/br/backup/"+execution.getId()));
                getResponse().setEntity(getFormatGet().toRepresentation(execution));
                getResponse().setStatus(Status.SUCCESS_CREATED);
            } else {
                throw new IllegalArgumentException("Unknown Content-Type: " + getRequest().getEntity().getMediaType());
            }
        } catch (IllegalArgumentException iae) {
            throw new RestletException("Unable to perform backup: ", Status.CLIENT_ERROR_BAD_REQUEST, iae);
        } catch (Exception e) {
            throw new RestletException("Unable to perform backup: ", Status.SERVER_ERROR_INTERNAL, e);
        }
        
        return execution;
    }
    
    /**
     * 
     * @param allowAll
     * @param mustExist
     * @return
     */
    Object lookupContext(boolean allowAll, boolean mustExist) {
        String i = getAttribute("backupId");
        if (i != null) {
            BackupExecutionAdapter backupExecution = null;
            try {
                backupExecution = getBackupFacade().getBackupExecutions().get(Long.parseLong(i));
            } catch (NumberFormatException e) {
            }
            if (backupExecution == null && mustExist) {
                throw new RestletException("No such backup execution: " + i, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return backupExecution;
        }
        else {
            if (allowAll) {
                return getBackupFacade().getBackupExecutions().entrySet().iterator();
            }
            throw new RestletException("No backup execution specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

}