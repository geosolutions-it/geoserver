/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.gs.rasteralgebra;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.operator.BinarizeDescriptor;

import org.geoserver.wps.gs.GridCoverage2DRIA;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.util.Converters;
import org.geotools.util.Utilities;
import org.jaitools.imageutils.ImageLayout2;
import org.jaitools.media.jai.rangelookup.RangeLookupDescriptor;
import org.jaitools.media.jai.rangelookup.RangeLookupTable;
import org.jaitools.numeric.Range;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * {@link FilterVisitor} implementation that computes the processing chain
 * specified within the filter provided.
 * <p>
 * Right now we only implement {@link Not}, {@link Or}, {@link And},
 * {@link PropertyIsBetween}, {@link PropertyIsEqualTo},
 * {@link PropertyIsGreaterThan}, {@link PropertyIsGreaterThanOrEqualTo},
 * {@link PropertyIsLessThan}, {@link PropertyIsLessThanOrEqualTo} *
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
class CoverageProcessor extends DefaultFilterVisitor implements
        FilterVisitor {

private final RenderingHints hints;

private final HashMap<String, GridCoverage2D> sources;

private final GridGeometry2D finalGridGeometry2D;

/**
     * 
     */
public CoverageProcessor(final Map<String, GridCoverage2D> sourceCoverages,
        final GridGeometry2D finalGridGeometry2D, final RenderingHints hints) {
    // checks
    Utilities.ensureNonNull("sourceCoverages", sourceCoverages);
    Utilities.ensureNonNull("finalGridGeometry2D", finalGridGeometry2D);

    // assignments
    this.sources = new HashMap<String, GridCoverage2D>(sourceCoverages);
    this.finalGridGeometry2D = new GridGeometry2D(finalGridGeometry2D);
    this.hints = hints != null ? (RenderingHints) hints.clone() : GeoTools
            .getDefaultHints();
}

@Override
public Object visit(And filter, Object extraData) {
    // extract children RenderedImage instances
    final List<RenderedImage> results = extractRenderedImages(filter);

    // checks TODO
    final RenderedImage sources[] = results.toArray(new RenderedImage[results
            .size()]);
    assert sources.length > 0;
    return Operator.AND.process(sources, hints);
}

/**
 * Not unary operator
 */
@Override
public Object visit(Not filter, Object extraData) {

    // parse child
    Filter child = filter.getFilter();
    if (child != null) {
        extraData = child.accept(this, extraData);
    }

    // checks TODO
    final RenderedImage source = (RenderedImage) extraData;
    return Operator.NOT.process(new RenderedImage[] { source }, hints);
}

@Override
public Object visit(Or filter, Object extraData) {

    // extract children RenderedImage instances
    final List<RenderedImage> results = extractRenderedImages(filter);

    // checks TODO
    final RenderedImage sources[] = results.toArray(new RenderedImage[results
            .size()]);
    assert sources.length > 0;
    return Operator.OR.process(sources, hints);
}

/**
 * @param filter
 * @return
 * @throws IllegalStateException
 */
private List<RenderedImage> extractRenderedImages(BinaryLogicOperator filter)
        throws IllegalStateException {
    // parse children
    List<Filter> childList = filter.getChildren();
    final List<RenderedImage> results = new ArrayList<RenderedImage>();
    if (childList != null) {
        for (Filter child : childList) {
            if (child == null) {
                continue;
            }
            // get result from child
            Object result = child.accept(this, null);
            if (result instanceof RenderedImage) {
                results.add((RenderedImage) result);
            } else {
                throw new IllegalStateException(
                        "Null intermediate result, maybe an unsupported filter? "
                                + child.toString()); // TODO
            }
        }
    }
    return results;
}

@Override
public Object visit(PropertyIsBetween filter, Object data) {

    // parse values
    final Object lowerS = filter.getLowerBoundary().accept(this, data);
    final Object upperS = filter.getUpperBoundary().accept(this, data);
    final String name = (String) filter.getExpression().accept(this, data);

    // check values
    final double minValue = Converters.convert(lowerS, Double.class);
    final double maxValue = Converters.convert(upperS, Double.class);

    // fetch source image
    final RenderedImage source = fetchSourceImage(name);

    // create table
    return evaluateNumericBinaryComparisonOperator(minValue, true, maxValue,
            true, source);
}

/**
 * Evalutes numeric {@link BinaryComparisonOperator} producing a binary image.
 * <p>
 * Internally we apply a {@link RangeLookupDescriptor} operation using the
 * provided values. The result is then binarized using 1 as threshold.
 * 
 * @param minValue minimum value as per the operator
 * @param maxValue maximum value as per the operator
 * @param maxInclusive <code>true</code> if the maximum is included,
 *        <code>false</code> otherwise.
 * @param minInclusive <code>true</code> if the minimum is included,
 *        <code>false</code> otherwise.
 * @param source the input {@link RenderedImage}.
 * @return a binarized {@link RenderedImage}
 */
private RenderedImage evaluateNumericBinaryComparisonOperator(
        final double minValue, boolean minInclusive, final double maxValue,
        boolean maxInclusive, final RenderedImage source) {

    // === create RangeLookupTable
    final RangeLookupTable.Builder<Double, Byte> builder = new RangeLookupTable.Builder<Double, Byte>();
    builder.add(new Range<Double>(minValue, minInclusive, maxValue,
            maxInclusive), (byte) 1);
    RangeLookupTable<Double, Byte> table = builder.build();

    // === create rangelookup operation

    // create hints with color model
    Hints tempHints = (Hints) hints.clone();
    final ImageLayout layout;
    if (tempHints.containsKey(JAI.KEY_IMAGE_LAYOUT)) {
        layout = (ImageLayout) tempHints.get(JAI.KEY_IMAGE_LAYOUT);
    } else {
        layout = new ImageLayout2();
        layout.setTileHeight(JAI.getDefaultTileSize().height).setTileWidth(
                JAI.getDefaultTileSize().width);
    }

    // Sample Model
    // if(!ImageUtil.isBinary(source.getSampleModel())){
    // SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
    // layout.getTileWidth(source),
    // layout.getTileHeight(source),
    // 1);
    // layout.setSampleModel(sm);
    // layout.setColorModel(ImageUtil.getCompatibleColorModel(sm,Collections.emptyMap()));
    // ColorModel cm = new ComponentColorModel(
    // ColorSpace.getInstance(ColorSpace.CS_GRAY),
    // false,
    // false,
    // Transparency.OPAQUE,
    // DataBuffer.TYPE_BYTE);
    //
    // layout.setColorModel(cm);
    // layout.setSampleModel(cm.createCompatibleSampleModel(layout.getTileWidth(source),layout.getTileHeight(source)));
    // } else {
    // layout.setColorModel(source.getColorModel());
    // layout.setSampleModel(source.getSampleModel());
    // }
    // hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT,layout));

    // // Ignore any ImageLayout that was provided and create one here
    // ColorModel cm = new ComponentColorModel(
    // ColorSpace.getInstance(ColorSpace.CS_GRAY),
    // false,
    // false,
    // Transparency.OPAQUE,
    // DataBuffer.TYPE_SHORT);
    // layout.setColorModel(cm);
    // layout.setSampleModel(cm.createCompatibleSampleModel(layout.getTileWidth(source),layout.getTileHeight(source)));
    // hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT,layout));

    // operation
    ParameterBlockJAI pb = new ParameterBlockJAI("rangelookup");
    pb.setSource("source0", source);
    pb.setParameter("table", table);
    pb.setParameter("default", Byte.valueOf((byte) 0));
    final RenderedImage rangeLookup = JAI.create("rangelookup", pb, hints);

    // return result
    return BinarizeDescriptor.create(rangeLookup, 1.0, hints);
}

/**
 * @param name
 * @return
 * @throws IllegalArgumentException
 */
private RenderedImage fetchSourceImage(final String name)
        throws IllegalArgumentException {
    if (!sources.containsKey(name)) {
        throw new IllegalArgumentException(
                "Unable to find source coverage with name: " + name);
    }
    final GridCoverage2D gridCoverage = sources.get(name);
    final RenderedImage source = GridCoverage2DRIA.create(gridCoverage,
            this.finalGridGeometry2D,
            org.geotools.resources.coverage.CoverageUtilities
                    .getBackgroundValues(gridCoverage)[0]);
    return source;
}

@Override
public Object visit(PropertyIsEqualTo filter, Object data) {

    // extract from filters
    final String name = (String) filter.getExpression1().accept(this, data);
    final double value = Converters.convert(
            filter.getExpression2().accept(this, data), Double.class);

    final RenderedImage source = fetchSourceImage(name);

    return evaluateNumericBinaryComparisonOperator(value, true, value, true,
            source);
}

@Override
public Object visit(PropertyIsNotEqualTo filter, Object data) {

    // extract frm filters
    final String name = (String) filter.getExpression1().accept(this, data);
    final double value = Converters.convert(
            filter.getExpression2().accept(this, data), Double.class);

    final RenderedImage source = fetchSourceImage(name);

    // create table
    return evaluateNumericBinaryComparisonOperator(value, false,
            Double.POSITIVE_INFINITY, false, source);
}

@Override
public Object visit(PropertyIsGreaterThan filter, Object data) {
    final String name = (String) filter.getExpression1().accept(this, data);
    final double value = Converters.convert(
            filter.getExpression2().accept(this, data), Double.class);

    final RenderedImage source = fetchSourceImage(name);

    // create table
    return evaluateNumericBinaryComparisonOperator(value, false,
            Double.POSITIVE_INFINITY, false, source);
}

@Override
public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object data) {
    final String name = (String) filter.getExpression1().accept(this, data);
    final double value = Converters.convert(
            filter.getExpression2().accept(this, data), Double.class);

    final RenderedImage source = fetchSourceImage(name);

    // create table
    return evaluateNumericBinaryComparisonOperator(value, true,
            Double.POSITIVE_INFINITY, false, source);
}

@Override
public Object visit(PropertyIsLessThan filter, Object data) {
    final String name = (String) filter.getExpression1().accept(this, data);
    final double value = Converters.convert(
            filter.getExpression2().accept(this, data), Double.class);

    // fetch source image
    final RenderedImage source = fetchSourceImage(name);

    // create table
    return evaluateNumericBinaryComparisonOperator(Double.NEGATIVE_INFINITY,
            false, value, false, source);
}

@Override
public Object visit(PropertyIsLessThanOrEqualTo filter, Object data) {
    final String name = (String) filter.getExpression1().accept(this, data);
    final double value = Converters.convert(
            filter.getExpression2().accept(this, data), Double.class);

    final RenderedImage source = fetchSourceImage(name);

    // create table
    return evaluateNumericBinaryComparisonOperator(Double.NEGATIVE_INFINITY,
            true, value, false, source);
}

/**
 * Extracts single values.
 */
@Override
public Object visit(Literal expression, Object data) {
    return expression.getValue();
}

/**
 * Collects coverage names
 */
@Override
public Object visit(PropertyName expression, Object data) {
    return expression.getPropertyName();
}

/**
     * 
     */
public void dispose() {
    // TODO Auto-generated method stub

}

@Override
public Object visit(Function function, Object arg1) {
    Utilities.ensureNonNull("function", function);

    // parse function
    final List<Expression> params = function.getParameters();
    String functionName = function.getName();
    final List<String> inputs = new ArrayList<String>();
    for (Expression exp : params) {
        inputs.add((String) exp.accept(this, null));
    }

    // execute function
    Operator operator = Operator.valueOf(functionName.toUpperCase());
    final RenderedImage img[] = new RenderedImage[inputs.size()];
    for (int i = 0; i < img.length; i++) {
        img[i] = fetchSourceImage(inputs.get(i));
    }
    // execute operator
    return operator.process(img, hints);
}
}
