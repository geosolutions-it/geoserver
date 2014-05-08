/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.awt.image.SampleModel;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.media.jai.ImageLayout;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
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
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.io.GridCoverage2DReader;

/**
 * Base page for VirtualCoverage creation/editing
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@SuppressWarnings("serial")
public class VirtualCoveragePage extends GeoServerSecuredPage {

    private static final String BAND_SEPARATOR = "@";

    public static final String COVERAGESTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    String storeId;
    
    String coverageInfoId;

    String definition;
    
    String name;

    boolean newCoverage;
    
    List<String> availableCoverages; 
    List<String> selectedCoverages;
    ListMultipleChoice coveragesChoice;
//    DropDownChoice compositionType;
    
    List<VirtualCoverageBand> outputBands; 
    ListMultipleChoice outputBandsChoice;

    private TextArea definitionEditor;
    
//    private GeoServerTablePanel<SQLViewAttribute> attributes;
    
//    private GeoServerTablePanel<Parameter> parameters;

//    private SQLViewParamProvider paramProvider;
    
//    boolean guessGeometrySrid = false;

//    private CheckBox guessCheckbox;
    
//    private boolean escapeSql = true;

//    private static final List GEOMETRY_TYPES = Arrays.asList(Geometry.class,
//            GeometryCollection.class, Point.class, MultiPoint.class, LineString.class,
//            MultiLineString.class, Polygon.class, MultiPolygon.class);

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

            // get the store
//            DataAccess da = store.getDataStore(null);
//            if (!(da instanceof JDBCDataStore)) {
//                error("Cannot create a VirtualCoverage if the store is not database based");
//                doReturn(StorePage.class);
//                return;
//            }

            name = virtualCoverage.getName();
            String[] coverageNames = reader.getGridCoverageNames();
//            sql = virtualTable.getSql();
//            escapeSql = virtualTable.isEscapeSql();
//
//            paramProvider.init(virtualTable);
//            try {
//                SimpleFeatureType ft = testViewDefinition(virtualTable, false);
//                attProvider.setFeatureType(ft, virtualTable);
//            } catch(Exception e) {
//                LOGGER.log(Level.SEVERE, "Failed to build feature type for the sql view", e);
//            }
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
//                    builder.append(coverage).append("@").append(i).append("\n");
                    availableCoverages.add(coverage + BAND_SEPARATOR + i);
                }
                
            }
            Collections.sort(availableCoverages);
//            coverages = builder.substring(0, builder.length()-1).toString();
        }
        selectedCoverages = new ArrayList<String>();
        
        // build the form and the text area
        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);
        
//        add(new VirtualCoverageEditor("virtualCoverageConfig", LiveCollectionModel.list(new PropertyModel(model, "keywords"))));
        
        final TextField nameField = new TextField("name");
        nameField.setRequired(true);
        nameField.add(new VirtualCoverageNameValidator());
        form.add(nameField);
        
        
        coveragesChoice = new ListMultipleChoice("coveragesChoice",
                new Model((ArrayList<String>) selectedCoverages), 
//                new PropertyModel(this, "selectedCoverages"),
                availableCoverages);
                /* availableCoverages, new ChoiceRenderer<String>() {
                    @Override
                    public Object getDisplayValue(String kw) {
                        return kw;
                    }
            });*/
        coveragesChoice.setOutputMarkupId(true);
        form.add(coveragesChoice);
        form.add(addBandButton());
        
//        compositionType = new DropDownChoice("compositionType", Arrays.asList(CompositionType.values()), new CompositionTypeRenderer());
//        form.add(compositionType);
        outputBands = new ArrayList<VirtualCoverageBand>();
        outputBandsChoice= new ListMultipleChoice("outputBandsChoice", new Model(),
                outputBands, new ChoiceRenderer<VirtualCoverageBand>() {
                    @Override
                    public Object getDisplayValue(VirtualCoverageBand kw) {
                        return kw.getDefinition();
                    }
            });
        outputBandsChoice.setOutputMarkupId(true);
        form.add(outputBandsChoice);
        
        
        
        
        
        definitionEditor = new TextArea("definition");
        form.add(definitionEditor);
        
        
        
        
        // the links to refresh, add and remove a parameter
//        form.add(new GeoServerAjaxFormLink("guessParams") {
//            
//            @Override
//            protected void onClick(AjaxRequestTarget target, Form form) {
//                sqlEditor.processInput();
//                parameters.processInputs();
//                if (sql != null && !"".equals(sql.trim())) {
//                    paramProvider.refreshFromSql(sql);
//                    target.addComponent(parameters);
//                }
//            }
//        });
//        form.add(new GeoServerAjaxFormLink("addNewParam") {
//            
//            @Override
//            protected void onClick(AjaxRequestTarget target, Form form) {
//                paramProvider.addParameter();
//                target.addComponent(parameters);
//            }
//        });
//        form.add(new GeoServerAjaxFormLink("removeParam") {
//            
//            @Override
//            protected void onClick(AjaxRequestTarget target, Form form) {
//                paramProvider.removeAll(parameters.getSelection());
//                parameters.clearSelection();
//                target.addComponent(parameters);
//            }
//        });
//        
//        // the parameters table
//        parameters = new GeoServerTablePanel<Parameter>("parameters", paramProvider, true) {
//
//            @Override
//            protected Component getComponentForProperty(String id, IModel itemModel,
//                    Property<Parameter> property) {
//                Fragment f = new Fragment(id, "text", VirtualCoveragePage.this);
//                TextField text = new TextField("text", property.getModel(itemModel));
//                text.setLabel(new ParamResourceModel("th." + property.getName(), VirtualCoveragePage.this));
//                if(property == SQLViewParamProvider.NAME) {
//                    text.setRequired(true);
//                } else if(property == SQLViewParamProvider.REGEXP) {
//                    text.add(new RegexpValidator());
//                }
//                f.add(text);
//                return f;
//            }
//            
//        };
//        parameters.setFilterVisible(false);
//        parameters.setSortable(false);
//        parameters.getTopPager().setVisible(false);
//        parameters.getBottomPager().setVisible(false);
//        parameters.setOutputMarkupId(true);
//        form.add(parameters);
//        
//        // the "refresh attributes" link
//        form.add(refreshLink());
//        form.add(guessCheckbox = new CheckBox("guessGeometrySrid", new PropertyModel(this, "guessGeometrySrid")));
//        form.add(new CheckBox("escapeSql"));
// 
//        // the editable attribute table
//        attributes = new GeoServerTablePanel<SQLViewAttribute>("attributes", attProvider) {
//
//            @Override
//            protected Component getComponentForProperty(String id, IModel itemModel,
//                    Property<SQLViewAttribute> property) {
//                SQLViewAttribute att = (SQLViewAttribute) itemModel.getObject();
//                boolean isGeometry = att.getType() != null
//                        && Geometry.class.isAssignableFrom(att.getType());
//                if (property == SQLViewAttributeProvider.PK) {
//                    // editor for pk status
//                    Fragment f = new Fragment(id, "checkbox", VirtualCoveragePage.this);
//                    f.add(new CheckBox("identifier", new PropertyModel(itemModel, "pk")));
//                    return f;
//                } else if (property == SQLViewAttributeProvider.TYPE && isGeometry) {
//                    Fragment f = new Fragment(id, "geometry", VirtualCoveragePage.this);
//                    f.add(new DropDownChoice("geometry", new PropertyModel(itemModel, "type"),
//                            GEOMETRY_TYPES, new GeometryTypeRenderer()));
//                    return f;
//                } else if(property == SQLViewAttributeProvider.SRID && isGeometry) {
//                    Fragment f = new Fragment(id, "text", VirtualCoveragePage.this);
//                    f.add(new TextField("text", new PropertyModel(itemModel, "srid")));
//                    return f;
//                }
//                return null;
//            }
//        };
//        // just a plain table, no filters, no paging, 
//        attributes.setFilterVisible(false);
//        attributes.setSortable(false);
//        attributes.setPageable(false);
//        attributes.setOutputMarkupId(true);
//        form.add(attributes);

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
    
    private AjaxButton addBandButton() {
        AjaxButton button = new AjaxButton("addBand") {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                coveragesChoice.getValue();
                List selection = (List) coveragesChoice.getModelObject();
                selection.get(0);
//                    update(target, coveragesChoice, outputBandsChoice);
            }
//          
        };
        button.setDefaultFormProcessing(false);
        return button;
    }
        
        private void update(AjaxRequestTarget target, ListMultipleChoice from, ListMultipleChoice to){
//            String value = newKeyword.getInput();
                //TODO: Check bandComposition
            for (String selected : (List<String>) from.getChoices()) {
//                List choices = from.getChoices();
                if (!to.getChoices().contains(selected)) {
                  to.getChoices().add(selected);
                  
                }
              }
              target.addComponent(to);
              target.addComponent(from);
            
            
//                List<String> coverages = (List<String>) coveragesChoice.getModelObject();
//                int i=0;
//                for (String coverage: coverages) {
//                    //TODO check for band composition
//                    final int bandIndexChar = coverage.indexOf(BAND_SEPARATOR);
//                    String coverageName = coverage.substring(0, bandIndexChar);
//                    String bandIndex = coverage.substring(bandIndexChar + 1 , coverage.length());
//                    VirtualCoverageBand band = new VirtualCoverageBand(Collections.singletonList(new InputCoverageBand(coverageName, bandIndex)), coverageName, i++, CompositionType.BAND_SELECT);
//                    outputBands.add(band);
//                }
//                
//                outputBandsChoice.setChoices(outputBands);
//                coverages.clear();
//                target.addComponent(coveragesChoice);
            }
       
      

    private GeoServerAjaxFormLink refreshLink() {
        return new GeoServerAjaxFormLink("refresh") {

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                definitionEditor.processInput();
//                parameters.processInputs();
//                guessCheckbox.processInput();
//                if (sql != null && !"".equals(sql.trim())) {
//                    SimpleFeatureType newSchema = null;
//                    try {
//                        newSchema = testViewDefinition(guessGeometrySrid);
//
//                        if (newSchema != null) {
//                            attProvider.setFeatureType(newSchema, null);
//                            target.addComponent(attributes);
//                        }
//                    } catch (IOException e) {
//                        LOGGER.log(Level.INFO, "Error testing SQL query", e);
//                        error(getFirstErrorMessage(e));
//                    }
//                }
            }
        };
    }

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
        String inputs[] = definition.split(",");
        List<VirtualCoverageBand> bands = new ArrayList<VirtualCoverageBand>(inputs.length);
        int i=0;
        for (String input: inputs) {
            bands.add(new VirtualCoverageBand(Collections.singletonList(new InputCoverageBand(input, "1")), input, i++, CompositionType.BAND_SELECT));
        }
        VirtualCoverage virtualCoverage = new VirtualCoverage(name, bands);
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
    
//    public DropDownChoice getCompositionType() {
//        return compositionType;
//    }
//
//    public void setCompositionType(DropDownChoice compositionType) {
//        this.compositionType = compositionType;
//    }

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
    
    private class CompositionTypeRenderer implements  IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((CompositionType) object).name(), VirtualCoveragePage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return ((CompositionType) object).name();
        }
        
    }
    
    
   
}
