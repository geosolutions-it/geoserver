/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.measure.unit.Unit;

import net.sf.json.JSONNull;
import net.sf.json.util.JSONBuilder;

import org.geoserver.platform.ServiceException;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.Description;
import org.geotools.styling.Fill;
import org.geotools.styling.Halo;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.MultiValuedFilter.MatchAction;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.spatial.SpatialOperator;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.style.ExtensionSymbolizer;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.PointSymbolizer;

/**
 * 
 * @author Carlo Cancellieri, GeoSolutions SAS
 * @version $Id$
 */
public class JSONLegendGraphicBuilder {

    /**
     * Default constructor. Subclasses may provide its own with a String parameter to establish its desired output format, if they support more than
     * one (e.g. a JAI based one)
     */
    public JSONLegendGraphicBuilder() {
        super();
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

        Rule[] applicableRules = LegendGraphicModel.buildApplicableRules(model.getRequest());
        
        JSONBuilder json = new JSONBuilder(w);

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
            writeFilter(filter, json, true);

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
                    json.key("Raster");
                    json.object();

                    writeColorMap(rSymbolizer.getColorMap(), json);
                    // TODO rSymbolizer.getChannelSelection();

                    json.endObject();
                } else if (symbolizer instanceof PolygonSymbolizer) {
                    PolygonSymbolizer pSymbolizer = ((PolygonSymbolizer) symbolizer);
                    json.key("Polygon");
                    json.object();

                    writeFill(pSymbolizer.getFill(), json);

                    Expression po = pSymbolizer.getPerpendicularOffset();
                    if (po != null)
                        json.key("perpendicularOffset").value(Integer.parseInt(po.toString()));

                    writeStroke(pSymbolizer.getStroke(), json);

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

                    writeFont(tSymbolizer.getFont(), json, "font");

                    json.endObject();
                } else if (symbolizer instanceof LineSymbolizer) {
                    LineSymbolizer lSymbolizer = ((LineSymbolizer) symbolizer);
                    json.key("Line");
                    json.object();
                    Expression po = lSymbolizer.getPerpendicularOffset();
                    if (po != null)
                        json.key("perpendicularOffset").value(Integer.parseInt(po.toString()));

                    Stroke stroke = lSymbolizer.getStroke();
                    writeStroke(stroke, json);

                    json.endObject();

                } else if (symbolizer instanceof PointSymbolizer) {
                    PointSymbolizer pSymbolizer = ((PointSymbolizer) symbolizer);
                    json.key("Point");
                    json.object();
                    writeGraphic(pSymbolizer.getGraphic(), json, null);
                    json.endObject();

                } else if (symbolizer instanceof ExtensionSymbolizer) {
                    ExtensionSymbolizer eSymbolizer = ((ExtensionSymbolizer) symbolizer);
                    json.key("Extension");
                    json.object();
                    json.key("name").value(eSymbolizer.getExtensionName());

                    Map<String, Expression> params = eSymbolizer.getParameters();
                    if (params != null) {
                        json.key("parameters");
                        json.object();
                        for (Map.Entry<String, Expression> e : params.entrySet()) {
                            json.key(e.getKey()).value(getMatchingType(e.getValue()));
                        }
                        json.endObject();
                    }
                    json.endObject();

                } else {
                    // TODO log problem
                    json.key(symbolizer.getClass().getSimpleName());
                    json.object();
                    json.endObject();
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
        }
        json.endArray();
        json.endObject();
    }

    private static void writeFont(org.opengis.style.Font font, JSONBuilder json, String prefix) {
        if (font != null) {

            json.key(prefix != null ? prefix + "Family" : "family").value(font.getFamily().get(0));
            json.key(prefix != null ? prefix + "Style" : "style").value(font.getStyle());
            json.key(prefix != null ? prefix + "Weight" : "weight").value(font.getWeight());
            Expression size = font.getSize();
            if (size != null)
                json.key(prefix != null ? prefix + "Size" : "size").value(
                        Integer.parseInt(size.toString()));
        }
    }

    private static void writeGraphic(org.opengis.style.Graphic graphic, JSONBuilder json,
            String prefix) {
        if (graphic != null) {

            Expression e = graphic.getSize();
            if (e != null && !e.equals(Expression.NIL)) {
                json.key(prefix != null ? prefix + "Size" : "size").value(getMatchingType(e));
            }
            Expression r = graphic.getRotation();
            if (r != null)
                json.key(prefix != null ? prefix + "Rotation" : "rotation").value(
                        Double.parseDouble(r.toString()));

            Expression o = graphic.getOpacity();
            if (o != null)
                json.key(prefix != null ? prefix + "Opacity" : "opacity").value(
                        Double.parseDouble(o.toString()));

            List<GraphicalSymbol> gSymbols = graphic.graphicalSymbols();
            if (gSymbols != null) {
                json.key(prefix != null ? prefix + "GraphicalSymbols" : "graphicalSymbols");
                json.array();
                for (GraphicalSymbol gs : gSymbols) {
                    json.value(gs.toString());
                }
                json.endArray();
            }
        }
    }

    private static void writeFill(Fill fill, JSONBuilder json) {
        if (fill != null) {
            json.key("fill").value(true);
            Expression o = fill.getOpacity();
            if (o != null)
                json.key("opacity").value(Double.parseDouble(o.toString()));

            json.key("fillColor").value(fill.getColor());

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

            Expression dashOff = stroke.getDashOffset();
            if (dashOff != null)
                json.key("strokeDashOffset").value(Integer.parseInt(dashOff.toString()));

            // TODO continue
        }
    }

    private static void writeColorMap(ColorMap colormap, JSONBuilder json) {
        if (colormap != null) {
            json.key("colorMap");
            json.array();
            for (ColorMapEntry c : colormap.getColorMapEntries()) {
                json.object();
                json.key("label").value(c.getLabel());
                Expression o = c.getOpacity();
                if (o != null)
                    json.key("opacity").value(getMatchingType(o));
                Expression q = c.getQuantity();
                if (q != null)
                    json.key("quantity").value(getMatchingType(q));
                json.key("color").value(c.getColor());
                json.endObject();
            }
            json.endArray();
        }
    }

    private static Object getMatchingType(Expression q) {
        if (q == null) {
            return JSONNull.getInstance();
        }
        String value = q.toString();
        if (value.isEmpty()) {
            return JSONNull.getInstance();
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException nfe) {
            return value;
        }
    }

    /**
     * 
     * 
     * 
     * 
     * 
     * @param filter
     * @param json
     */
    private static void writeFilter(Filter filter, JSONBuilder json, boolean withKey) {
        if (filter != null) {
            // json.key("filter").value(filter.toString());
            if (withKey){
                json.key("filter");
            }
            json.object();
            Class<? extends Filter> filterClass = filter.getClass();
            if (Not.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Logical.NOT = !;
                json.key("type").value("!");
                Not not = (Not) filter;
                writeFilter(not.getFilter(), json, true);

            } else if (Id.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Logical.FeatureId = FID;
                json.key("type").value("FID");
                Id id = (Id) filter;
                Set<Identifier> sIds = id.getIdentifiers();
                if (sIds != null) {
                    json.key("identifiers");
                    json.array();
                    for (Identifier i : sIds) {
                        json.value(i.getID());
                    }
                    json.endArray();
                }

            } else if (PropertyIsNil.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Logical....
                json.key("type").value("isNil");
                PropertyIsNil nil = (PropertyIsNil) filter;
                json.key("property").value(nil.getExpression());
                json.key("reason").value(nil.getNilReason());

            } else if (PropertyIsNull.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Logical....
                json.key("type").value("isNull");
                PropertyIsNull nu11 = (PropertyIsNull) filter;
                json.key("property").value(nu11.getExpression());

            } else if (BinaryLogicOperator.class.isAssignableFrom(filterClass)) {

                BinaryLogicOperator b = (BinaryLogicOperator) filter;
                if (And.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Locical.AND = &&;
                    json.key("type").value("&&");
                } else if (Or.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Logical.OR = ||;
                    json.key("type").value("||");
                }
                json.key("filters");
                json.array();
                List<Filter> filters = b.getChildren();
                if (filters != null) {
                    for (Filter cildren : filters) {
                        writeFilter(cildren, json, false);
                    }
                }
                json.endArray();
            } else if (BinaryTemporalOperator.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Logical....
                if (After.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("After");
                } else if (AnyInteracts.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("AnyInteracts");
                } else if (Before.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("Before");
                } else if (Begins.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("Begins");
                } else if (BegunBy.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("BegunBy");
                } else if (During.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("During");
                } else if (EndedBy.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("EndedBy");
                } else if (Ends.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("Ends");
                } else if (Meets.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("Meets");
                } else if (MetBy.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("MetBy");
                } else if (OverlappedBy.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("OverlappedBy");
                } else if (TContains.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("TContains");
                } else if (TEquals.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("TEquals");
                } else if (TOverlaps.class.isAssignableFrom(filterClass)) {
                    json.key("type").value("TOverlaps");
                    // } else {
                    // TODO Throw exception UnrecognizedFilter
                }
                BinaryTemporalOperator b = (BinaryTemporalOperator) filter;
                json.key("matchAction").value(b.getMatchAction());
                json.key("start").value(b.getExpression1());
                json.key("end").value(b.getExpression2());

            } else if (PropertyIsBetween.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Comparison.BETWEEN = ..;
                json.key("type").value("..");
                PropertyIsBetween b = (PropertyIsBetween) filter;
                json.key("property").value(b.getExpression());
                json.key("lowerBoundary").value(getMatchingType(b.getLowerBoundary()));
                json.key("upperBoundary").value(getMatchingType(b.getUpperBoundary()));
                json.key("matchAction").value(b.getMatchAction());

            } else if (PropertyIsLike.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Comparison.LIKE = ~;
                json.key("type").value("~");
                PropertyIsLike b = (PropertyIsLike) filter;
                json.key("property").value(b.getExpression());
                json.key("escape").value(b.getEscape());
                json.key("value").value(b.getLiteral());
                json.key("matchAction").value(b.getMatchAction());
            } else if (SpatialOperator.class.isAssignableFrom(filterClass)) {
                // OpenLayers.Filter.Spatial.BBOX = BBOX;
                // OpenLayers.Filter.Spatial.INTERSECTS = INTERSECTS;
                // OpenLayers.Filter.Spatial.DWITHIN = DWITHIN;
                // OpenLayers.Filter.Spatial.WITHIN = WITHIN;
                // OpenLayers.Filter.Spatial.CONTAINS = CONTAINS;
                SpatialOperator b = (SpatialOperator) filter;
                json.key("matchAction").value(b.getMatchAction());
                // TODO subtypes

            } else if (BinaryComparisonOperator.class.isAssignableFrom(filterClass)) {
                if (PropertyIsEqualTo.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Comparison.EQUAL_TO = ==;
                    json.key("type").value("==");
                } else if (PropertyIsGreaterThan.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Comparison.GREATER_THAN = >;
                    json.key("type").value(">");
                } else if (PropertyIsGreaterThanOrEqualTo.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO = >=;
                    json.key("type").value(">=");
                } else if (PropertyIsNotEqualTo.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Comparison.NOT_EQUAL_TO = !=;
                    json.key("type").value("!=");
                } else if (PropertyIsLessThan.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Comparison.LESS_THAN = <;
                    json.key("type").value("<");
                } else if (PropertyIsLessThanOrEqualTo.class.isAssignableFrom(filterClass)) {
                    // OpenLayers.Filter.Comparison.LESS_THAN_OR_EQUAL_TO = <=;
                    json.key("type").value("<=");
                    // } else if (BetweenFilter.class.isAssignableFrom(filterClass)) {
                    // //OpenLayers.Filter.Comparison.BETWEEN = ..;
                    // json.key("type").value("..");
                    // PropertyIsBetween b = (PropertyIsBetween) filter;
                    // json.key("property").value(b.getExpression());
                    // json.key("lowerBoundary").value(getMatchingType(b.getLowerBoundary()));
                    // json.key("upperBoundary").value(getMatchingType(b.getUpperBoundary()));
                    // json.key("matchAction").value(b.getMatchAction());
                    // json.endObject();
                    // return;
                }

                BinaryComparisonOperator b = (BinaryComparisonOperator) filter;
                json.key("property").value(b.getExpression1());
                json.key("value").value(getMatchingType(b.getExpression2()));
                json.key("matchCase").value(b.isMatchingCase());

            }
//            if (withKey){
                json.endObject();
//            }
        }
    }

}
