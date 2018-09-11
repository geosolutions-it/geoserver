/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.datatype.XMLGregorianCalendar;
import net.opengis.ows11.ExceptionType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wps.executor.ExecuteResponseBuilder;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.executor.WPSExecutionManager;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.springframework.context.ApplicationContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Internal Base {@link TransformerBase} for GetExecutions
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class GetExecutionsTransformer extends TransformerBase {

    static final Logger LOGGER = Logging.getLogger(GetExecutionsTransformer.class);

    private List<ExecutionStatus> executions = new ArrayList<ExecutionStatus>();

    WPSInfo wps;

    /** The object tracking the status of various processes */
    private ProcessStatusTracker statusTracker;

    /** The resource tracker, we use it to build the responses */
    private WPSResourceManager resources;

    /** Used by the response builder */
    private ApplicationContext ctx;

    /** Used to cancel the progress of a certain process */
    private WPSExecutionManager executionManager;

    /** The original request POJO storing the query KVPs */
    private GetExecutionsType request;

    /** Pagination variables */
    private Integer total;

    private Integer startIndex;
    private Integer maxFeatures;

    public GetExecutionsTransformer(
            WPSInfo wps,
            WPSExecutionManager executionManager,
            ProcessStatusTracker statusTracker,
            WPSResourceManager resources,
            ApplicationContext ctx,
            GetExecutionsType request,
            Integer total,
            Integer startIndex,
            Integer maxFeatures) {
        this.wps = wps;
        this.executionManager = executionManager;
        this.statusTracker = statusTracker;
        this.resources = resources;
        this.ctx = ctx;
        this.request = request;
        this.total = total;
        this.startIndex = startIndex;
        this.maxFeatures = maxFeatures;
    }

    public void append(ExecutionStatus status) {
        executions.add(status);
    }

    class GMLTranslator extends TranslatorSupport {

        public GMLTranslator(ContentHandler contentHandler) {
            super(contentHandler, null, null);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            // register namespaces provided by extended capabilities
            NamespaceSupport namespaces = getNamespaceSupport();
            namespaces.declarePrefix("wps", "http://www.opengis.net/wps/1.0.0");
            namespaces.declarePrefix("ows", "http://www.opengis.net/ows/1.1");
            namespaces.declarePrefix("gml", "http://www.opengis.net/gml/3.2");
            namespaces.declarePrefix("xs", "http://www.w3.org/2001/XMLSchema");
            namespaces.declarePrefix("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            namespaces.declarePrefix("xlink", "http://www.w3.org/1999/xlink");

            final AttributesImpl attributes = new AttributesImpl();
            registerNamespaces(getNamespaceSupport(), attributes);

            final String proxyBaseUrl =
                    wps.getGeoServer().getGlobal().getSettings().getProxyBaseUrl();
            final String baseUrl = proxyBaseUrl != null ? proxyBaseUrl : "/";
            String serviceInstance =
                    ResponseUtils.appendQueryString(
                            ResponseUtils.buildURL(baseUrl, "ows", null, URLType.SERVICE), "");

            attributes.addAttribute("", "xml:lang", "xml:lang", "", "en");
            attributes.addAttribute("", "service", "service", "", wps.getName());
            attributes.addAttribute("", "version", "version", "", "1.0.0");
            attributes.addAttribute("", "serviceInstance", "serviceInstance", "", serviceInstance);
            attributes.addAttribute("", "count", "count", "", String.valueOf(total));

            getPaginationAttributes(serviceInstance, attributes);

            start("wps:GetExecutionsResponse", attributes);
            for (ExecutionStatus status : executions) {
                ExecuteType execute = status.getRequest();
                try {
                    if (execute == null) {
                        execute = resources.getStoredRequestObject(status.getExecutionId());
                    }
                    if (execute == null) {
                        throw new WPSException(
                                "Could not locate the original request for execution id: "
                                        + status.getExecutionId());
                    } else {
                        ExecuteResponseBuilder builder =
                                new ExecuteResponseBuilder(execute, ctx, status);
                        ExecuteResponseType responseType = builder.build();

                        start("wps:ExecuteResponse");
                        final AttributesImpl processAttributes = new AttributesImpl();
                        processAttributes.addAttribute(
                                "",
                                "wps:processVersion",
                                "wps:processVersion",
                                "",
                                responseType.getProcess().getProcessVersion());
                        start("wps:Process", processAttributes);
                        element(
                                "ows:Identifier",
                                responseType.getProcess().getIdentifier().getValue());
                        element("ows:Title", responseType.getProcess().getTitle().getValue());
                        element("ows:Abstract", responseType.getProcess().getAbstract().getValue());
                        end("wps:Process");

                        final AttributesImpl statusAttributes = new AttributesImpl();
                        statusAttributes.addAttribute(
                                "",
                                "creationTime",
                                "creationTime",
                                "",
                                responseType.getStatus().getCreationTime().toString());
                        statusAttributes.addAttribute(
                                "",
                                "completionTime",
                                "completionTime",
                                "",
                                Converters.convert(
                                                status.getCompletionTime(),
                                                XMLGregorianCalendar.class)
                                        .toString());
                        statusAttributes.addAttribute(
                                "",
                                "lastUpdated",
                                "lastUpdated",
                                "",
                                Converters.convert(
                                                status.getLastUpdated(), XMLGregorianCalendar.class)
                                        .toString());

                        start("wps:Status", statusAttributes);
                        element("wps:JobID", status.getExecutionId());
                        element(
                                "wps:Identifier",
                                responseType.getProcess().getIdentifier().getValue());
                        element("wps:Owner", status.getUserName());
                        element("wps:Status", status.getPhase().name());
                        if (status.getEstimatedCompletion() != null) {
                            element(
                                    "wps:EstimatedCompletion",
                                    Converters.convert(
                                                    status.getEstimatedCompletion(),
                                                    XMLGregorianCalendar.class)
                                            .toString());
                        }
                        element(
                                "wps:ExpirationDate",
                                Converters.convert(
                                                status.getExpirationDate(),
                                                XMLGregorianCalendar.class)
                                        .toString());
                        element(
                                "wps:NextPoll",
                                Converters.convert(status.getNextPoll(), XMLGregorianCalendar.class)
                                        .toString());
                        element("wps:PercentCompleted", String.valueOf(status.getProgress()));
                        if (status.getException() != null) {
                            StringBuffer stackTrace = new StringBuffer();
                            EList exceptions =
                                    responseType
                                            .getStatus()
                                            .getProcessFailed()
                                            .getExceptionReport()
                                            .getException();
                            for (Object ex : exceptions) {
                                if (ex instanceof ExceptionType) {
                                    stackTrace.append(((ExceptionType) ex).getExceptionCode());
                                    stackTrace.append(": ");
                                    stackTrace.append(((ExceptionType) ex).getExceptionText());
                                    stackTrace.append("\n");
                                }
                            }
                            element("wps:ProcessFailed", stackTrace.toString());
                        } else if (status.getPhase() == ProcessState.QUEUED) {
                            element(
                                    "wps:ProcessAccepted",
                                    responseType.getStatus().getProcessAccepted());
                        } else if (status.getPhase() == ProcessState.RUNNING) {
                            element(
                                    "wps:ProcessStarted",
                                    responseType.getStatus().getProcessStarted().getValue());
                        } else {
                            element(
                                    "wps:ProcessSucceeded",
                                    responseType.getStatus().getProcessSucceeded());
                        }

                        // status location, if asynch
                        if (status.isAsynchronous() && responseType.getStatusLocation() != null) {
                            element("wps:StatusLocation", responseType.getStatusLocation());
                        }

                        // lineage, should be included only if requested, the response should
                        // contain it
                        // even if the process is not done computing. From the spec:
                        // * If lineage is "true" the server shall include in the execute response a
                        // complete copy
                        // of
                        // the DataInputs and OutputDefinition elements _as received in the execute
                        // request_.
                        // *If lineage is "false" then/ these elements shall be omitted from the
                        // response
                        if (responseType.getDataInputs() != null
                                && responseType.getDataInputs().getInput().size() > 0) {
                            EList inputs = responseType.getDataInputs().getInput();
                            for (Object input : inputs) {
                                /** TODO: Encode Inputs on the Status Response */
                                System.out.println(
                                        " ------------------------------------ " + input);
                            }
                        }

                        if (responseType.getOutputDefinitions() != null
                                && responseType.getOutputDefinitions().getOutput().size() > 0) {
                            EList outputs = responseType.getOutputDefinitions().getOutput();
                            for (Object output : outputs) {
                                /** TODO: Encode Outputs on the Status Response */
                                System.out.println(
                                        " ------------------------------------ " + output);
                            }
                        }
                        end("wps:Status");
                        end("wps:ExecuteResponse");
                    }
                } catch (IOException e) {
                    throw new WPSException(Executions.INTERNAL_SERVER_ERROR_CODE, e);
                }
            }
            end("wps:GetExecutionsResponse");
        }

        /**
         * Set Pagination accordingly to the GSIP-169: - if number less or equal than
         * MAX_FEATURES_PER_PAGE, then go ahead - if number greater than MAX_FEATURES_PER_PAGE --
         * add "count" attribute to the GetExecutionsResponse, representing the total number of
         * elements -- add "next" attribute to the GetExecutionsResponse, representing the URL of
         * the next page; it this is not present then there are no more pages available -- add
         * "previous" attribute to the GetExecutionsResponse, representing the URL of the previous
         * page; it this is not present then we are at the first page
         *
         * @param serviceInstance
         * @param attributes
         */
        private void getPaginationAttributes(String serviceInstance, AttributesImpl attributes) {
            String baseRequestUrl =
                    serviceInstance
                            + "service="
                            + request.service
                            + "&version="
                            + request.version
                            + "&request=GetExecutions&";
            if (request.identifier != null) {
                baseRequestUrl += "identifier=" + request.identifier;
            }
            if (request.owner != null) {
                baseRequestUrl += "owner=" + request.owner;
            }
            if (request.status != null) {
                baseRequestUrl += "status=" + request.status;
            }
            if (request.orderBy != null) {
                baseRequestUrl += "orderBy=" + request.orderBy;
            }
            if (request.maxFeatures != null) {
                baseRequestUrl += "maxFeatures=" + request.maxFeatures;
            }

            if (maxFeatures != null && maxFeatures > 0) {
                int index = startIndex != null ? startIndex : 0;
                if (index > 0) {
                    attributes.addAttribute(
                            "",
                            "previous",
                            "previous",
                            "",
                            baseRequestUrl
                                    + "&startIndex="
                                    + (index - Math.min(maxFeatures, index)));
                }

                if ((total - maxFeatures) > index) {
                    attributes.addAttribute(
                            "",
                            "next",
                            "next",
                            "",
                            baseRequestUrl + "&startIndex=" + (index + maxFeatures));
                }
            }
        }

        /**
         * Register all namespaces as xmlns:xxx attributes for the top level element of a xml
         * document
         *
         * @param ns
         * @param attributes
         */
        void registerNamespaces(NamespaceSupport ns, AttributesImpl attributes) {
            Enumeration declaredPrefixes = ns.getDeclaredPrefixes();
            while (declaredPrefixes.hasMoreElements()) {
                String prefix = (String) declaredPrefixes.nextElement();
                String uri = ns.getURI(prefix);

                // ignore xml prefix
                if ("xml".equals(prefix)) {
                    continue;
                }

                String prefixDef = "xmlns:" + prefix;

                attributes.addAttribute("", prefixDef, prefixDef, "", uri);
            }
        }
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new GMLTranslator(handler);
    }

    /** @return the executions */
    public List<ExecutionStatus> getExecutions() {
        return executions;
    }
}
