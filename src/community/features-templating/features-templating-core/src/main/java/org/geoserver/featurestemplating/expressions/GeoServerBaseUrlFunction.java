package org.geoserver.featurestemplating.expressions;

import org.apache.commons.lang3.StringUtils;
import org.geoserver.ows.Request;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.capability.FunctionNameImpl;

import javax.servlet.http.HttpServletRequest;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

public class GeoServerBaseUrlFunction extends RequestFunction  {

    public static FunctionName NAME =
        new FunctionNameImpl(
            "geoServerBaseUrl",
            parameter("result", String.class));

    public GeoServerBaseUrlFunction() {
        super(NAME);
    }

    @Override
    protected Object evaluateInternal(Request request, Object object) {
        HttpServletRequest req = request.getHttpRequest();
        String hostHeader = req.getHeader("Host");
        StringBuilder serviceUrl = new StringBuilder();
        if (req.getScheme() != null) {
            serviceUrl.append(req.getScheme());
            serviceUrl.append("://");
        } else {
            serviceUrl.append("http://");
        }
        if (StringUtils.isNotBlank(hostHeader)){
            serviceUrl.append(hostHeader);
        } else {
            serviceUrl.append(req.getServerName());
            if (req.getServerPort() != -1) {
                serviceUrl.append(":");
                serviceUrl.append(req.getServerPort());
            }
        }
        serviceUrl.append(req.getContextPath());
        return serviceUrl.toString();
    }

}
