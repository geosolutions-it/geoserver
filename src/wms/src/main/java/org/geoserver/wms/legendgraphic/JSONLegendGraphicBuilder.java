/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.unit.Unit;

import net.sf.json.util.JSONBuilder;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StyledShapePainter;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.Description;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.MultiValuedFilter.MatchAction;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.spatial.SpatialOperator;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.style.GraphicalSymbol;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Template {@linkPlain org.vfny.geoserver.responses.wms.GetLegendGraphicProducer} based on GeoTools' {@link GeoTools' {@link http
 * ://svn.geotools.org/geotools/trunk/gt/module/main/src/org/geotools /renderer/lite/StyledShapePainter.java StyledShapePainter} that produces a
 * BufferedImage with the appropiate legend graphic for a given GetLegendGraphic WMS request.
 * 
 * <p>
 * It should be enough for a subclass to implement {@linkPlain org.vfny.geoserver.responses.wms.GetLegendGraphicProducer#writeTo(OutputStream)} and
 * <code>getContentType()</code> in order to encode the BufferedImage produced by this class to the appropiate output format.
 * </p>
 * 
 * <p>
 * This class takes literally the fact that the arguments <code>WIDTH</code> and <code>HEIGHT</code> are just <i>hints</i> about the desired
 * dimensions of the produced graphic, and the need to produce a legend graphic representative enough of the SLD style for which it is being
 * generated. Thus, if no <code>RULE</code> parameter was passed and the style has more than one applicable Rule for the actual scale factor, there
 * will be generated a legend graphic of the specified width, but with as many stacked graphics as applicable rules were found, providing by this way
 * a representative enough legend.
 * </p>
 * 
 * @author Carlo Cancellieri, GeoSolutions SAS
 * @version $Id$
 */
public class JSONLegendGraphicBuilder {

    /** Tolerance used to compare doubles for equality */
    public static final double TOLERANCE = 1e-6;

    /**
     * Singleton shape painter to serve all legend requests. We can use a single shape painter instance as long as it remains thread safe.
     */
    private static final StyledShapePainter shapePainter = new StyledShapePainter();

    /**
     * used to create sample point shapes with LiteShape (not lines nor polygons)
     */
    private static final GeometryFactory geomFac = new GeometryFactory();

    /**
     * set to <code>true</code> when <code>abort()</code> gets called, indicates that the rendering of the legend graphic should stop gracefully as
     * soon as possible
     */
    private boolean renderingStopRequested;

    /**
     * Just a holder to avoid creating many polygon shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleRect;

    /**
     * Just a holder to avoid creating many line shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleLine;

    /**
     * Just a holder to avoid creating many point shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 samplePoint;

    /**
     * Default constructor. Subclasses may provide its own with a String parameter to establish its desired output format, if they support more than
     * one (e.g. a JAI based one)
     */
    public JSONLegendGraphicBuilder() {
        super();
    }

    public static LegendGraphicModel buildLegendGraphic(GetLegendGraphicRequest request)
            throws ServiceException {
        return new LegendGraphicModel(request);
    }

    /**
     * Takes a GetLegendGraphicRequest and produces a BufferedImage that then can be used by a subclass to encode it to the appropiate output format.
     * 
     * @param request the "parsed" request, where "parsed" means that it's values are already validated so this method must not take care of verifying
     *        the requested layer exists and the like.
     * @return
     * 
     * @throws ServiceException if there are problems creating a "sample" feature instance for the FeatureType <code>request</code> returns as the
     *         required layer (which should not occur).
     */
    public static void buildLegendGraphic(Writer w, LegendGraphicModel model)
            throws ServiceException {

        GetLegendGraphicRequest request = model.getRequest();
        // the style we have to build a legend for
        Style gt2Style = request.getStyle();
        if (gt2Style == null) {
            throw new NullPointerException("request.getStyle()");
        }

        // // width and height, we might have to rescale those in case of DPI usage
        // int w = request.getWidth();
        // int h = request.getHeight();
        //
        // // apply dpi rescale
        // double dpi = RendererUtilities.getDpi(request.getLegendOptions());
        // double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        // if(dpi != standardDpi) {
        // double scaleFactor = dpi / standardDpi;
        // w = (int) Math.round(w * scaleFactor);
        // h = (int) Math.round(h * scaleFactor);
        // RescaleStyleVisitor dpiVisitor = new RescaleStyleVisitor(scaleFactor);
        // dpiVisitor.visit(gt2Style);
        // gt2Style = (Style) dpiVisitor.getCopy();
        // }
        // apply UOM rescaling if we have a scale
        if (request.getScale() > 0) {
            double pixelsPerMeters = RendererUtilities.calculatePixelsPerMeterRatio(
                    request.getScale(), request.getLegendOptions());
            UomRescaleStyleVisitor rescaleVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
            rescaleVisitor.visit(gt2Style);
            gt2Style = (Style) rescaleVisitor.getCopy();
        }

        final FeatureType layer = request.getLayer();
        boolean strict = request.isStrict();
        final boolean buildRasterLegend = (!strict && layer == null && LegendUtils
                .checkRasterSymbolizer(gt2Style)) || LegendUtils.checkGridLayer(layer);
        // if (buildRasterLegend) {
        // final RasterLayerLegendHelper rasterLegendHelper = new RasterLayerLegendHelper(request);
        // final BufferedImage image = rasterLegendHelper.getLegend();
        // return image;
        // }

        // final SimpleFeature sampleFeature;
        final Feature sampleFeature;
        if (layer == null) {
            sampleFeature = createSampleFeature();
        } else {
            sampleFeature = createSampleFeature(layer);
        }
        final FeatureTypeStyle[] ftStyles = gt2Style.featureTypeStyles().toArray(
                new FeatureTypeStyle[0]);
        final double scaleDenominator = request.getScale();

        final Rule[] applicableRules;
        String ruleName = request.getRule();
        if (ruleName != null) {
            Rule rule = LegendUtils.getRule(ftStyles, ruleName);
            if (rule == null) {
                throw new ServiceException("Specified style does not contains a rule named "
                        + ruleName);
            }
            applicableRules = new Rule[] { rule };
        } else {
            applicableRules = LegendUtils.getApplicableRules(ftStyles, scaleDenominator);
        }

        // final NumberRange<Double> scaleRange = NumberRange.create(scaleDenominator,
        // scaleDenominator);

        /**
         * A legend graphic is produced for each applicable rule. They're being held here until the process is done and then painted on a "stack" like
         * legend.
         */
        JSONBuilder json = new JSONBuilder(w);

        // final SLDStyleFactory styleFactory = new SLDStyleFactory();

        json.object().key("getLegendGraphic");
        json.array();
        final int ruleCount = applicableRules.length;
        for (int i = 0; i < ruleCount; i++) {

            json.object();
            Description desc = applicableRules[i].getDescription();
            if (desc != null) {
                json.key("description").value(desc.getAbstract());
                json.key("title").value(desc.getTitle());
            }
            json.key("name").value(applicableRules[i].getName());
            Filter filter = applicableRules[i].getFilter();
            writeFilter(filter,json);

            // json.key("").value(applicableRules[i].)
            // json.key("").value(applicableRules[i].)
            // json.key("").value(applicableRules[i].)
            json.key("symbolyzers");
            json.array();

            final Symbolizer[] symbolizers = applicableRules[i].getSymbolizers();
            for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                final Symbolizer symbolizer = symbolizers[sIdx];

                json.object();

                json.key("name").value(applicableRules[i].getName());

                Description symDesc = applicableRules[i].getDescription();
                if (desc != null) {
                    json.key("description").value(symDesc.getAbstract());
                    json.key("title").value(symDesc.getTitle());
                }

                if (symbolizer instanceof RasterSymbolizer) {
                    RasterSymbolizer rSymbolizer = ((RasterSymbolizer) symbolizer);
                    json.key("type").value("Raster");

                    ColorMap colormap = rSymbolizer.getColorMap();
                    if (colormap != null) {
                        json.key("colorMap");
                        json.array();
                        for (ColorMapEntry c : colormap.getColorMapEntries()) {
                            json.object();
                            json.key("label").value(c.getLabel());
                            json.key("opacity").value(c.getOpacity());
                            json.key("quantity").value(c.getQuantity());
                            json.key("color").value(c.getColor());
                            json.endObject();
                        }
                        json.endArray();

                        // Function function=colormap.getFunction();
                        // if (function!=null){
                        // json.object();
                        // json.key("functionName").value(colormap.getFunction().getName());
                        // json.endObject();
                        // }
                    }
                    // rSymbolizer.getChannelSelection()

                } else if (symbolizer instanceof PolygonSymbolizer) {
                    PolygonSymbolizer pSymbolizer = ((PolygonSymbolizer) symbolizer);
                    json.key("Polygon");
                    json.object();
                    Fill fill = pSymbolizer.getFill();
                    writeFill(fill, json);

                    json.key("perpendicularOffset").value(pSymbolizer.getPerpendicularOffset());
                    Stroke stroke = pSymbolizer.getStroke();
                    writeStroke(stroke, json);
                    json.endObject();
                } else if (symbolizer instanceof TextSymbolizer) {
                    TextSymbolizer tSymbolizer = ((TextSymbolizer) symbolizer);
                    json.key("Text");
                    json.object();
                    Fill fill = tSymbolizer.getFill();
                    writeFill(fill, json);

                    Halo halo = tSymbolizer.getHalo();
                    if (halo != null) {
                        json.key("halo").value(true);
                        Fill hFill = halo.getFill();
                        if (hFill != null) {
                            json.key("haloColor").value(hFill.getColor());
                            json.key("haloOpacity").value(hFill.getOpacity());
                        }
                        json.key("haloRadius").value(halo.getRadius());

                    }
                    json.endObject();
                } else if (symbolizer instanceof LineSymbolizer) {
                    LineSymbolizer lSymbolizer = ((LineSymbolizer) symbolizer);
                    json.key("Line");
                    json.object();
                    json.key("perpendicularOffset").value(lSymbolizer.getPerpendicularOffset());

                    Stroke stroke = lSymbolizer.getStroke();
                    if (stroke != null) {
                        json.key("stroke").value(true);
                        json.key("strokeColor").value(stroke.getColor());

                        writeGraphic(stroke.getGraphicFill(), json, "fill");

                        writeGraphic(stroke.getGraphicStroke(), json, "stroke");

                        json.key("dashArray").value(Arrays.toString(stroke.getDashArray()));

                        // TODO continue
                    }

                    json.endObject();
                } else {

                    json.key("type").value(symbolizer.getClass().getSimpleName());

                }

                json.key("geometry").value(symbolizer.getGeometry());
                json.key("geometryPropertyName").value(symbolizer.getGeometryPropertyName());
                Unit<?> unit = symbolizer.getUnitOfMeasure();
                if (unit != null) {
                    json.key("UOM").value(unit.toString());
                }

                json.endObject();
            }
            json.endArray();
            json.endObject();
            // legendsStack.add(image);
            // graphics.dispose();
        }
        json.endArray();
        json.endObject();
        // JD: changed legend behavior, see GEOS-812
        // this.legendGraphic = scaleImage(mergeLegends(legendsStack), request);
        // BufferedImage image = mergeLegends(legendsStack, applicableRules, request);
    }

    private static void writeGraphic(Graphic gFill, JSONBuilder json, String prefix) {
        if (gFill != null) {
            json.object();
            json.key(prefix + "Size").value(gFill.getSize());
            json.key(prefix + "Rotation").value(gFill.getRotation());
            json.key(prefix + "Opacity").value(gFill.getOpacity());
            List<GraphicalSymbol> gSymbols = gFill.graphicalSymbols();
            if (gSymbols != null) {
                json.key(prefix + "GraphicalSymbols");
                json.array();
                for (GraphicalSymbol gs : gSymbols) {
                    json.key("GraphicalSymbol").value(gs.toString());
                }
                json.endArray();
            }
            json.endObject();
        }
    }

    private static void writeFill(Fill fill, JSONBuilder json) {
        if (fill != null) {
            json.key("fill").value(true);
            json.key("fillColor").value(fill.getColor());
            json.key("fillOpacity").value(fill.getOpacity());
            Graphic gFill = fill.getGraphicFill();
            writeGraphic(gFill, json, "fill");
        }
    }

    private static void writeStroke(Stroke stroke, JSONBuilder json) {
        if (stroke != null) {
            json.key("strokeColor").value(stroke.getColor());

            writeGraphic(stroke.getGraphicFill(), json, "strokeFill");

            writeGraphic(stroke.getGraphicStroke(), json, "strokeGraphic");

            json.key("strokeDashArray").value(Arrays.toString(stroke.getDashArray()));
            json.key("strokeDashOffset").value(stroke.getDashOffset());

            // TODO continue
        }
    }
    
    private static void writeFilter(Filter filter, JSONBuilder json) {
        if (filter != null) {
//            json.key("filter").value(filter.toString());
            json.key("filter");
            json.object();
            if (BinaryComparisonOperator.class.isAssignableFrom(filter.getClass())){
                BinaryComparisonOperator b=(BinaryComparisonOperator)filter;
                b.getMatchAction();
                b.getExpression1();
                b.getExpression2();
                b.isMatchingCase();
            } else if (BinaryTemporalOperator.class.isAssignableFrom(filter.getClass())){
                BinaryTemporalOperator b=(BinaryTemporalOperator)filter;
                b.getMatchAction();
                b.getExpression1();
                b.getExpression2();
            } else if (PropertyIsBetween.class.isAssignableFrom(filter.getClass())){
                PropertyIsBetween b=(PropertyIsBetween)filter;
                b.getMatchAction();
                b.getLowerBoundary();
                b.getUpperBoundary();
            } else if (PropertyIsLike.class.isAssignableFrom(filter.getClass())){
                PropertyIsLike b=(PropertyIsLike)filter;
                b.getMatchAction();
                b.getEscape();
                b.getLiteral();
                MatchAction ma=b.getMatchAction();
            } else if (SpatialOperator.class.isAssignableFrom(filter.getClass())){
                // TODO subtypes
                SpatialOperator b=(SpatialOperator)filter;
                b.getMatchAction();
//                b.getEscape();
//                MatchAction ma=b.getMatchAction();
            } else if (PropertyIsLike.class.isAssignableFrom(filter.getClass())){
                PropertyIsLike b=(PropertyIsLike)filter;
                b.getMatchAction();
                b.getEscape();
                b.getLiteral();
                MatchAction ma=b.getMatchAction();
            }
            json.endObject();
        }
    }

    /**
     * Recieves a list of <code>BufferedImages</code> and produces a new one which holds all the images in <code>imageStack</code> one above the
     * other.
     * 
     * @param imageStack the list of BufferedImages, one for each applicable Rule
     * @param rules The applicable rules, one for each image in the stack
     * @param request The request.
     * 
     * @return the stack image with all the images on the argument list.
     * 
     * @throws IllegalArgumentException if the list is empty
     */
    private static BufferedImage mergeLegends(List<RenderedImage> imageStack, Rule[] rules,
            GetLegendGraphicRequest req) {

        Font labelFont = LegendUtils.getLabelFont(req);
        boolean useAA = LegendUtils.isFontAntiAliasing(req);

        boolean forceLabelsOn = false;
        boolean forceLabelsOff = false;
        if (req.getLegendOptions().get("forceLabels") instanceof String) {
            String forceLabelsOpt = (String) req.getLegendOptions().get("forceLabels");
            if (forceLabelsOpt.equalsIgnoreCase("on")) {
                forceLabelsOn = true;
            } else if (forceLabelsOpt.equalsIgnoreCase("off")) {
                forceLabelsOff = true;
            }
        }

        if (imageStack.size() == 0) {
            throw new IllegalArgumentException("No legend graphics passed");
        }

        final BufferedImage finalLegend;

        if (imageStack.size() == 1 && !forceLabelsOn) {
            finalLegend = (BufferedImage) imageStack.get(0);
        } else {
            final int imgCount = imageStack.size();
            final String[] labels = new String[imgCount];

            BufferedImage img = ((BufferedImage) imageStack.get(0));

            int totalHeight = 0;
            int totalWidth = 0;
            int[] rowHeights = new int[imgCount];
            BufferedImage labelsGraphics[] = new BufferedImage[imgCount];
            for (int i = 0; i < imgCount; i++) {
                img = (BufferedImage) imageStack.get(i);

                if (forceLabelsOff) {
                    totalWidth = (int) Math.ceil(Math.max(img.getWidth(), totalWidth));
                    rowHeights[i] = img.getHeight();
                    totalHeight += img.getHeight();
                } else {

                    Rule rule = rules[i];

                    // What's the label on this rule? We prefer to use
                    // the 'title' if it's available, but fall-back to 'name'
                    final Description description = rule.getDescription();
                    if (description != null && description.getTitle() != null) {
                        final InternationalString title = description.getTitle();
                        labels[i] = title.toString();
                    } else if (rule.getName() != null) {
                        labels[i] = rule.getName();
                    } else {
                        labels[i] = "";
                    }

                    Graphics2D g = img.createGraphics();
                    g.setFont(labelFont);

                    if (useAA) {
                        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    } else {
                        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                    }

                    if (labels[i] != null && labels[i].length() > 0) {
                        final BufferedImage renderedLabel = LegendUtils.renderLabel(labels[i], g,
                                req);
                        labelsGraphics[i] = renderedLabel;
                        final Rectangle2D bounds = new Rectangle2D.Double(0, 0,
                                renderedLabel.getWidth(), renderedLabel.getHeight());

                        totalWidth = (int) Math.ceil(Math.max(img.getWidth() + bounds.getWidth(),
                                totalWidth));
                        rowHeights[i] = (int) Math.ceil(Math.max(img.getHeight(),
                                bounds.getHeight()));
                    } else {
                        totalWidth = (int) Math.ceil(Math.max(img.getWidth(), totalWidth));
                        rowHeights[i] = (int) Math.ceil(img.getHeight());
                        labelsGraphics[i] = null;
                    }

                    totalHeight += rowHeights[i];

                }
            }

            // buffer the width a bit
            totalWidth += 2;

            final boolean transparent = req.isTransparent();
            final Color backgroundColor = LegendUtils.getBackgroundColor(req);
            final Map<RenderingHints.Key, Object> hintsMap = new HashMap<RenderingHints.Key, Object>();
            // create the final image
            finalLegend = ImageUtils.createImage(totalWidth, totalHeight, (IndexColorModel) null,
                    transparent);
            Graphics2D finalGraphics = ImageUtils.prepareTransparency(transparent, backgroundColor,
                    finalLegend, hintsMap);

            int topOfRow = 0;

            for (int i = 0; i < imgCount; i++) {
                img = (BufferedImage) imageStack.get(i);

                // draw the image
                int y = topOfRow;

                if (img.getHeight() < rowHeights[i]) {
                    // move the image to the center of the row
                    y += (int) ((rowHeights[i] - img.getHeight()) / 2d);
                }

                finalGraphics.drawImage(img, 0, y, null);
                if (forceLabelsOff) {
                    topOfRow += rowHeights[i];
                    continue;
                }

                finalGraphics.setFont(labelFont);

                if (useAA) {
                    finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                } else {
                    finalGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                }

                // draw the label
                if (labels[i] != null && labels[i].length() > 0) {
                    // first create the actual overall label image.
                    final BufferedImage renderedLabel = labelsGraphics[i];

                    y = topOfRow;

                    if (renderedLabel.getHeight() < rowHeights[i]) {
                        y += (int) ((rowHeights[i] - renderedLabel.getHeight()) / 2d);
                    }

                    finalGraphics.drawImage(renderedLabel, img.getWidth(), y, null);
                    // cleanup
                    renderedLabel.flush();
                    labelsGraphics[i] = null;
                }

                topOfRow += rowHeights[i];
            }

            finalGraphics.dispose();
        }
        return finalLegend;
    }

    /**
     * Returns a <code>java.awt.Shape</code> appropiate to render a legend graphic given the symbolizer type and the legend dimensions.
     * 
     * @param symbolizer the Symbolizer for whose type a sample shape will be created
     * @param legendWidth the requested width, in output units, of the legend graphic
     * @param legendHeight the requested height, in output units, of the legend graphic
     * 
     * @return an appropiate Line2D, Rectangle2D or LiteShape(Point) for the symbolizer, wether it is a LineSymbolizer, a PolygonSymbolizer, or a
     *         Point ot Text Symbolizer
     * 
     * @throws IllegalArgumentException if an unknown symbolizer impl was passed in.
     */
    private LiteShape2 getSampleShape(Symbolizer symbolizer, int legendWidth, int legendHeight) {
        LiteShape2 sampleShape;
        final float hpad = (legendWidth * LegendUtils.hpaddingFactor);
        final float vpad = (legendHeight * LegendUtils.vpaddingFactor);

        if (symbolizer instanceof LineSymbolizer) {
            if (this.sampleLine == null) {
                Coordinate[] coords = { new Coordinate(hpad, legendHeight - vpad - 1),
                        new Coordinate(legendWidth - hpad - 1, vpad) };
                LineString geom = geomFac.createLineString(coords);

                try {
                    this.sampleLine = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleLine = null;
                }
            }

            sampleShape = this.sampleLine;
        } else if ((symbolizer instanceof PolygonSymbolizer)
                || (symbolizer instanceof RasterSymbolizer)) {
            if (this.sampleRect == null) {
                final float w = legendWidth - (2 * hpad) - 1;
                final float h = legendHeight - (2 * vpad) - 1;

                Coordinate[] coords = { new Coordinate(hpad, vpad), new Coordinate(hpad, vpad + h),
                        new Coordinate(hpad + w, vpad + h), new Coordinate(hpad + w, vpad),
                        new Coordinate(hpad, vpad) };
                LinearRing shell = geomFac.createLinearRing(coords);
                Polygon geom = geomFac.createPolygon(shell, null);

                try {
                    this.sampleRect = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleRect = null;
                }
            }

            sampleShape = this.sampleRect;
        } else if (symbolizer instanceof PointSymbolizer || symbolizer instanceof TextSymbolizer) {
            if (this.samplePoint == null) {
                Coordinate coord = new Coordinate(legendWidth / 2, legendHeight / 2);

                try {
                    this.samplePoint = new LiteShape2(geomFac.createPoint(coord), null, null, false);
                } catch (Exception e) {
                    this.samplePoint = null;
                }
            }

            sampleShape = this.samplePoint;
        } else {
            throw new IllegalArgumentException("Unknown symbolizer: " + symbolizer);
        }

        return sampleShape;
    }

    private static SimpleFeature createSampleFeature() {
        SimpleFeatureType type;
        try {
            type = DataUtilities.createType("Sample", "the_geom:Geometry");
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return SimpleFeatureBuilder.template((SimpleFeatureType) type, null);
    }

    /**
     * Creates a sample Feature instance in the hope that it can be used in the rendering of the legend graphic.
     * 
     * @param schema the schema for which to create a sample Feature instance
     * 
     * @return
     * 
     * @throws ServiceException
     */
    private static Feature createSampleFeature(FeatureType schema) throws ServiceException {
        Feature sampleFeature;
        try {
            if (schema instanceof SimpleFeatureType) {
                sampleFeature = SimpleFeatureBuilder.template((SimpleFeatureType) schema, null);
            } else {
                sampleFeature = DataUtilities.templateFeature(schema);
            }
        } catch (IllegalAttributeException e) {
            throw new ServiceException(e);
        }
        return sampleFeature;
    }

}
