/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.VirtualCoverage;
import org.geoserver.catalog.VirtualCoverage.VirtualCoverageBand;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;

public class VirtualCoverageEditPage extends VirtualCoverageAbstractPage {

    public VirtualCoverageEditPage(String workspaceName, String storeName, String coverageName,
            CoverageInfo coverageInfo, ResourceConfigurationPage previusPage) throws IOException {
        super(workspaceName, storeName, coverageName, coverageInfo);
        this.previusPage = previusPage;
        this.coverageInfo = coverageInfo;
    }

    private CoverageInfo coverageInfo;

    private ResourceConfigurationPage previusPage;

    protected void onSave() {
        try {
            Catalog catalog = getCatalog();
            CatalogBuilder builder = new CatalogBuilder(catalog);
            CoverageStoreInfo coverageStoreInfo = catalog.getCoverageStore(storeId);
            VirtualCoverage virtualCoverage = buildVirtualCoverage();
            List<VirtualCoverageBand> coverageBands = virtualCoverage.getCoverageBands(); 
            if (coverageBands == null || coverageBands.isEmpty()) {
                throw new IllegalArgumentException("No output bands have been specified ");
            }
            virtualCoverage.updateVirtualCoverageInfo(name, coverageStoreInfo, builder,
                    coverageInfo);

            // set it back in the main page and redirect to it
            previusPage.updateResource(coverageInfo);
            setResponsePage(previusPage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                    .getString());
        }
    }

    protected void onCancel() {
        setResponsePage(previusPage);
    }
}
