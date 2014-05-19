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
import java.util.logging.Level;

import javax.media.jai.ImageLayout;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.VirtualCoverage;
import org.geoserver.catalog.VirtualCoverage.CompositionType;
import org.geoserver.catalog.VirtualCoverage.InputCoverageBand;
import org.geoserver.catalog.VirtualCoverage.VirtualCoverageBand;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.VirtualCoverageEditor;
import org.geotools.coverage.grid.io.GridCoverage2DReader;

/**
 * Base page for VirtualCoverage creation/editing
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@SuppressWarnings("serial")
public class VirtualCoveragePage extends GeoServerSecuredPage {

    public static final String COVERAGESTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    String storeId;
    
    String coverageInfoId;

    String definition;
    
    String name;

    boolean newCoverage;
    
    List<String> availableCoverages; 
    List<String> selectedCoverages;
    
    VirtualCoverageEditor coverageEditor;

    
//    private GeoServerTablePanel<SQLViewAttribute> attributes;
    
//    private GeoServerTablePanel<Parameter> parameters;

    
    public VirtualCoveragePage(PageParameters params) throws IOException {
        this(params.getString(WORKSPACE), params.getString(COVERAGESTORE), null, null);
    }

    @SuppressWarnings("deprecation")
    public VirtualCoveragePage(String workspaceName, String storeName, String coverageName, VirtualCoverage virtualCoverage)
            throws IOException {
        storeId = getCatalog().getStoreByName(workspaceName, storeName, CoverageStoreInfo.class).getId();
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getStore(storeId, CoverageStoreInfo.class);
        if (coverageName != null) {
            newCoverage = false;

//             grab the virtual coverage
            CoverageInfo coverageInfo = catalog.getResourceByStore(store, coverageName, CoverageInfo.class);
            GridCoverage2DReader reader = (GridCoverage2DReader) catalog.getResourcePool().getGridCoverageReader(store, null);
            // the type can be still not saved
            if (coverageInfo != null) {
                String coverageInfoId = coverageInfo.getId();
            }
            if(virtualCoverage  == null) {
                throw new IllegalArgumentException("The specified coverage does not have a virtual coverage attached to it");
            }


            name = virtualCoverage.getName();
            String[] coverageNames = reader.getGridCoverageNames();
        } else {
            newCoverage = true;
            GridCoverage2DReader reader = (GridCoverage2DReader) catalog.getResourcePool().getGridCoverageReader(store, null);
            String[] coverageNames = reader.getGridCoverageNames();
            StringBuilder builder = new StringBuilder();
            if (availableCoverages == null) {
                availableCoverages = new ArrayList<String>();
            }
            for (String coverage: coverageNames) {
                ImageLayout layout = reader.getImageLayout(coverage);
                SampleModel sampleModel = layout.getSampleModel(null);
                final int numBands = sampleModel.getNumBands();
                for (int i=0; i < numBands; i++ ) {
                    availableCoverages.add(coverage + VirtualCoverage.BAND_SEPARATOR + i);
                }
                
            }
            Collections.sort(availableCoverages);
        }
        selectedCoverages = new ArrayList<String>(availableCoverages);
        
        // build the form and the text area
        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);
        
        
        final TextField nameField = new TextField("name");
        nameField.setRequired(true);
        nameField.add(new VirtualCoverageNameValidator());
        form.add(nameField);
        

        coverageEditor = new VirtualCoverageEditor("coverages", LiveCollectionModel.list(new PropertyModel(this, "selectedCoverages")), availableCoverages);
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
//                onCancel();
            }
        });
    }
    
//      
//
//    private GeoServerAjaxFormLink refreshLink() {
//        return new GeoServerAjaxFormLink("refresh") {
//
//            @Override
//            protected void onClick(AjaxRequestTarget target, Form form) {
//                definitionEditor.processInput();
////                parameters.processInputs();
////                guessCheckbox.processInput();
////                if (sql != null && !"".equals(sql.trim())) {
////                    SimpleFeatureType newSchema = null;
////                    try {
////                        newSchema = testViewDefinition(guessGeometrySrid);
////
////                        if (newSchema != null) {
////                            attProvider.setFeatureType(newSchema, null);
////                            target.addComponent(attributes);
////                        }
////                    } catch (IOException e) {
////                        LOGGER.log(Level.INFO, "Error testing SQL query", e);
////                        error(getFirstErrorMessage(e));
////                    }
////                }
//            }
//        };
//    }

//    /**
//     * Checks the view definition works as expected and returns the feature type guessed solely by
//     * looking at the sql and the first row of its output
//     * 
//     * @param newSchema
//     * @return
//     * @throws IOException
//     */
//    protected SimpleFeatureType testViewDefinition(boolean guessGeometrySrid) throws IOException {
//        // check out if the view can be used
//        JDBCDataStore ds = (JDBCDataStore) getCatalog().getDataStore(storeId).getDataStore(null);
//        String vtName = null;
//        try {
//            // use a highly random name
//            do {
//                vtName = UUID.randomUUID().toString();
//            } while (Arrays.asList(ds.getTypeNames()).contains(vtName));
//
//            // try adding the vt and see if that works
//            VirtualTable vt = new VirtualTable(vtName, sql);
//            paramProvider.updateVirtualTable(vt);
//            ds.addVirtualTable(vt);
//            return guessFeatureType(ds, vt.getName(), guessGeometrySrid);
//        } finally {
//            if(vtName != null) {
//                ds.removeVirtualTable(vtName);
//            }
//            
//        }
//    }
//    
//    protected SimpleFeatureType getFeatureType(VirtualCoverage vt) throws IOException {
//     // check out if the view can be used
//        JDBCDataStore ds = (JDBCDataStore) getCatalog().getCoverageStore(storeId).getDataStore(null);
//        String vtName = null;
//        try {
//            // use a highly random name
//            do {
//                vtName = UUID.randomUUID().toString();
//            } while (Arrays.asList(ds.getTypeNames()).contains(vtName));
//
//            // try adding the vt and see if that works
//            ds.addVirtualTable(new VirtualTable(vtName, vt));
//            return ds.getSchema(vtName);
//        } finally {
//            if(vtName != null) {
//                ds.removeVirtualTable(vtName);
//            }
//            
//        }
//    }
//    
//    /**
//     * Checks the view definition works as expected and returns the feature type guessed solely by
//     * looking at the sql and the first row of its output
//     * 
//     * @param newSchema
//     * @return
//     * @throws IOException
//     */
//    protected SimpleFeatureType testViewDefinition(VirtualTable virtualTable, boolean guessGeometrySrid) throws IOException {
//        // check out if the view can be used
//        JDBCDataStore ds = (JDBCDataStore) getCatalog().getDataStore(storeId).getDataStore(null);
//        String vtName = null;
//        try {
//            // use a highly random name
//            do {
//                vtName = UUID.randomUUID().toString();
//            } while (Arrays.asList(ds.getTypeNames()).contains(vtName));
//
//            // try adding the vt and see if that works
//            VirtualTable vt = new VirtualTable(vtName, virtualTable);
//            // hide the primary key definitions or we'll loose some columns
//            vt.setPrimaryKeyColumns(Collections.EMPTY_LIST);
//            vt.setEscapeSql(escapeSql);
//            ds.addVirtualTable(vt);
//            return guessFeatureType(ds, vt.getName(), guessGeometrySrid);
//        } finally {
//            if(vtName != null) {
//                ds.removeVirtualTable(name);
//            }
//            
//        }
//    }

    protected VirtualCoverage buildVirtualCoverage(CoverageStoreInfo storeInfo) throws IOException {
                // TODO: ADD HINTS
        VirtualCoverage virtualCoverage = new VirtualCoverage(name, coverageEditor.getOutputBands());
        return virtualCoverage;
    }

    /**
     * Data stores tend to return IOExceptions with no explanation, and the actual error coming from
     * the db is in the cause. This method extracts the first not null message in the cause chain
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
        
        if(t == null) {
            return original.getMessage();
        } else {
            return t.getMessage();
        }
    }
    
    protected void onSave() {
        try {
            Catalog catalog = getCatalog();
            CoverageStoreInfo coverageStoreInfo = catalog.getCoverageStore(storeId);
            VirtualCoverage virtualCoverage = buildVirtualCoverage(coverageStoreInfo);
            CatalogBuilder builder = new CatalogBuilder(catalog);
            CoverageInfo coverageInfo = virtualCoverage.createVirtualCoverageInfo(name, coverageStoreInfo, builder);
            LayerInfo layerInfo = builder.buildLayer(coverageInfo);
            setResponsePage(new ResourceConfigurationPage(layerInfo, true));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(new ParamResourceModel("creationFailure", this, getFirstErrorMessage(e))
                    .getString());
        }
    }


//    protected abstract void onCancel();
//    
//    /**
//     * Displays the geometry type in the geom type drop down
//     * @author Andrea Aime - OpenGeo
//     */
//    static class GeometryTypeRenderer implements IChoiceRenderer {
//
//        public Object getDisplayValue(Object object) {
//            return ((Class) object).getSimpleName();
//        }
//
//        public String getIdValue(Object object, int index) {
//            return (String) getDisplayValue(object);
//        }
//        
//    }
//    
//    /**
//     * Validaes the regular expression syntax
//     */
//    static class RegexpValidator extends AbstractValidator {
//
//        @Override
//        protected void onValidate(IValidatable validatable) {
//            String value = (String) validatable.getValue();
//            if(value != null) {
//                try {
//                    Pattern.compile(value);
//                } catch(PatternSyntaxException e) {
//                    Map<String, String> map = new HashMap<String, String>();
//                    map.put("regexp", value);
//                    map.put("error", e.getMessage().replaceAll("\\^?", ""));
//                    error(validatable, "invalidRegexp", map);
//                }
//            }
//        }
//        
//    }
//
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
                VirtualCoverage currvc = curr.getMetadata().get("VIRTUAL_COVERAGE", VirtualCoverage.class);
                if(currvc != null) {
                    if(coverageInfoId == null || !coverageInfoId.equals(curr.getId())) {
                        if(currvc.getName().equals(vcName)) {
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


    private class CompositionTypeRenderer implements  IChoiceRenderer {

        public CompositionTypeRenderer() {
        }

        public Object getDisplayValue(Object object) {
            return object.toString();
        }

        public String getIdValue(Object object, int index ) {
            return object.toString();
        }
    }
   
}
