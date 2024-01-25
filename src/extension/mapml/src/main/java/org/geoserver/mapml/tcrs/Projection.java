/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.NoninvertibleTransformException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;

/*
 * The Projection class supplies projection/unprojection transformations for known projections
 */

/** @author prushforth */
public class Projection {

    protected CoordinateReferenceSystem crs;
    protected CoordinateReferenceSystem baseCRS;
    protected boolean baseLatLon;

    protected MathTransform toProjected;
    protected MathTransform toLatLng;

    /** @param code */
    public Projection(String code) {
        try {
            this.crs = CRS.decode(code);
            this.baseCRS =
                    CRS.getProjectedCRS(crs) != null
                            ? CRS.getProjectedCRS(crs).getBaseCRS()
                            : this.crs;
            this.baseLatLon = CRS.getAxisOrder(this.baseCRS) == CRS.AxisOrder.NORTH_EAST;
            this.toProjected = CRS.findMathTransform(this.baseCRS, crs);
            this.toLatLng = this.toProjected.inverse();
        } catch (FactoryException | NoninvertibleTransformException ex) {
            Logger.getLogger(Projection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param latlng
     * @return
     * @throws MismatchedDimensionException - MismatchedDimensionException
     * @throws TransformException - TransformException
     */
    public Point project(LatLng latlng) throws MismatchedDimensionException, TransformException {
        if (toProjected.isIdentity()) {
            return toPoint(latlng);
        }
        Position2D projected = new Position2D(crs);
        toProjected.transform(toPosition2D(latlng), projected);
        return new Point(projected.x, projected.y);
    }

    /**
     * @param p
     * @return
     * @throws MismatchedDimensionException - MismatchedDimensionException
     * @throws TransformException - TransformException
     */
    public LatLng unproject(Point p) throws MismatchedDimensionException, TransformException {
        if (toLatLng.isIdentity()) {
            return toLatLng(p);
        }
        Position2D unprojected = new Position2D(this.baseCRS);
        toLatLng.transform(new Position2D(this.crs, p.x, p.y), unprojected);
        return toLatLon(unprojected);
    }

    private LatLng toLatLng(Point p) {
        if (baseLatLon) return new LatLng(p.x, p.y);
        else return new LatLng(p.y, p.x);
    }

    private LatLng toLatLon(Position2D unprojected) {
        if (baseLatLon) return new LatLng(unprojected.getOrdinate(0), unprojected.getOrdinate(1));
        else return new LatLng(unprojected.getOrdinate(1), unprojected.getOrdinate(0));
    }

    private Point toPoint(LatLng latlng) {
        if (baseLatLon) return new Point(latlng.lat, latlng.lng);
        else return new Point(latlng.lng, latlng.lat);
    }

    private Position2D toPosition2D(LatLng latlng) {
        if (baseLatLon) return new Position2D(this.baseCRS, latlng.lat, latlng.lng);
        else return new Position2D(this.baseCRS, latlng.lng, latlng.lat);
    }

    /** @return */
    public CoordinateReferenceSystem getCRS() {
        return this.crs;
    }
}
