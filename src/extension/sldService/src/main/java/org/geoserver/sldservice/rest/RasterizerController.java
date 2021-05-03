/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.sldservice.utils.classifier.ColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.CustomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.GrayColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.JetColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RandomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RedColorRamp;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ClassifierController. */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/sldservice")
public class RasterizerController extends BaseSLDServiceController {
    private static final Logger LOGGER = Logging.getLogger(RasterizerController.class);

    public enum COLORRAMP_TYPE {
        RED,
        BLUE,
        GRAY,
        JET,
        RANDOM,
        CUSTOM
    };

    public enum COLORMAP_TYPE {
        RAMP,
        INTERVALS,
        VALUES
    };

    private static final String DEFAULT_MIN = "0.0";

    private static final String DEFAULT_MAX = "100.0";

    private static final String DEFAULT_CLIP_MIN = "0.0";

    private static final String DEFAULT_CLIP_MAX = "0.0";

    private static final Double DEFAULT_MIN_DECREMENT = 0.000000001;

    private static final String DEFAULT_CLASSES = "100";

    private static final String DEFAULT_DIGITS = "5";

    private static final int DEFAULT_TYPE = ColorMap.TYPE_RAMP;

    @Autowired
    public RasterizerController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        path = "/{layerName}/rasterize",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public Object rasterize(
            @PathVariable String layerName,
            @RequestParam(value = "min", required = false, defaultValue = DEFAULT_MIN) double min,
            @RequestParam(value = "max", required = false, defaultValue = DEFAULT_MAX) double max,
            @RequestParam(value = "clipMin", required = false, defaultValue = DEFAULT_CLIP_MIN)
                    double clipMin,
            @RequestParam(value = "clipMax", required = false, defaultValue = DEFAULT_CLIP_MAX)
                    double clipMax,
            @RequestParam(value = "classes", required = false, defaultValue = DEFAULT_CLASSES)
                    int classes,
            @RequestParam(value = "digits", required = false, defaultValue = DEFAULT_DIGITS)
                    int digits,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "startColor", required = false) String startColor,
            @RequestParam(value = "endColor", required = false) String endColor,
            @RequestParam(value = "midColor", required = false) String midColor,
            @RequestParam(value = "ramp", required = false) String ramp,
            @RequestParam(value = "cache", required = false, defaultValue = "600") long cachingTime,
            @RequestParam(value = "closed", required = false, defaultValue = "false")
                    boolean closed,
            final HttpServletResponse response)
            throws IOException {
        if (cachingTime > 0) {
            response.setHeader(
                    "cache-control",
                    CacheControl.maxAge(cachingTime, TimeUnit.SECONDS)
                            .cachePublic()
                            .getHeaderValue());
        }
        if (layerName == null) {
            return wrapList(new ArrayList(), ArrayList.class);
        }

        int colormapType = DEFAULT_TYPE;
        if (type != null) {
            if (type.equalsIgnoreCase(COLORMAP_TYPE.INTERVALS.toString())) {
                colormapType = ColorMap.TYPE_INTERVALS;
            } else if (type.equalsIgnoreCase(COLORMAP_TYPE.VALUES.toString())) {
                colormapType = ColorMap.TYPE_VALUES;
            }
        }

        COLORRAMP_TYPE rampType =
                (ramp != null ? COLORRAMP_TYPE.valueOf(ramp.toUpperCase()) : COLORRAMP_TYPE.RED);

        if (min == max) min = min - Double.MIN_VALUE;
        if (clipMin == 0.0 && clipMax == 0.0) {
            clipMin = Double.NEGATIVE_INFINITY;
            clipMax = Double.POSITIVE_INFINITY;
        }

        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo != null) {
            ResourceInfo obj = layerInfo.getResource();
            /* Check if it's feature type or coverage */
            if (obj instanceof CoverageInfo) {
                StyleInfo defaultStyle = layerInfo.getDefaultStyle();
                RasterSymbolizer rasterSymbolizer = getRasterSymbolizer(defaultStyle);

                DuplicatingStyleVisitor cloner = new DuplicatingStyleVisitor();
                rasterSymbolizer.accept(cloner);
                RasterSymbolizer rasterSymbolizer1 = (RasterSymbolizer) cloner.getCopy();

                if (rasterSymbolizer == null || rasterSymbolizer1 == null) {
                    throw new InvalidSymbolizer();
                }

                Style rasterized;
                try {
                    rasterized =
                            remapStyle(
                                    defaultStyle,
                                    rasterSymbolizer1,
                                    min,
                                    max,
                                    classes,
                                    rampType,
                                    layerName,
                                    digits,
                                    colormapType,
                                    startColor,
                                    endColor,
                                    midColor,
                                    clipMin,
                                    clipMax,
                                    closed);

                } catch (Exception e) {
                    throw new InvalidSymbolizer();
                }
                StyledLayerDescriptor sld = SF.createStyledLayerDescriptor();
                NamedLayer namedLayer = SF.createNamedLayer();
                namedLayer.setName(layerName);
                namedLayer.addStyle(rasterized);
                sld.addStyledLayer(namedLayer);
                try {
                    return sldAsString(sld);
                } catch (TransformerException e) {
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(
                                Level.FINE,
                                "Exception occurred while transforming the style "
                                        + e.getLocalizedMessage(),
                                e);
                }
            }
        }

        return wrapList(new ArrayList(), ArrayList.class);
    }

    @ResponseStatus(
        value = HttpStatus.EXPECTATION_FAILED,
        reason = "RasterSymbolizer SLD expected!"
    )
    private class InvalidSymbolizer extends RuntimeException {
        private static final long serialVersionUID = 5453377766415209696L;
    }

    /**
     * @param defaultStyle
     * @param rasterSymbolizer
     * @param layerName
     * @param midColor
     * @param endColor
     * @param startColor
     * @throws Exception
     */
    private Style remapStyle(
            StyleInfo defaultStyle,
            RasterSymbolizer rasterSymbolizer,
            double min,
            double max,
            int classes,
            COLORRAMP_TYPE ramp,
            String layerName,
            final int digits,
            final int colorMapType,
            String startColor,
            String endColor,
            String midColor,
            double clipMin,
            double clipMax,
            boolean closed)
            throws Exception {
        StyleBuilder sb = new StyleBuilder();

        ColorMap resampledColorMap = null;

        if (classes > 0) {
            final int extraClasses = closed ? 2 : 1;
            String[] labels = new String[classes + extraClasses];
            double[] quantities = new double[classes + extraClasses];

            ColorRamp colorRamp = null;
            quantities[0] = min - DEFAULT_MIN_DECREMENT;
            if (colorMapType == ColorMap.TYPE_INTERVALS) {
                max = max + DEFAULT_MIN_DECREMENT;
                min = min + DEFAULT_MIN_DECREMENT;
            }
            double res = (max - min) / (classes - 1);
            labels[0] = "transparent";
            final String format = "%." + digits + "f";
            for (int c = 1; c <= classes; c++) {
                quantities[c] = min + res * (c - 1);
                labels[c] = String.format(Locale.US, format, quantities[c]);
            }

            switch (ramp) {
                case RED:
                    colorRamp = new RedColorRamp();
                    break;
                case BLUE:
                    colorRamp = new BlueColorRamp();
                    break;
                case GRAY:
                    colorRamp = new GrayColorRamp();
                    break;
                case JET:
                    colorRamp = new JetColorRamp();
                    break;
                case RANDOM:
                    colorRamp = new RandomColorRamp();
                    break;
                case CUSTOM:
                    colorRamp = new CustomColorRamp();
                    CustomColorRamp customRamp = (CustomColorRamp) colorRamp;
                    if (startColor != null) {
                        customRamp.setStartColor(Color.decode(startColor));
                    }
                    if (endColor != null) {
                        customRamp.setEndColor(Color.decode(endColor));
                    }
                    if (midColor != null) {
                        customRamp.setMid(Color.decode(midColor));
                    }
                    break;
            }
            colorRamp.setNumClasses(classes);

            List<Color> realColorRamp = new ArrayList<Color>();
            realColorRamp.add(Color.BLACK);
            realColorRamp.addAll(colorRamp.getRamp());
            if (closed) {
                realColorRamp.add(Color.BLACK);
                quantities[quantities.length - 1] = max + DEFAULT_MIN_DECREMENT;
                labels[labels.length - 1] = "transparent";
            }
            if (clipMin != Double.NEGATIVE_INFINITY || clipMax != Double.POSITIVE_INFINITY) {
                int start = 1;
                int end = closed ? quantities.length - 2 : quantities.length - 1;
                for (int count = 1;
                        count <= (closed ? quantities.length - 2 : quantities.length - 1);
                        count++) {
                    double quantity = quantities[count];
                    if (clipMin >= quantity) {
                        start = count;
                    }
                    if (clipMax >= quantity) {
                        end = count;
                    }
                }
                final String[] newLabels = new String[(end - start + 1) + extraClasses];
                newLabels[0] = labels[0];
                if (closed) {
                    newLabels[newLabels.length - 1] = labels[labels.length - 1];
                }
                for (int count = start; count <= end; count++) {
                    if (count == start) {
                        newLabels[count - start + 1] =
                                String.format(
                                        Locale.US, format, Math.max(clipMin, quantities[count]));
                    } else if (count == end) {
                        newLabels[count - start + 1] =
                                String.format(
                                        Locale.US, format, Math.min(clipMax, quantities[count]));
                    } else {
                        newLabels[count - start + 1] = labels[count];
                    }
                }
                labels = newLabels;

                final double[] newQuantities = new double[(end - start + 1) + extraClasses];
                newQuantities[0] = quantities[0];
                if (closed) {
                    newQuantities[newQuantities.length - 1] =
                            Math.min(
                                    quantities[quantities.length - 1],
                                    clipMax + DEFAULT_MIN_DECREMENT);
                }
                for (int count = start; count <= end; count++) {
                    if (count == start) {
                        newQuantities[count - start + 1] = Math.max(clipMin, quantities[count]);
                    } else if (count == end) {
                        newQuantities[count - start + 1] = Math.min(clipMax, quantities[count]);
                    } else {
                        newQuantities[count - start + 1] = quantities[count];
                    }
                }
                quantities = newQuantities;

                List<Color> newColorRamp = new ArrayList<Color>();
                newColorRamp.add(realColorRamp.get(0));
                for (int count = start; count <= end; count++) {
                    newColorRamp.add(realColorRamp.get(count));
                }
                if (closed) {
                    newColorRamp.add(realColorRamp.get(realColorRamp.size() - 1));
                }
                realColorRamp = newColorRamp;
            }

            resampledColorMap =
                    sb.createColorMap(
                            labels, quantities, realColorRamp.toArray(new Color[1]), colorMapType);
            FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(null);
            resampledColorMap.getColorMapEntry(0).setOpacity(filterFactory.literal(0));
            if (closed) {
                resampledColorMap
                        .getColorMapEntry(resampledColorMap.getColorMapEntries().length - 1)
                        .setOpacity(filterFactory.literal(0));
            }
        } else {
            return defaultStyle.getStyle();
        }

        rasterSymbolizer.setColorMap(resampledColorMap);
        Style style = sb.createStyle("Feature", rasterSymbolizer);

        return style;
    }

    /** @param defaultStyle */
    private RasterSymbolizer getRasterSymbolizer(StyleInfo sInfo) {
        RasterSymbolizer rasterSymbolizer = null;

        try {
            for (FeatureTypeStyle ftStyle :
                    sInfo.getStyle().featureTypeStyles().toArray(new FeatureTypeStyle[0])) {
                for (Rule rule : ftStyle.rules().toArray(new Rule[0])) {
                    for (Symbolizer sym : rule.getSymbolizers()) {
                        if (sym instanceof RasterSymbolizer) {
                            rasterSymbolizer = (RasterSymbolizer) sym;
                            break;
                        }
                    }

                    if (rasterSymbolizer != null) break;
                }

                if (rasterSymbolizer != null) break;
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(
                        Level.FINE,
                        "The following exception has occurred " + e.getLocalizedMessage(),
                        e);
            return null;
        }

        return rasterSymbolizer;
    }
}
