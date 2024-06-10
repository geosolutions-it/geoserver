package org.geoserver.mapml.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.api.feature.simple.SimpleFeatureType;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MapMLMapTemplate {
    /** The template configuration used for placemark descriptions */
    static Configuration templateConfig;

    static {
        // initialize the template engine, this is static to maintain a cache
        templateConfig = TemplateUtils.getSafeConfiguration();

        // set the default output formats for dates
        templateConfig.setDateFormat("MM/dd/yyyy");
        templateConfig.setDateTimeFormat("MM/dd/yyyy HH:mm:ss");
        templateConfig.setTimeFormat("HH:mm:ss");

        // set the default locale to be US and the
        // TODO: this may be somethign we want to configure/change
        templateConfig.setLocale(Locale.US);
        templateConfig.setNumberFormat("0.###########");

        // encoding
        templateConfig.setDefaultEncoding("UTF-8");
    }

    /** The pattern used by DATETIME_FORMAT */
    public static String DATE_FORMAT_PATTERN = "MM/dd/yy";

    /** The pattern used by DATETIME_FORMAT */
    public static String DATETIME_FORMAT_PATTERN = "MM/dd/yy HH:mm:ss";

    /** The pattern used by DATETIME_FORMAT */
    public static String TIME_FORMAT_PATTERN = "HH:mm:ss";

    public static final String MAPML_PREVIEW_HEAD_FTL = "mapml-preview-head.ftl";

    /** Template cache used to avoid paying the cost of template lookup for each feature */
    Map<MapMLMapTemplate.TemplateKey, Template> templateCache = new HashMap<>();

    /**
     * Cached writer used for plain conversion from Feature to String. Improves performance
     * significantly compared to an OutputStreamWriter over a ByteOutputStream.
     */
    CharArrayWriter caw = new CharArrayWriter();

    public void preview(Map<String, Object> model, SimpleFeatureType featureType, Writer writer)
            throws IOException {
        execute(model, featureType, writer, MAPML_PREVIEW_HEAD_FTL);
    }

    public String preview(SimpleFeatureType featureType) throws IOException {
        caw.reset();
        preview(Collections.emptyMap(), featureType, caw);

        return caw.toString();
    }

    /*
     * Internal helper method to exceute the template against feature or
     * feature collection.
     */
    private void execute(
            Map<String, Object> model,
            SimpleFeatureType featureType,
            Writer writer,
            String template)
            throws IOException {

        Template t = lookupTemplate(featureType, template, null);

        try {
            t.process(model, writer);
        } catch (TemplateException e) {
            String msg = "Error occured processing template.";
            throw (IOException) new IOException(msg).initCause(e);
        }
    }

    /**
     * Returns the template for the specified feature type. Looking up templates is pretty
     * expensive, so we cache templates by feture type and template.
     */
    private Template lookupTemplate(SimpleFeatureType featureType, String template, Class<?> lookup)
            throws IOException {

        // lookup the cache first
        TemplateKey key = new TemplateKey(featureType, template);
        Template t = templateCache.get(key);
        if (t != null) return t;

        // otherwise, build a loader and do the lookup
        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(
                        lookup != null ? lookup : getClass(),
                        GeoServerExtensions.bean(GeoServerResourceLoader.class));
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        templateLoader.setFeatureType(catalog.getFeatureTypeByName(featureType.getName()));

        // Configuration is not thread safe
        synchronized (templateConfig) {
            templateConfig.setTemplateLoader(templateLoader);
            t = templateConfig.getTemplate(template);
        }
        templateCache.put(key, t);
        return t;
    }

    /** Returns true if the required template is empty or has its default content */
    public boolean isTemplateEmpty(
            SimpleFeatureType featureType,
            String template,
            Class<FeatureTemplate> lookup,
            String defaultContent)
            throws IOException {
        Template t = lookupTemplate(featureType, template, lookup);
        if (t == null) {
            return true;
        }
        // check if the template is empty
        StringWriter sw = new StringWriter();
        t.dump(sw);
        // an empty template canonical form is "0\n".. weird!
        String templateText = sw.toString();
        return "".equals(templateText)
                || (defaultContent != null && defaultContent.equals(templateText));
    }

    public Map<String, String> getMapRequestElementsToModel(
            String workspace,
            String layersCommaDelimited,
            String bbox,
            String format,
            String width,
            String height) {
        HashMap<String, String> model = new HashMap<>();
        // <map-link
        // href="${serviceLink(${serviceRequest},${workspace},${format},${bbox},${layers},${width},${height},${layers})}" rel="style" title="templateinsertedstyle"/>
        model.put("serviceRequest", "getMap");
        model.put("workspace", workspace);
        model.put("format", format);
        model.put("bbox", bbox);
        model.put("layers", layersCommaDelimited);
        model.put("width", width);
        model.put("height", height);
        return model;
    }

    private static class TemplateKey {
        SimpleFeatureType type;
        String template;

        public TemplateKey(SimpleFeatureType type, String template) {
            super();
            this.type = type;
            this.template = template;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((template == null) ? 0 : template.hashCode());
            result = PRIME * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final MapMLMapTemplate.TemplateKey other = (MapMLMapTemplate.TemplateKey) obj;
            if (template == null) {
                if (other.template != null) return false;
            } else if (!template.equals(other.template)) return false;
            if (type == null) {
                if (other.type != null) return false;
            } else if (!type.equals(other.type)) return false;
            return true;
        }
    }
}
