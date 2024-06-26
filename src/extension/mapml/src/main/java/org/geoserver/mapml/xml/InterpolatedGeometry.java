package org.geoserver.mapml.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlMixed;
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

    @XmlMixed
    @XmlElementRef(
            name = "map-coordinates",
            type = Coordinates.class,
            namespace = "http://www.w3.org/1999/xhtml")
    protected List<Coordinates> coordinates;

    public void setCoordinates(List<Coordinates> coordinates) {
        this.coordinates = coordinates;
    }

    public List<Coordinates> getCoordinates() {
        if (coordinates == null) {
            coordinates = new ArrayList<>();
        }
        return this.coordinates;
    }
}
