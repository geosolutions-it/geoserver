package org.geoserver.mapml.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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

    @XmlElements({
        @XmlElement(name = "point", type = Point.class, namespace = "http://www.w3.org/1999/xhtml"),
        @XmlElement(
                name = "map-linestring",
                type = LineString.class,
                namespace = "http://www.w3.org/1999/xhtml"),
        @XmlElement(
                name = "map-polygon",
                type = Polygon.class,
                namespace = "http://www.w3.org/1999/xhtml"),
        @XmlElement(
                name = "map-multipoint",
                type = MultiPoint.class,
                namespace = "http://www.w3.org/1999/xhtml"),
        @XmlElement(
                name = "map-multilinestring",
                type = MultiLineString.class,
                namespace = "http://www.w3.org/1999/xhtml"),
        @XmlElement(
                name = "map-multipolygon",
                type = MultiPolygon.class,
                namespace = "http://www.w3.org/1999/xhtml")
    })
    @XmlElement(name = "map-components", namespace = "http://www.w3.org/1999/xhtml")
    protected List<Object> components;

    public List<Object> getComponents() {
        if (components == null) {
            components = new ArrayList<>();
        }
        return components;
    }

    @XmlElement(name = "map-coordinates", namespace = "http://www.w3.org/1999/xhtml")
    protected List<String> coordinates;

    public List<String> getCoordinates() {
        if (coordinates == null) {
            coordinates = new ArrayList<>();
        }
        return this.coordinates;
    }
}
