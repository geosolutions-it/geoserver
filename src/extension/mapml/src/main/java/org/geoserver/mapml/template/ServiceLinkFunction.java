package org.geoserver.mapml.template;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

public class ServiceLinkFunction extends FunctionImpl {
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "serviceLink",
                    String.class,
                    parameter("template", String.class),
                    parameter("param", Object.class, 0, Integer.MAX_VALUE));

    public ServiceLinkFunction() {
        this.functionName = NAME;
    }

    @Override
    public Object evaluate(Object feature) {
        List<Expression> params = getParameters();
        String template = params.get(0).evaluate(feature, String.class);
        if (template == null) return null;

        Object[] templateParameters =
                params.stream()
                        .skip(1)
                        .map(p -> p.evaluate(feature, String.class))
                        .map(v -> v != null ? ResponseUtils.urlEncode(v) : null)
                        .toArray();

        String uri = String.format(template, templateParameters);
        Map<String, String> kvp = lenientQueryStringParse(uri);
        String path = ResponseUtils.getPath(uri);

        Request request = Dispatcher.REQUEST.get();
        if (request == null) return path; // just for testing purposes
        String baseURL = ResponseUtils.baseURL(request.getHttpRequest());
        return ResponseUtils.buildURL(baseURL, path, kvp, URLMangler.URLType.SERVICE);
    }

    /**
     * Turns the query string in a <code>Map<String, String></code>. If a parameter is repeated,
     * then only the first instance of it is retained.
     */
    private Map<String, String> lenientQueryStringParse(String path) {
        Map<String, String> kvp = new LinkedHashMap<>();
        KvpUtils.parseQueryString(path)
                .forEach(
                        (k, v) -> {
                            if (v instanceof String) {
                                kvp.put(k, (String) v);
                            } else if (v instanceof String[]) {
                                kvp.put(k, ((String[]) v)[0]);
                            } else if (v != null) {
                                // generic fallback
                                kvp.put(k, v.toString());
                            }
                        });
        return kvp;
    }
}
