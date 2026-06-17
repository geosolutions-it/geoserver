/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.media.jai.Interpolation;
import org.geoserver.catalog.Predicates;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.coverage.processing.Operation;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.parameter.GeneralParameterDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterNotFoundException;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.coverage.processing.operation.Scale;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;

/**
 * Applies access limits policies around the wrapped reader
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredGridCoverage2DReader extends DecoratingGridCoverage2DReader {

    /** Parameters used to control the {@link Crop} operation. */
    private static final ParameterValueGroup cropParams;

    /** Cached crop factory */
    private static final Crop coverageCropFactory = new Crop();

    /** Cached scale factory */
    private static final Scale coverageScaleFactory = new Scale();

    static {
        final CoverageProcessor processor = new CoverageProcessor(new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
        cropParams = processor.getOperation("CoverageCrop").getParameters();
    }

    WrapperPolicy policy;

    public SecuredGridCoverage2DReader(GridCoverage2DReader delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public Format getFormat() {
        Format format = delegate.getFormat();
        if (format == null) {
            return null;
        } else {
            return SecuredObjects.secure(format, policy);
        }
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IllegalArgumentException, IOException {
        return SecuredGridCoverage2DReader.read(delegate, policy, parameters);
    }

    static GridCoverage2D read(GridCoverage2DReader delegate, WrapperPolicy policy, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        // Package private static method to share reading code with Structured reader
        MultiPolygon rasterFilter = null;
        if (policy.getLimits() instanceof CoverageAccessLimits) {
            CoverageAccessLimits limits = (CoverageAccessLimits) policy.getLimits();

            // get the crop filter
            rasterFilter = limits.getRasterFilter();
            Filter readFilter = limits.getReadFilter();

            // update the read params
            final GeneralParameterValue[] limitParams = limits.getParams();
            if (parameters == null) {
                parameters = limitParams;
            } else if (limitParams != null) {
                // scan the input params, add and overwrite with the limits params as needed
                List<GeneralParameterValue> params = new ArrayList<>(Arrays.asList(parameters));
                for (GeneralParameterValue lparam : limitParams) {
                    // remove the overwritten param, if any
                    for (Iterator it = params.iterator(); it.hasNext(); ) {
                        GeneralParameterValue param = (GeneralParameterValue) it.next();
                        if (param.getDescriptor().equals(lparam.getDescriptor())) {
                            it.remove();
                            break;
                        }
                    }
                    // add the overwrite param (will be an overwrite if it was already there, an
                    // addition otherwise)
                    params.add(lparam);
                }

                parameters = params.toArray(new GeneralParameterValue[params.size()]);
            }

            if (readFilter != null && !Filter.INCLUDE.equals(readFilter)) {
                Format format = delegate.getFormat();
                ParameterValueGroup readParameters = format.getReadParameters();
                List<GeneralParameterDescriptor> descriptors =
                        readParameters.getDescriptor().descriptors();

                // scan all the params looking for the one we want to add
                boolean replacedOriginalFilter = false;
                for (GeneralParameterValue pv : parameters) {
                    String pdCode = pv.getDescriptor().getName().getCode();
                    if ("FILTER".equals(pdCode) || "Filter".equals(pdCode)) {
                        replacedOriginalFilter = true;
                        ParameterValue pvalue = (ParameterValue) pv;
                        Filter originalFilter = (Filter) pvalue.getValue();
                        if (originalFilter == null || Filter.INCLUDE.equals(originalFilter)) {
                            pvalue.setValue(readFilter);
                        } else {
                            Filter combined = Predicates.and(originalFilter, readFilter);
                            pvalue.setValue(combined);
                        }
                    }
                }
                if (!replacedOriginalFilter) {
                    parameters = CoverageUtils.mergeParameter(descriptors, parameters, readFilter, "FILTER", "Filter");
                }
            }
        }

        GridCoverage2D grid = delegate.read(parameters);

        // crop if necessary
        if (rasterFilter != null && grid != null) {
            Geometry coverageBounds = JTS.toGeometry((Envelope) new ReferencedEnvelope(grid.getEnvelope2D()));
            if (coverageBounds.intersects(rasterFilter)) {
                Interpolation interpolation = null;
                for (GeneralParameterValue pv : parameters) {
                    String pdCode = pv.getDescriptor().getName().getCode();
                    if ("Interpolation".equals(pdCode)) {
                        ParameterValue pvalue = (ParameterValue) pv;
                        interpolation = (Interpolation) pvalue.getValue();
                        break;
                    }
                }

                // The underlying reader may have returned a coverage with a larger envelope than the one requested
                grid = cropToEnvelope(
                        grid,
                        new ReferencedEnvelope(
                                rasterFilter.getEnvelopeInternal(), grid.getCoordinateReferenceSystem2D()));

                // The underlying reader may have returned a coverage with a different resolution than the one
                // requested. The requested gridGeometry may have been limited too, due to reaching the
                // Max Oversampling Factor.
                //
                // This happens for example when the data resolution is bad and the map is heavily oversampled.
                // We want to scale it to the requested map raster extent before cropping to the geometry.

                grid = scaleToRequestedSize(grid, getRequestedMapRasterArea(), interpolation);
                if (grid != null) {
                    grid = cropToGeometry(grid, rasterFilter);
                }
            } else {
                return null;
            }
        }
        return grid;
    }

    private static Rectangle getRequestedMapRasterArea() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null
                || request.getOperation() == null
                || request.getOperation().getParameters() == null) {
            return null;
        }

        for (Object parameter : request.getOperation().getParameters()) {
            Rectangle mapExtent = getRequestedMapRasterArea(parameter);
            if (mapExtent != null) {
                return mapExtent;
            }
        }
        return null;
    }

    private static Rectangle getRequestedMapRasterArea(Object parameter) {
        if (parameter == null) {
            return null;
        }

        Integer width = getProperty(parameter, "width", Integer.class);
        Integer height = getProperty(parameter, "height", Integer.class);
        if (width == null || height == null) {
            return null;
        }
        return new Rectangle(width, height);
    }

    private static <T> T getProperty(Object object, String property, Class<T> type) {
        if (!OwsUtils.has(object, property)) {
            return null;
        }
        return OwsUtils.property(object, property, type);
    }

    private static GridCoverage2D scaleToRequestedSize(
            GridCoverage2D grid, Rectangle requestedGridRange, Interpolation interpolation) {
        if (requestedGridRange == null) {
            return grid;
        }

        RenderedImage image = grid.getRenderedImage();
        int width = image.getWidth();
        int height = image.getHeight();
        int requestedWidth = (int) requestedGridRange.getWidth();
        int requestedHeight = (int) requestedGridRange.getHeight();
        if (width <= 0 || height <= 0 || requestedWidth <= 0 || requestedHeight <= 0) {
            return grid;
        }

        double xScale = requestedWidth / (double) width;
        double yScale = requestedHeight / (double) height;
        if (xScale == 1d && yScale == 1d) {
            return grid;
        }

        Operation scaleOperation = CoverageProcessor.getInstance().getOperation("Scale");
        ParameterValueGroup param = scaleOperation.getParameters();
        param.parameter("Source").setValue(grid);
        param.parameter("xScale").setValue(xScale);
        param.parameter("yScale").setValue(yScale);
        param.parameter("xTrans").setValue(0.0);
        param.parameter("yTrans").setValue(0.0);
        setScaleInterpolation(param, interpolation);
        return (GridCoverage2D) coverageScaleFactory.doOperation(param, null);
    }

    private static void setScaleInterpolation(ParameterValueGroup param, Interpolation interpolation) {
        if (interpolation == null) {
            return;
        }
        try {
            param.parameter("Interpolation").setValue(interpolation);
        } catch (ParameterNotFoundException e) {
            param.parameter("InterpolationType").setValue(interpolation);
        }
    }

    private static GridCoverage2D cropToEnvelope(GridCoverage2D grid, ReferencedEnvelope envelope) {
        ReferencedEnvelope coverageBounds = new ReferencedEnvelope(grid.getEnvelope2D());
        ReferencedEnvelope intersection = envelope.intersection(coverageBounds);
        if (intersection.isEmpty()) {
            return null;
        }

        final ParameterValueGroup param = cropParams.clone();
        param.parameter("Source").setValue(grid);
        param.parameter("Envelope").setValue(intersection);
        return (GridCoverage2D) coverageCropFactory.doOperation(param, null);
    }

    private static GridCoverage2D cropToGeometry(GridCoverage2D grid, Geometry rasterFilter) {
        final ParameterValueGroup param = cropParams.clone();
        param.parameter("Source").setValue(grid);
        param.parameter("ROI").setValue(rasterFilter);
        return (GridCoverage2D) coverageCropFactory.doOperation(param, null);
    }

    @Override
    public ServiceInfo getInfo() {
        ServiceInfo info = delegate.getInfo();
        if (info == null) {
            return null;
        } else {
            return SecuredObjects.secure(info, policy);
        }
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        ResourceInfo info = delegate.getInfo(coverageName);
        if (info == null) {
            return null;
        } else {
            return SecuredObjects.secure(info, policy);
        }
    }
}
