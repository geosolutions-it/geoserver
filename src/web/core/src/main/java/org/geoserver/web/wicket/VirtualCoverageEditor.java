/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.VirtualCoverage;
import org.geoserver.catalog.VirtualCoverage.CompositionType;
import org.geoserver.catalog.VirtualCoverage.InputCoverageBand;
import org.geoserver.catalog.VirtualCoverage.VirtualCoverageBand;

/**
 *
 */
@SuppressWarnings("serial")
public class VirtualCoverageEditor extends FormComponentPanel {

    List<String> availableCoverages; 
    List<String> selectedCoverages = new ArrayList<String>();
    ListMultipleChoice coveragesChoice;
    CompositionType compositionType;
    
    List<VirtualCoverageBand> outputBands = new ArrayList<VirtualCoverageBand>(); 
    ListMultipleChoice outputBandsChoice;

    TextField definition;
    DropDownChoice<CompositionType> compositionChoice;
    TextField coverageName;

    
    /**
     * Creates a new  editor. 
     * @param id
     * @param The module should return a non null collection of strings.
     */
    public VirtualCoverageEditor(String id, final IModel coverages, List<String> availableCoverages) {
        super(id, coverages);
        this.availableCoverages = availableCoverages;
//        coveragesChoice = new ListMultipleChoice("coveragesChoice",
////              new Model(),
//              new Model((ArrayList<String>) selectedCoverages), 
////              new PropertyModel(this, "selectedCoverages"),
//              availableCoverages);
        
        
        coveragesChoice = new ListMultipleChoice("coveragesChoice",  new Model(), 
                new ArrayList((List) coverages.getObject()), new ChoiceRenderer<String>() {
            @Override
            public Object getDisplayValue(String kw) {
                return kw;
            }
    });
        coveragesChoice.setOutputMarkupId(true);
        add(coveragesChoice);

        outputBands = new ArrayList<VirtualCoverageBand>();
        outputBandsChoice= new ListMultipleChoice("outputBandsChoice", new Model(),
                outputBands, new ChoiceRenderer<VirtualCoverageBand>() {
                    @Override
                    public Object getDisplayValue(VirtualCoverageBand vcb) {
                        return vcb.getDefinition();
                    }
            });
        outputBandsChoice.setOutputMarkupId(true);
        add(outputBandsChoice);

        
        
        add(addBandButton());
        definition = new TextField("newKeyword", new Model());
        definition.setOutputMarkupId(true);
        add(definition);
        compositionType = CompositionType.getDefault();
        compositionChoice = new DropDownChoice("compositionType", 
                new PropertyModel(this, "compositionType" ),
                Arrays.asList(CompositionType.values()), new CompositionTypeRenderer());
                
        compositionChoice.setOutputMarkupId(true);
        add(compositionChoice);

    }

    private AjaxButton addBandButton() {
        AjaxButton button = new AjaxButton("addBand") {
            
            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                List selection = (List) coveragesChoice.getModelObject();
                List coverages = coveragesChoice.getChoices();
                int i = 0;
                for (Iterator it = selection.iterator(); it.hasNext();) {
                    String coverage = (String) it.next();

                    final int bandIndexChar = coverage.indexOf(VirtualCoverage.BAND_SEPARATOR);
                    String coverageName = coverage.substring(0, bandIndexChar);
                    String bandIndex = coverage.substring(bandIndexChar + 1, coverage.length());
                    VirtualCoverageBand band = new VirtualCoverageBand(
                            Collections.singletonList(new InputCoverageBand(coverageName, bandIndex)),
                            coverageName, i++, CompositionType.BAND_SELECT);
                    outputBands.add(band);

                }
                outputBandsChoice.setChoices(outputBands);
                outputBandsChoice.modelChanged();
                coveragesChoice.setChoices(availableCoverages);
                coveragesChoice.modelChanged();

                // TODO: Reset choice
                target.addComponent(coveragesChoice);
                target.addComponent(outputBandsChoice);
            }
        };
        // button.setDefaultFormProcessing(false);
        return button;
    }
    
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        updateFields();
    }

    private void updateFields() {
        coveragesChoice.setChoices(getModel());
    }
    
    @Override
    protected void convertInput() {
        setConvertedInput(coveragesChoice.getChoices());
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
    
    public List<VirtualCoverageBand> getOutputBands() {
        return outputBands;
    }

}
