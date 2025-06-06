//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2018.12.17 at 04:13:52 PM PST
//

package org.geoserver.mapml.xml;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attGroup ref="{}ImageResourceMetadataAttributes"/&gt;
 *       &lt;attribute name="col" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" /&gt;
 *       &lt;attribute name="row" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "map-tile", namespace = "http://www.w3.org/1999/xhtml")
public class Tile implements Comparable<Tile> {

    @XmlAttribute(name = "col", required = true)
    protected BigInteger col;

    @XmlAttribute(name = "row", required = true)
    protected BigInteger row;

    @XmlAttribute(name = "zoom")
    protected BigInteger zoom;

    @XmlAttribute(name = "src", required = true)
    @XmlSchemaType(name = "anySimpleType")
    protected String src;

    @XmlAttribute(name = "x")
    protected Double x;

    @XmlAttribute(name = "y")
    protected Double y;

    @XmlAttribute(name = "width")
    protected BigInteger width;

    @XmlAttribute(name = "height")
    protected BigInteger height;

    @XmlAttribute(name = "angle")
    protected Double angle;

    @XmlAttribute(name = "type")
    @XmlSchemaType(name = "anySimpleType")
    protected String type;

    @XmlTransient
    protected Double distance;

    /**
     * Gets the value of the col property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getCol() {
        return col;
    }

    /**
     * Sets the value of the col property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setCol(BigInteger value) {
        this.col = value;
    }

    /**
     * Gets the value of the row property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getRow() {
        return row;
    }

    /**
     * Sets the value of the row property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setRow(BigInteger value) {
        this.row = value;
    }

    /**
     * Gets the value of the src property.
     *
     * @return possible object is {@link String }
     */
    public String getSrc() {
        return src;
    }

    /**
     * Sets the value of the src property.
     *
     * @param value allowed object is {@link String }
     */
    public void setSrc(String value) {
        this.src = value;
    }

    /**
     * Gets the value of the x property.
     *
     * @return possible object is {@link Double }
     */
    public Double getX() {
        return x;
    }

    /**
     * Sets the value of the x property.
     *
     * @param value allowed object is {@link Double }
     */
    public void setX(Double value) {
        this.x = value;
    }

    /**
     * Gets the value of the y property.
     *
     * @return possible object is {@link Double }
     */
    public Double getY() {
        return y;
    }

    /**
     * Sets the value of the y property.
     *
     * @param value allowed object is {@link Double }
     */
    public void setY(Double value) {
        this.y = value;
    }

    /**
     * Gets the value of the width property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getWidth() {
        return width;
    }

    /**
     * Sets the value of the width property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setWidth(BigInteger value) {
        this.width = value;
    }

    /**
     * Gets the value of the height property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getHeight() {
        return height;
    }

    /**
     * Sets the value of the height property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setHeight(BigInteger value) {
        this.height = value;
    }

    /**
     * Gets the value of the angle property.
     *
     * @return possible object is {@link Double }
     */
    public Double getAngle() {
        return angle;
    }

    /**
     * Sets the value of the angle property.
     *
     * @param value allowed object is {@link Double }
     */
    public void setAngle(Double value) {
        this.angle = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return possible object is {@link String }
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is {@link String }
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the zoom property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getZoom() {
        return zoom;
    }

    /**
     * Sets the value of the zoom property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setZoom(BigInteger value) {
        this.zoom = value;
    }

    /**
     * Gets the value of the distance property.
     *
     * @return value allowed object is {@link Double}
     */
    @XmlTransient
    public Double getDistance() {
        return distance;
    }

    /**
     * Sets the value of the distance property.
     *
     * @param value
     */
    public void setDistance(Double value) {
        this.distance = value;
    }

    @Override
    public int compareTo(Tile o) {
        if (this.distance == null && o.getDistance() == null) {
            return 0; // Both distances are null, treat as equal
        } else if (this.distance == null) {
            return -1; // Null is considered less than any non-null value
        } else if (o.getDistance() == null) {
            return 1; // Non-null is considered greater than null
        } else {
            return this.distance.compareTo(o.getDistance());
        }
    }
}
