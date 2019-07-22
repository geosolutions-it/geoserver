/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.generatedgeometries.core;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.geometry.BoundingBox;

public class GeneratedGeometryBoundsFinder implements FeatureVisitor {

    private FeatureTypeInfo featureTypeInfo;
    private BoundingBox bbox = null;

    public GeneratedGeometryBoundsFinder(FeatureTypeInfo featureTypeInfo) {
        this.featureTypeInfo = featureTypeInfo;
    }

    @Override
    public void visit(Feature feature) {

        if (bbox == null) bbox = feature.getBounds();
        else bbox.include(feature.getBounds());
    }

    public ReferencedEnvelope getBounds() {
        ReferencedEnvelope refEnv = new ReferencedEnvelope(featureTypeInfo.getCRS());

        if (bbox == null) return refEnv;

        refEnv.setBounds(bbox);
        return refEnv;
    }
}
