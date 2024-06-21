package org.geoserver.mapml.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "",
        propOrder = {"head", "mapInterpolatedProperties", "mapInterpolatedGeometry"})
@XmlRootElement(name = "mapml-interpolated", namespace = "http://www.w3.org/1999/xhtml")
public class Interpolated {
    @XmlElement(required = false, name = "map-head", namespace = "http://www.w3.org/1999/xhtml")
    protected HeadContent head;

    @XmlElement(
            name = "map-interpolated-property",
            required = false,
            namespace = "http://www.w3.org/1999/xhtml")
    protected List<InterpolatedProperty> mapInterpolatedProperties;

    @XmlElement(
            name = "map-interpolated-geometry",
            required = false,
            namespace = "http://www.w3.org/1999/xhtml")
    protected InterpolatedGeometry mapInterpolatedGeometry;

    /**
     * Gets the value of the head property.
     *
     * @return possible object is {@link HeadContent }
     */
    public HeadContent getHead() {
        return head;
    }

    /**
     * Sets the value of the head property.
     *
     * @param value allowed object is {@link HeadContent }
     */
    public void setHead(HeadContent value) {
        this.head = value;
    }

    public List<InterpolatedProperty> getMapInterpolatedProperties() {
        return mapInterpolatedProperties;
    }

    public void setMapInterpolatedProperties(List<InterpolatedProperty> mapInterpolatedProperties) {
        this.mapInterpolatedProperties = mapInterpolatedProperties;
    }

    public InterpolatedGeometry getMapInterpolatedGeometry() {
        return mapInterpolatedGeometry;
    }

    public void setMapInterpolatedGeometry(InterpolatedGeometry mapInterpolatedGeometry) {
        this.mapInterpolatedGeometry = mapInterpolatedGeometry;
    }
}
