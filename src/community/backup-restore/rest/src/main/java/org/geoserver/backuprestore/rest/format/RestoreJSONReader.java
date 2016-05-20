/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.resource.Files;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Utility class for reading restoreexecutions/filters/etc... from JSON.
 * 
 * Reads restore execution parameters from JSON
 *
 * <pre>
 * {
 *         "restore": {
 *              "archiveFile": "<path_to_archive_file>.zip"
 *         }
 * }
 * </pre>
 * 
 * Based on Importer {@link ImporterJSONReader}
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author Alessio Fabiani, GeoSolutions
 */
public class RestoreJSONReader {

    Backup backupFacade;

    JSONObject json;

    public RestoreJSONReader(Backup backupFacade, String in) throws IOException {
        this(backupFacade, new ByteArrayInputStream(in.getBytes()));
    }

    public RestoreJSONReader(Backup backupFacade, InputStream in) throws IOException {
        this.backupFacade = backupFacade;
        json = parse(in);
    }

    public RestoreJSONReader(Backup backupFacade, JSONObject obj) {
        this.backupFacade = backupFacade;
        json = obj;
    }

    public JSONObject object() {
        return json;
    }

    public RestoreExecutionAdapter execution() throws IOException {
        RestoreExecutionAdapter execution = null;
        if (json.has("restore")) {
            execution = new RestoreExecutionAdapter(null, 0);

            json = json.getJSONObject("restore");
            
            if (json.has("archiveFile")) {
                execution.setArchiveFile(Files.asResource(new File(json.getString("archiveFile"))));
            }
            
            if (json.has("options")) {
                execution.getOptions().addAll(getOptions(json));
            }
        }
        return execution;
    }

    List<String> getOptions(JSONObject json) {
        JSONArray array = json.getJSONArray("options");
        List<String> options = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            String option = array.getString(i);
            options.add(option);
        }
        return options;
    }

    JSONObject parse(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(in, bout);
        return JSONObject.fromObject(new String(bout.toByteArray()));
    }

    <T> T fromJSON(JSONObject json, Class<T> clazz) throws IOException {
        XStreamPersister xp = backupFacade.createXStreamPersisterJSON();
        return xp.load(new ByteArrayInputStream(json.toString().getBytes()), clazz);
    }

    <T> T fromJSON(Class<T> clazz) throws IOException {
        return fromJSON(json, clazz);
    }
}
