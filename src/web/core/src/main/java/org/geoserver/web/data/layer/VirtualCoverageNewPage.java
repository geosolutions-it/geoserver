/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.logging.Level;

import org.apache.wicket.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.VirtualCoverage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;

public class VirtualCoverageNewPage extends VirtualCoverageAbstractPage {

    public VirtualCoverageNewPage(PageParameters params) throws IOException {
        this(params.getString(WORKSPACE), params.getString(COVERAGESTORE), null, null);
    }

    public VirtualCoverageNewPage(String workspaceName, String storeName, String coverageName,
            CoverageInfo coverageInfo) throws IOException {
        super(workspaceName, storeName, coverageName, coverageInfo);
    }

    protected void onSave() {
        try {
            Catalog catalog = getCatalog();
            CatalogBuilder builder = new CatalogBuilder(catalog);
            CoverageStoreInfo coverageStoreInfo = catalog.getCoverageStore(storeId);
            CoverageInfo coverageInfo = null;
            VirtualCoverage virtualCoverage = buildVirtualCoverage();
            coverageInfo = virtualCoverage.createVirtualCoverageInfo(name, coverageStoreInfo,
                    builder);
            // else {
            // coverageInfo = virtualCoverageInfo;
            // coverageInfo.getMetadata().put(VirtualCoverage.VIRTUAL_COVERAGE, this);
            // }
            LayerInfo layerInfo = builder.buildLayer(coverageInfo);
            setResponsePage(new ResourceConfigurationPage(layerInfo, true));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                    .getString());
        }
    }

    protected void onCancel() {
        doReturn(LayerPage.class);
    }

}
