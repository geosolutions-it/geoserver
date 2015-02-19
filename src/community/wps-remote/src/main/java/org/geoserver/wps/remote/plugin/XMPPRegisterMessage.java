/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geoserver.wps.remote.RemoteProcessFactoryListener;
import org.geoserver.wps.remote.RemoteServiceDescriptor;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.opengis.feature.type.Name;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class XMPPRegisterMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals("register");
        return false;
    }

    @Override
    public void handleSignal(XMPPClient xmppClient, Packet packet, Message message,
            Map<String, String> signalArgs) {

        final String serviceName[] = signalArgs.get("service").split("\\.");

        if (serviceName.length <= 1)
            return;

        final Name name = new NameImpl(serviceName[0], serviceName[1]);

        try {
            String serviceDescriptorString = URLDecoder.decode(signalArgs.get("message"), "UTF-8");
            JSONObject serviceDescriptorJSON = (JSONObject) JSONSerializer
                    .toJSON(serviceDescriptorString);

            final String title = (String) serviceDescriptorJSON.get("title");
            final String description = (String) serviceDescriptorJSON.get("description");

            JSONArray input = (JSONArray) serviceDescriptorJSON.get("input");
            JSONArray output = (JSONArray) serviceDescriptorJSON.get("output");

            // INPUTS
            Map<String, Parameter<?>> inputs = new HashMap<String, Parameter<?>>();
            if (input != null) {
                for (int ii = 0; ii < input.size(); ii++) {
                    Object obj = input.get(ii);
                    if (obj instanceof JSONArray) {
                        JSONArray jsonArray = (JSONArray) obj;

                        String paramName = (String) jsonArray.get(0);
                        String ss = ((String) jsonArray.get(1));
                        ss = ss.substring(1, ss.length() - 1);
                        JSONObject paramType = (JSONObject) JSONSerializer.toJSON(ss);
                        String className = (String) paramType.get("type");
                        ParameterTemplate paramTemplate = xmppClient.convertToJavaClass(className,
                                XMPPClient.class.getClassLoader(), paramType.get("default"));

                        inputs.put(
                                paramName,
                                new Parameter(paramName, paramTemplate.getClazz(), Text
                                        .text((String) paramType.get("title")), Text
                                        .text((String) paramType.get("description")), paramType
                                        .get("min") == null || (Integer) paramType.get("min") > 0,
                                        paramType.get("min") != null ? (Integer) paramType
                                                .get("min") : 1,
                                        paramType.get("max") != null ? (Integer) paramType
                                                .get("max") : -1, paramTemplate.getDefaultValue(),
                                        null));
                    }
                }
            }

            // OUTPUTS
            Map<String, Parameter<?>> outputs = new HashMap<String, Parameter<?>>();
            if (output != null) {
                for (int oo = 0; oo < output.size(); oo++) {
                    Object obj = output.get(oo);
                    if (obj instanceof JSONArray) {
                        JSONArray jsonArray = (JSONArray) obj;

                        String paramName = (String) jsonArray.get(0);
                        String ss = ((String) jsonArray.get(1));
                        ss = ss.substring(1, ss.length() - 1);
                        JSONObject paramType = (JSONObject) JSONSerializer.toJSON(ss);
                        String className = (String) paramType.get("type");
                        ParameterTemplate paramTemplate = xmppClient.convertToJavaClass(className,
                                XMPPClient.class.getClassLoader(), paramType.get("default"));

                        outputs.put(
                                paramName,
                                new Parameter(paramName, paramTemplate.getClazz(), Text
                                        .text((String) paramType.get("title")), Text
                                        .text((String) paramType.get("description")), paramType
                                        .get("min") == null || (Integer) paramType.get("min") > 0,
                                        paramType.get("min") != null ? (Integer) paramType
                                                .get("min") : 1,
                                        paramType.get("max") != null ? (Integer) paramType
                                                .get("max") : 0, paramTemplate.getDefaultValue(),
                                        null));
                    }
                }
            }

            // NOTIFY LISTENERS
            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("serviceJID", packet.getFrom());
            for (RemoteProcessFactoryListener listener : xmppClient.getRemoteFactoryListeners()) {
                listener.registerProcess(new RemoteServiceDescriptor(name, title, description,
                        inputs, outputs, metadata));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);

            // NOTIFY LISTENERS
            final Set<RemoteProcessClientListener> remoteClientListeners = xmppClient
                    .getRemoteClientListeners();
            synchronized (remoteClientListeners) {
                for (RemoteProcessClientListener listener : remoteClientListeners) {

                    Map<String, Object> metadata = new HashMap<String, Object>();
                    metadata.put("serviceJID", packet.getFrom());

                    final String pID = (signalArgs != null ? signalArgs.get("id") : null);

                    listener.exceptionOccurred(pID, e, metadata);
                }
            }
        }

    }

}
