/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersister.Callback;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.RestletException;
import org.restlet.data.Status;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.util.JSONBuilder;

/**
 * Utility class for writing backupexecutions/statuses/etc... to JSON.
 * 
 * Based on Importer {@link ImporterJSONWriter}
 *  
 * @author Justin Deoliveira, OpenGeo
 * @author Alessio Fabiani, GeoSolutions
 */
public class BackupJSONWriter {

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    Backup backupFacade;
    PageInfo page;
    FlushableJSONBuilder json;

    public BackupJSONWriter(Backup backupFacade, PageInfo page) {
        this(backupFacade, page, new ByteArrayOutputStream());
    }

    public BackupJSONWriter(Backup backupFacade, PageInfo page, OutputStream out) {
        this(backupFacade, page, new OutputStreamWriter(out));
    }

    public BackupJSONWriter(Backup backupFacade, PageInfo page, Writer w) {
        this.backupFacade = backupFacade;
        this.page = page;
        this.json = new FlushableJSONBuilder(w);
    }

    public void executions(Iterator<BackupExecutionAdapter> executions, int expand) throws IOException {
        json.object().key("backups").array();
        while (executions.hasNext()) {
            Entry entry = (Entry) executions.next();
            BackupExecutionAdapter execution = (BackupExecutionAdapter) entry.getValue();
            execution(execution, false, expand);
        }
        json.endArray().endObject();
        json.flush();
    }

    public void execution(BackupExecutionAdapter execution, boolean top, int expand) throws IOException {
        if (top) {
            json.object().key("backup");
        }

        json.object();
        json.key("id").value(execution.getId());
        json.key("status").value(execution.getStatus());
        json.key("exitStatus").value(execution.getExitStatus());
        if (execution.getJobParameters() != null) {
            json.key("parameters").value(execution.getJobParameters());
        }
        
        if (execution.getArchiveFile() != null) {
            json.key("archiveFile").value(execution.getArchiveFile().file().getAbsolutePath());
        }
        
        json.key("overwrite").value(execution.isOverwrite());
        
        json.key("progress").value(execution.getExecutedSteps() + "/" + execution.getTotalNumberOfSteps());
        
        json.endObject();

        if (top) {
            json.endObject();
        }
        json.flush();
    }

    String concatErrorMessages(Throwable ex) {
        StringBuilder buf = new StringBuilder();
        while (ex != null) {
            if (buf.length() > 0) {
                buf.append('\n');
            }
            if (ex.getMessage() != null) {
                buf.append(ex.getMessage());
            }
            ex = ex.getCause();
        }
        return buf.toString();
    }

    FlushableJSONBuilder builder(OutputStream out) {
        return new FlushableJSONBuilder(new OutputStreamWriter(out));
    }

    JSONObject toJSON(Object o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        toJSON(o, out);
        return (JSONObject) JSONSerializer.toJSON(new String(out.toByteArray()));
    }

    void toJSON(Object o, OutputStream out) throws IOException {
        toJSON(o, out, null);
    }
    
    void toJSON(Object o, OutputStream out, Callback callback) throws IOException {
        XStreamPersister xp = persister();
        if (callback != null) {
            xp.setCallback(callback);
        }
        xp.save(o, out);
        out.flush();
    }

    XStreamPersister persister() {
        XStreamPersister xp = 
                backupFacade.initXStreamPersister(new XStreamPersisterFactory().createJSONPersister());
        
        xp.setReferenceByName(true);
        xp.setExcludeIds();

        //xp.setCatalog(importer.getCatalog());
        xp.setHideFeatureTypeAttributes();
        // @todo this is copy-and-paste from org.geoserver.catalog.rest.FeatureTypeResource
        xp.setCallback(new XStreamPersister.Callback() {

            @Override
            protected void postEncodeFeatureType(FeatureTypeInfo ft,
                    HierarchicalStreamWriter writer, MarshallingContext context) {
                try {
                    writer.startNode("attributes");
                    context.convertAnother(ft.attributes());
                    writer.endNode();
                } catch (IOException e) {
                    throw new RuntimeException("Could not get native attributes", e);
                }
            }
        });
        return xp;
    }

    static RestletException badRequest(String error) {
        JSONObject errorResponse = new JSONObject();
        JSONArray errors = new JSONArray();
        errors.add(error);
        errorResponse.put("errors", errors);
        
        JSONRepresentation rep = new JSONRepresentation(errorResponse);
        return new RestletException(rep, Status.CLIENT_ERROR_BAD_REQUEST);
    }

    public static class FlushableJSONBuilder extends JSONBuilder {

        public FlushableJSONBuilder(Writer w) {
            super(w);
        }

        public void flush() throws IOException {
            writer.flush();
        }
    }
}
