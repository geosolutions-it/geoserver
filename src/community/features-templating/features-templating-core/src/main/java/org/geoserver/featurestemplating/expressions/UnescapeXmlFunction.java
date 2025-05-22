/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;

/**
 * Unescapes XML entities in a string.
 *
 * <p>Example: {@code unscapeXml("Hello &amp; World") -> "Hello & World"}
 */
public class UnescapeXmlFunction extends FunctionImpl {

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "unescapeXml",
                    String.class,
                    parameter("unecapedString", String.class),
                    parameter("param", String.class));

    public UnescapeXmlFunction() {
        this.functionName = NAME;
    }

    @Override
    public Object evaluate(Object feature) {
        List<Expression> params = getParameters();
        String escapedValue = params.get(0).evaluate(feature, String.class);
        if (escapedValue == null) return null;
        return StringEscapeUtils.unescapeXml(escapedValue);
    }
}
