/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.rest.format.BackupJSONReader;
import org.geoserver.backuprestore.rest.format.BackupJSONWriter;
import org.geoserver.rest.AbstractResource;
import org.geotools.util.logging.Logging;

/**
 * Abstract Class representing a Backup REST Resource. 
 * 
 * It contains a the {@link Backup} backupFacade reference and utility methods to read/write the
 * resources to/from JSON.
 * 
 * Based on Importer {@link BaseResource}
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class BaseResource extends AbstractResource {

    static Logger LOGGER = Logging.getLogger(BaseResource.class);

    protected Backup backupFacade;

    public BaseResource(Backup backupFacade) {
        this.backupFacade = backupFacade;
    }

    // TODO
    protected int expand(int def) {
        String ex = getRequest().getResourceRef().getQueryAsForm().getFirstValue("expand");
        if (ex == null) {
            return def;
        }

        try {
            return "self".equalsIgnoreCase(ex) ? 1
                    : "all".equalsIgnoreCase(ex) ? Integer.MAX_VALUE
                            : "none".equalsIgnoreCase(ex) ? 0 : Integer.parseInt(ex);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // TODO
    protected BackupJSONReader newReader(InputStream input) throws IOException {
        return new BackupJSONReader(backupFacade, input);
    }

    // TODO
    protected BackupJSONWriter newWriter(OutputStream output) throws IOException {
        return new BackupJSONWriter(backupFacade, getPageInfo(), output);
    }
}
