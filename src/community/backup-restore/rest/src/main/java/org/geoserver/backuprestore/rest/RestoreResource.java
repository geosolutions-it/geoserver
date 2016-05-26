/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.backuprestore.rest.format.RestoreJSONReader;
import org.geoserver.backuprestore.rest.format.RestoreJSONWriter;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * REST resource for 
 * 
 * <pre>/br/restore[/&lt;restoreId&gt;]?expand=1</pre>
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RestoreResource  extends BaseResource {

    public RestoreResource(Backup backupFacade) {
        super(backupFacade);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected List<DataFormat> createSupportedFormats(Request arg0, Response arg1) {
        return (List) Arrays.asList(new RestoreJSONFormat(MediaType.APPLICATION_JSON),
                new RestoreJSONFormat(MediaType.TEXT_HTML));
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
        DataFormat formatGet = getFormatGet();
        if (formatGet == null) {
            formatGet = new RestoreJSONFormat(MediaType.APPLICATION_JSON);
        }
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
        RestoreExecutionAdapter execution = null;
        if (obj instanceof RestoreExecutionAdapter) {
            // TODO: restart an existing execution
            /*try {
                restartRestore((RestoreExecutionAdapter) obj);
            } catch (Throwable t) {
                if (t instanceof ValidationException) {
                    throw new RestletException(t.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST, t);
                } else {
                    throw new RestletException("Error occured executing restore: ", Status.SERVER_ERROR_INTERNAL, t);
                }
            }*/
        }
        else {
            execution = runRestore();
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
    protected RestoreExecutionAdapter runRestore() {
        RestoreExecutionAdapter execution = null;
        
        try {
            Form query = getRequest().getResourceRef().getQueryAsForm();
    
            if (MediaType.APPLICATION_JSON.equals(getRequest().getEntity().getMediaType())) {
                RestoreExecutionAdapter newExecution = (RestoreExecutionAdapter) getFormatPostOrPut().toObject(getRequest().getEntity());
    
                // TODO: archiveFile and overwrite option integrity checks
                
                execution = getBackupFacade().runRestoreAsync(newExecution.getArchiveFile(), asParams(newExecution.getOptions()));
                
                LOGGER.log(Level.INFO, "Restore file started: " + newExecution.getArchiveFile());
    
                getResponse().redirectSeeOther(getPageInfo().rootURI("/br/restore/"+execution.getId()));
                getResponse().setEntity(getFormatGet().toRepresentation(execution));
                getResponse().setStatus(Status.SUCCESS_CREATED);
            } else {
                throw new IllegalArgumentException("Unknown Content-Type: " + getRequest().getEntity().getMediaType());
            }
        } catch (IllegalArgumentException iae) {
            throw new RestletException("Unable to perform restore: ", Status.CLIENT_ERROR_BAD_REQUEST, iae);
        } catch (Exception e) {
            throw new RestletException("Unable to perform restore: ", Status.SERVER_ERROR_INTERNAL, e);
        }
        
        return execution;
    }

    /**
     * 
     * @author afabiani
     *
     */
    class RestoreJSONFormat extends StreamDataFormat {

        public RestoreJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).execution();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            
            PageInfo pageInfo = getPageInfo();
            // @hack lop off query if there is one or resulting URIs look silly
            int queryIdx = pageInfo.getPagePath().indexOf('?');
            if (queryIdx > 0) {
                pageInfo.setPagePath(pageInfo.getPagePath().substring(0, queryIdx));
            }

            RestoreJSONWriter json = newWriter(out);
            if (object instanceof RestoreExecutionAdapter) {
                json.execution((RestoreExecutionAdapter) object, true, expand(1));
            }
            else {
                json.executions((Iterator<RestoreExecutionAdapter>) object, expand(0));
            }
        }
    }
    
    /**
     * 
     * @param allowAll
     * @param mustExist
     * @return
     */
    Object lookupContext(boolean allowAll, boolean mustExist) {
        String i = getAttribute("restoreId");
        if (i != null) {
            RestoreExecutionAdapter restoreExecution = null;
            try {
                restoreExecution = getBackupFacade().getRestoreExecutions().get(Long.parseLong(i));
            } catch (NumberFormatException e) {
            }
            if (restoreExecution == null && mustExist) {
                throw new RestletException("No such restore execution: " + i, Status.CLIENT_ERROR_NOT_FOUND);
            }
            return restoreExecution;
        }
        else {
            if (allowAll) {
                return getBackupFacade().getRestoreExecutions().entrySet().iterator();
            }
            throw new RestletException("No restore execution specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }
    
    /**
     * 
     * @param input
     * @return
     * @throws IOException
     */
    public RestoreJSONReader newReader(InputStream input) throws IOException {
        return new RestoreJSONReader(getBackupFacade(), input);
    }

    /**
     * 
     * @param output
     * @return
     * @throws IOException
     */
    public RestoreJSONWriter newWriter(OutputStream output) throws IOException {
        return new RestoreJSONWriter(getBackupFacade(), getPageInfo(), output);
    }
}
