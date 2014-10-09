/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate.impl;

import java.io.IOException;

import org.geoserver.importer.ImportTask;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;

/**
 * Implementation of the {@link TranslateItem} wrapping an {@link ImportTransform}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class TransformItem extends TranslateItem {

    private ImportTransform transform;

    /**
     * @return the transform
     */
    public ImportTransform getTransform() {
        return transform;
    }

    /**
     * @param transform the transform to set
     */
    public void setTransform(ImportTransform transform) {
        this.transform = transform;
    }

    @Override
    protected TranslateItem execute(TranslateContext context) throws IOException {
        final ImportTask task = context.getImportContext().getTasks().get(0);
        if (task != null && task.getTransform() != null) {
            final TransformChain transformChain = task.getTransform();
            transformChain.add(this.transform);

            context.getImporter().changed(task);
        }

        return null;
    }

}
