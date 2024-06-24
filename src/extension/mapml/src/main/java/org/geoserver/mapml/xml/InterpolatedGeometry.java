package org.geoserver.mapml.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "",
        propOrder = {"components", "coordinates"})
public class InterpolatedGeometry {
    @XmlAttribute protected String type;

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    @XmlElementWrapper(name = "map-components", namespace = "http://www.w3.org/1999/xhtml")
    @XmlElement(
            name = "map-interpolated-geometry",
            type = InterpolatedGeometry.class,
            namespace = "http://www.w3.org/1999/xhtml")
    protected List<InterpolatedGeometry> components;

    public List<InterpolatedGeometry> getComponents() {
        if (components == null) {
            components = new ArrayList<>();
        }
        return components;
    }

    public void setComponents(List<InterpolatedGeometry> components) {
        this.components = components;
    }

    @XmlElement(name = "map-coordinates", namespace = "http://www.w3.org/1999/xhtml")
    protected List<String> coordinates;

    public void setCoordinates(List<String> coordinates) {
        this.coordinates = coordinates;
    }

    public List<String> getCoordinates() {
        if (coordinates == null) {
            coordinates = new ArrayList<>();
        }
        return this.coordinates;
    }
}
