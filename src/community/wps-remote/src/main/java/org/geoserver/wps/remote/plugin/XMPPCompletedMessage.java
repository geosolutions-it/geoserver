/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.razorvine.pickle.PickleException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class XMPPCompletedMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals("completed");
        return false;
    }

    @Override
    public void handleSignal(XMPPClient xmppClient, Packet packet, Message message,
            Map<String, String> signalArgs) {

        final String pID = signalArgs.get("id");
        final String type = signalArgs.get("message");

        // NOTIFY THE LISTENERS
        if ("textual".equals(type)) {
            Object outputs;
            try {
                String serviceResultString = URLDecoder.decode(signalArgs.get("result"), "UTF-8");
                JSONObject serviceResultJSON = (JSONObject) JSONSerializer
                        .toJSON(serviceResultString);
                outputs = xmppClient.unPickle(xmppClient.pickle(serviceResultJSON));
                for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
                    listener.complete(pID, outputs);
                }
            } catch (PickleException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            } catch (IOException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        // In any case stop the process by notifying the listeners ...
        else {
            for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
                listener.complete(pID, null);
            }
        }

        // NOTIFY THE SERVICE
        final String serviceJID = message.getFrom();
        xmppClient.sendMessage(serviceJID, "topic=finish");

    }

}
