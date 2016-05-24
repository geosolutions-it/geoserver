/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.backuprestore.Backup;
import org.geoserver.rest.AbstractResource;
import org.geotools.factory.Hints;
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

    private Backup backupFacade;

    public BaseResource(Backup backupFacade) {
        this.backupFacade = backupFacade;
    }

    /**
     * @return the backupFacade
     */
    public Backup getBackupFacade() {
        return backupFacade;
    }

    /**
     * @param backupFacade the backupFacade to set
     */
    public void setBackupFacade(Backup backupFacade) {
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected  Hints asParams(List<String> options) {
        Hints hints = new Hints(new HashMap(2));
        
        if (options != null) {
            for (String option : options) {
                if (Backup.PARAM_DRY_RUN_MODE.equals(option)) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_DRY_RUN_MODE), option));
                }
                
                if (Backup.PARAM_BEST_EFFORT_MODE.equals(option)) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), option));
                }                
            }
        }
        
        return hints;
    }
}
