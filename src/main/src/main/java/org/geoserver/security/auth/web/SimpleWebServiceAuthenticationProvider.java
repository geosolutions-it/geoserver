/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections.map.HashedMap;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.util.IOUtils;
import org.geotools.data.ows.HTTPClient;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.SimpleHttpClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class SimpleWebServiceAuthenticationProvider extends GeoServerAuthenticationProvider {

    private static final String HTTP_AUTHORIZATION_HEADER = "X-HTTP-AUTHORIZATION";

    SimpleWebAuthenticationConfig config;

    @Override
    public String getName() {

        return "SimpleWebServiceAuthenticationProvider";
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {

        this.config = (SimpleWebAuthenticationConfig) config;
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        // works with user name based authentication only
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request) {

        Set<GrantedAuthority> roles = new TreeSet<GrantedAuthority>();
        roles.addAll(authentication.getAuthorities());

        String responseBody = null;
        try {
            HTTPClient client = getHTTPClient(config);
            Map<String, String> headerMap = null;

            if (config.isUseHeader()) headerMap = getHeader(authentication);

            HTTPResponse httpResponse =
                    client.get(
                            getAuthenticationURL(config.getConnectionURL(), authentication),
                            headerMap);
            responseBody = IOUtils.toString(httpResponse.getResponseStream());
            if (responseBody == null)
                throw new UsernameNotFoundException(
                        "Web Service Authentication Failed for "
                                + authentication.getPrincipal().toString());
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        } catch (Exception e) {
            LOGGER.severe(
                    "Web Service Authentication Failed for "
                            + authentication.getPrincipal().toString());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            // let the other provider have a g
            return null;
        }

        // if a regex is set extract roles from it
        if (config.getRoleRegex() != null && !config.getRoleRegex().isEmpty()) {
            roles.addAll(extractRoles(responseBody, config.getRoleRegex()));
        }

        // next extract user roles from configured service
        // if no role service is selected, use the system default
        try {
            roles.addAll(authorize(authentication.getPrincipal().toString(), config));
        } catch (IOException e) {
            LOGGER.severe(
                    "Error get roles from "
                            + config.getRoleServiceName()
                            + " Role Servie for user: "
                            + authentication.getPrincipal().toString());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        // authenticated but did find any roles..mark as anonymous
        if (roles.isEmpty()) roles.add(GeoServerRole.ANONYMOUS_ROLE);

        UsernamePasswordAuthenticationToken result =
                new UsernamePasswordAuthenticationToken(
                        authentication.getPrincipal(), authentication.getCredentials(), roles);
        result.setDetails(authentication.getDetails());
        return result;
    }

    /* extract roles for USER from default or configured service*/
    private Set<GeoServerRole> authorize(String userName, SimpleWebAuthenticationConfig config)
            throws IOException {
        GeoServerRoleService roleService;

        if (config.getRoleServiceName() == null || config.getRoleServiceName().isEmpty())
            roleService = getSecurityManager().getActiveRoleService();
        else roleService = getSecurityManager().loadRoleService(config.getRoleServiceName());

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Using Role Service" + roleService.getName());

        Set<GeoServerRole> rolesFromService = roleService.getRolesForUser(userName);

        return rolesFromService;
    }

    private HTTPClient getHTTPClient(SimpleWebAuthenticationConfig config) {
        // check for mock bindings. Since we are going to run this code in production as well,
        // guard it so that it only triggers if the MockHttpClientProvider has any active binding
        if (TestHttpClientProvider.testModeEnabled()
                && config.getConnectionURL().startsWith(TestHttpClientProvider.MOCKSERVER)) {
            HTTPClient client = TestHttpClientProvider.get(config.getConnectionURL());

            return client;
        }

        HTTPClient client;
        client = new SimpleHttpClient();

        int connectTimeout =
                (config.getConnectionTimeOut() <= 0)
                        ? SimpleWebAuthenticationConfig.DEFAULT_TIME_OUT
                        : config.getConnectionTimeOut();
        int readTimeout =
                (config.getReadTimeoutOut() <= 0)
                        ? SimpleWebAuthenticationConfig.DEFAULT_READTIME_OUT
                        : config.getReadTimeoutOut();

        client.setConnectTimeout(connectTimeout);
        client.setReadTimeout(readTimeout);

        return client;
    }

    private Set<GrantedAuthority> extractRoles(final String responseBody, final String rolesRegex) {
        final Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
        final Pattern searchRolesRegex = Pattern.compile(rolesRegex);

        Matcher matcher = searchRolesRegex.matcher(responseBody);
        if (matcher != null && matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                for (String roleName : matcher.group(i).split(",")) {

                    authorities.add(new GeoServerRole(roleName.trim()));
                }
            }
        }
        return new TreeSet<GrantedAuthority>(authorities);
    }

    // returns a URL with credentials substitued in place of place holders
    private URL getAuthenticationURL(String connectionURL, Authentication authentication)
            throws MalformedURLException {
        return new URL(
                connectionURL
                        .replace(
                                SimpleWebAuthenticationConfig.URL_PLACEHOLDER_USER,
                                authentication.getPrincipal().toString())
                        .replace(
                                SimpleWebAuthenticationConfig.URL_PLACEHOLDER_PASSWORD,
                                authentication.getCredentials().toString()));
    }

    private Map<String, String> getHeader(Authentication authentication) {
        Map<String, String> headerMap = new HashedMap();

        String credentials = authentication.getPrincipal() + ":" + authentication.getCredentials();
        headerMap.put(HTTP_AUTHORIZATION_HEADER, credentials);
        return headerMap;
    }
}
