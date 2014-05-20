/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.awt.image.SampleModel;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.jai.ImageLayout;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.VirtualCoverage;
import org.geoserver.catalog.VirtualCoverage.VirtualCoverageBand;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.VirtualCoverageEditor;
import org.geotools.coverage.grid.io.GridCoverage2DReader;

/**
 * Base page for VirtualCoverage creation/editing
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@SuppressWarnings("serial")
public abstract class VirtualCoverageAbstractPage extends GeoServerSecuredPage {

    public static final String COVERAGESTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    String storeId;

    String coverageInfoId;

    String definition;

    String name;

    boolean newCoverage;

    CoverageInfo virtualCoverageInfo;

    List<String> availableCoverages;

    List<String> selectedCoverages;

    List<VirtualCoverageBand> outputBands;

    VirtualCoverageEditor coverageEditor;

    public VirtualCoverageAbstractPage(PageParameters params) throws IOException {
        this(params.getString(WORKSPACE), params.getString(COVERAGESTORE), null, null);
    }

    @SuppressWarnings("deprecation")
    public VirtualCoverageAbstractPage(String workspaceName, String storeName, String coverageName,
            CoverageInfo coverageInfo) throws IOException {
        storeId = getCatalog().getStoreByName(workspaceName, storeName, CoverageStoreInfo.class)
                .getId();
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getStore(storeId, CoverageStoreInfo.class);

        GridCoverage2DReader reader = (GridCoverage2DReader) catalog.getResourcePool()
                .getGridCoverageReader(store, null);
        String[] coverageNames = reader.getGridCoverageNames();
        if (availableCoverages == null) {
            availableCoverages = new ArrayList<String>();
        }
        for (String coverage : coverageNames) {
            ImageLayout layout = reader.getImageLayout(coverage);
            SampleModel sampleModel = layout.getSampleModel(null);
            final int numBands = sampleModel.getNumBands();
            for (int i = 0; i < numBands; i++) {
                availableCoverages.add(coverage
                        + (numBands > 1 ? (VirtualCoverage.BAND_SEPARATOR + i) : ""));
            }
        }
        Collections.sort(availableCoverages);
        if (coverageName != null) {
            newCoverage = false;

            // grab the virtual coverage
            virtualCoverageInfo = coverageInfo != null ? coverageInfo : catalog.getResourceByStore(
                    store, coverageName, CoverageInfo.class);
            VirtualCoverage virtualCoverage = virtualCoverageInfo.getMetadata().get(
                    VirtualCoverage.VIRTUAL_COVERAGE, VirtualCoverage.class);
            // the type can be still not saved
            if (virtualCoverageInfo != null) {
                coverageInfoId = virtualCoverageInfo.getId();
            }
            if (virtualCoverage == null) {
                throw new IllegalArgumentException(
                        "The specified coverage does not have a virtual coverage attached to it");
            }
            outputBands = new ArrayList<VirtualCoverageBand>(virtualCoverage.getCoverageBands());
            name = virtualCoverage.getName();
        } else {
            outputBands = new ArrayList<VirtualCoverageBand>();
            newCoverage = true;
            virtualCoverageInfo = null;
        }
        selectedCoverages = new ArrayList<String>(availableCoverages);

        // build the form and the text area
        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);

        final TextField nameField = new TextField("name");
        nameField.setRequired(true);
        nameField.add(new VirtualCoverageNameValidator());
        form.add(nameField);

        coverageEditor = new VirtualCoverageEditor("coverages", /* LiveCollectionModel.list( */
                new PropertyModel(this, "selectedCoverages")/*
                                                             * )
                                                             */,/* LiveCollectionModel.list( */
                new PropertyModel(this, "outputBands")/* ) */, availableCoverages);
        form.add(coverageEditor);

        // save and cancel at the bottom of the page
        form.add(new SubmitLink("save") {
            @Override
            public void onSubmit() {
                onSave();
            }
        });
        form.add(new Link("cancel") {

            @Override
            public void onClick() {
                onCancel();
            }
        });
    }


    protected VirtualCoverage buildVirtualCoverage() throws IOException {
        // TODO: ADD HINTS
        VirtualCoverage virtualCoverage = new VirtualCoverage(name,
                (List<VirtualCoverageBand>) ((FormComponent) coverageEditor
                        .get("outputBandsChoice")).getModelObject() /* outpugetOutputBands() */);
        return virtualCoverage;
    }

    /**
     * Data stores tend to return IOExceptions with no explanation, and the actual error coming from the db is in the cause. This method extracts the
     * first not null message in the cause chain
     * 
     * @param t
     * @return
     */
    protected String getFirstErrorMessage(Throwable t) {
        Throwable original = t;

        while (!(t instanceof SQLException)) {
            t = t.getCause();
            if (t == null) {
                break;
            }
        }

        if (t == null) {
            return original.getMessage();
        } else {
            return t.getMessage();
        }
    }

    protected abstract void onSave();

    protected abstract void onCancel();

    /**
     * Checks the Virtual coverage name is unique
     */
    class VirtualCoverageNameValidator extends AbstractValidator {
        @Override
        protected void onValidate(IValidatable validatable) {
            String vcName = (String) validatable.getValue();

            final CoverageStoreInfo store = getCatalog().getStore(storeId, CoverageStoreInfo.class);
            List<CoverageInfo> coverages = getCatalog().getCoveragesByCoverageStore(store);
            for (CoverageInfo curr : coverages) {
                VirtualCoverage currvc = curr.getMetadata().get("VIRTUAL_COVERAGE",
                        VirtualCoverage.class);
                if (currvc != null) {
                    if (coverageInfoId == null || !coverageInfoId.equals(curr.getId())) {
                        if (currvc.getName().equals(vcName) && newCoverage) {
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("name", vcName);
                            map.put("coverageName", curr.getName());
                            error(validatable, "duplicateVirtualCoverageName", map);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }

    public List<String> getSelectedCoverages() {
        return selectedCoverages;
    }

    public void setSelectedCoverages(List<String> selectedCoverages) {
        this.selectedCoverages = selectedCoverages;
    }

    private class CompositionTypeRenderer implements IChoiceRenderer {

        public CompositionTypeRenderer() {
        }

        public Object getDisplayValue(Object object) {
            return object.toString();
        }

        public String getIdValue(Object object, int index) {
            return object.toString();
        }
    }

}
