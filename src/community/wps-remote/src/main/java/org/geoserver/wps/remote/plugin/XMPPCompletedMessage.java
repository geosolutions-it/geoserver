/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.razorvine.pickle.PickleException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.SpatialFile;
import org.geoserver.wps.process.FileRawData;
import org.geoserver.wps.process.StreamRawData;
import org.geoserver.wps.process.StringRawData;
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

    protected Importer importer;
    
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
        final Boolean publish = (signalArgs.get("publish_as_layer") != null ? Boolean.valueOf(signalArgs.get("publish_as_layer")) : false);

        // NOTIFY THE LISTENERS
        if (type != null && !type.isEmpty()) {
            Map<String, Object> outputs = new HashMap<String, Object>();
            try {
                for (Entry<String, String> result : signalArgs.entrySet()) {
                    if (result.getKey().startsWith("result")) {
                        String serviceResultString = URLDecoder.decode(result.getValue(),
                                "UTF-8");
                        JSONObject serviceResultJSON = (JSONObject) JSONSerializer
                                .toJSON(serviceResultString);
                        Object output = xmppClient.unPickle(xmppClient.pickle(serviceResultJSON));
                        if (!"textual".equals(type) && output instanceof Map) {
                            for (Entry<String, Object> entry : ((Map<String, Object>) output)
                                    .entrySet()) {
                                Object value = entry.getValue();
                                if (value != null && value instanceof String
                                        && !((String) value).isEmpty()) {
                                    if (XMPPClient.PRIMITIVE_NAME_TYPE_MAP.get(type) != null) {
                                        Object sample = sample = ((Object[]) XMPPClient.PRIMITIVE_NAME_TYPE_MAP.get(type))[2];
                                        
                                        if (sample instanceof StreamRawData) {
                                            value = new StreamRawData(((StreamRawData) sample).getMimeType(), new FileInputStream(((String) value)), ((StreamRawData) sample).getFileExtension());
                                            if (publish) {
                                                importLayer(new File(((String) value)));
                                            }
                                        } else if (sample instanceof FileRawData) {
                                            value = new FileRawData(new File(((String) value)), ((FileRawData) sample).getMimeType(), ((FileRawData) sample).getFileExtension()); 
                                            if (publish) {
                                                importLayer(new File(((String) value)));
                                            }
                                        } else if (sample instanceof StringRawData) {
                                            value = new StringRawData((String) value, ((StringRawData) sample).getMimeType());
                                            if (publish) {
                                                final File tempFile = File.createTempFile("wps-remote-str-rawdata", "."+((StringRawData) sample).getFileExtension());
                                                Writer outputFile = new FileWriter(tempFile);
                                                IOUtils.write((String)value, outputFile);
                                                
                                                importLayer(tempFile);
                                            }
                                        }
                                        outputs.put(result.getKey(), value);                                        
                                    }
                                }
                            }
                        }
                        else if (output instanceof Map){
                            outputs.putAll((Map<String, Object>) output);
                        }
                    }
                }
                
                for (RemoteProcessClientListener listener : xmppClient
                        .getRemoteClientListeners()) {
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

    /**
     * @param value
     * @throws IOException
     */
    private void importLayer(File file) throws IOException {
        ImportContext context = 
                importer.createContext(new SpatialFile(file));
        
        ImportTask task = context.getTasks().get(0);
        //assertEquals(ImportTask.State.READY, task.getState());
        
        //assertEquals("the layer name", task.getLayer().getResource().getName());

        importer.run(context);
    }

}
