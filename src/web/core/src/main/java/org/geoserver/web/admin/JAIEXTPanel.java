package org.geoserver.web.admin;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.config.JAIEXTInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.wicket.LiveCollectionModel;

public class JAIEXTPanel extends Panel {

    public JAIEXTPanel(String id, IModel<JAIInfo> model) {
        super(id, model);

        PropertyModel<JAIEXTInfo> jaiextModel = new PropertyModel<JAIEXTInfo>(model, "JAIEXTInfo");

        Palette jaiextSelector = new Palette("jaiextOps",
                LiveCollectionModel.set(new PropertyModel(jaiextModel, "JAIOperations")),
                LiveCollectionModel.set(new PropertyModel(jaiextModel, "JAIEXTOperations")),
                new ChoiceRenderer(), 7, false) {
            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newSelectedHeader(final String componentId) {
                return new Label(componentId, new ResourceModel("JAIEXTPanel.selectedHeader"));
            }

            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newAvailableHeader(final String componentId) {
                return new Label(componentId, new ResourceModel("JAIEXTPanel.availableHeader"));
            }
        };
        add(jaiextSelector);
    }

    
    static class ChoiceRenderer implements IChoiceRenderer<String>{

        @Override
        public Object getDisplayValue(String object) {
            if(object.equalsIgnoreCase("Stats")){
                return "Statistics";
            } else if(object.equalsIgnoreCase("operationConst")){
                return "Math operations with Constant";
            }else if(object.equalsIgnoreCase("algebric")){
                return "Math operations";
            }else {
                return object;
            }
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
        
    }
}
