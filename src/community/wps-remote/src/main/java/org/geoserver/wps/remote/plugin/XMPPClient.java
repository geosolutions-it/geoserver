/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.razorvine.pickle.Opcodes;
import net.razorvine.pickle.PickleException;
import net.razorvine.pickle.PickleUtils;
import net.razorvine.pickle.Pickler;
import net.razorvine.pickle.Unpickler;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.geoserver.wps.remote.RemoteProcessClient;
import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.NodeInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.disco.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * @author alessio.fabiani
 * 
 */
public class XMPPClient implements RemoteProcessClient {

    public static final Logger LOGGER = Logging.getLogger(XMPPClient.class.getPackage().getName());

    private static final int packetReplyTimeout = 500; // millis

    private String server;

    private int port;

    private ConnectionConfiguration config;

    private XMPPConnection connection;

    private ChatManager chatManager;

    private PacketListener packetListener;

    private Roster roster;

    HashMap<String, Chat> openChat;

    private ServiceDiscoveryManager discoStu;

    private boolean enabled;

    private String username;

    private String password;

    private String domain;

    private String bus;

    private String roomManagerUser;

    private String roomManagerPassword;

    private String managementChannel;

    private List<String> serviceChannels;

    private List<Name> registeredServices = Collections.synchronizedList(new ArrayList<Name>());

    private List<RemoteProcessClientListener> listeners = Collections
            .synchronizedList(new ArrayList<RemoteProcessClientListener>());

    private List<MultiUserChat> mucServiceChannels = Collections
            .synchronizedList(new ArrayList<MultiUserChat>());

    private MultiUserChat mucManagementChannel;

    public XMPPClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /** Primitive type name -> class map. */
    private static final Map PRIMITIVE_NAME_TYPE_MAP = new HashMap();

    /** Setup the primitives map. */
    static {
        PRIMITIVE_NAME_TYPE_MAP.put("string", String.class);
        PRIMITIVE_NAME_TYPE_MAP.put("boolean", Boolean.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("byte", Byte.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("char", Character.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("short", Short.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("int", Integer.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("long", Long.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("float", Float.TYPE);
        PRIMITIVE_NAME_TYPE_MAP.put("double", Double.TYPE);
    }

    /**
     * Convert a list of Strings from an Interator into an array of Classes (the Strings are taken as classnames).
     * 
     * @param it A java.util.Iterator pointing to a Collection of Strings
     * @param cl The ClassLoader to use
     * 
     * @return Array of Classes
     * 
     * @throws ClassNotFoundException When a class could not be loaded from the specified ClassLoader
     */
    public final static Class<?>[] convertToJavaClasses(Iterator<String> it, ClassLoader cl)
            throws ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        while (it.hasNext()) {
            classes.add(convertToJavaClass(it.next(), cl));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Convert a given String into the appropriate Class.
     * 
     * @param name Name of class
     * @param cl ClassLoader to use
     * 
     * @return The class for the given name
     * 
     * @throws ClassNotFoundException When the class could not be found by the specified ClassLoader
     */
    private final static Class convertToJavaClass(String name, ClassLoader cl)
            throws ClassNotFoundException {
        int arraySize = 0;
        while (name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
            arraySize++;
        }

        // Check for a primitive type
        Class c = (Class) PRIMITIVE_NAME_TYPE_MAP.get(name);

        if (c == null) {
            // No primitive, try to load it from the given ClassLoader
            try {
                c = cl.loadClass(name);
            } catch (ClassNotFoundException cnfe) {
                throw new ClassNotFoundException("Parameter class not found: " + name);
            }
        }

        // if we have an array get the array class
        if (arraySize > 0) {
            int[] dims = new int[arraySize];
            for (int i = 0; i < arraySize; i++) {
                dims[i] = 1;
            }
            c = Array.newInstance(c, dims).getClass();
        }

        return c;
    }

    @Override
    public void init() throws Exception {

        System.out.println(String.format("Initializing connection to server %1$s port %2$d",
                server, port));

        SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTimeout);

        config = new ConnectionConfiguration(server, port);
        // config.setSASLAuthenticationEnabled(false);
        config.setSecurityMode(SecurityMode.disabled);

        connection = new XMPPTCPConnection(config);
        connection.connect();

        System.out.println("Connected: " + connection.isConnected());

        if (connection.isConnected()) {
            chatManager = ChatManager.getInstanceFor(connection);

            openChat = new HashMap<String, Chat>();

            discoStu = ServiceDiscoveryManager.getInstanceFor(connection);
            discoProperties();

            setInformation();

            performLogin(username, password);

            startPingTask();

            sendInvitations();
        }
    }

    @Override
    public String execute(Name name, Map<String, Object> input, Map<String, Object> metadata,
            ProgressListener monitor) throws Exception {
        // TODO: check for a free service
        if (metadata != null && metadata.containsKey("serviceJID")) {
            // Extract the PID
            final String serviceJID = (String) metadata.get("serviceJID");
            final String pid = md5Java(serviceJID) + "_" + md5Java(byteArrayToURLString(P(input)));
            // TODO: check if service is running on nodes
            String msg = "topic=request&id="+pid+"&message="+ byteArrayToURLString(P(input));
            sendMessage(serviceJID, msg);

            return pid;
        }

        return null;
    }
    
    /*
     * Add features to our XMPP client We do support Data forms, XHTML-IM, Service Discovery
     */
    private void discoProperties() {
        discoStu.addFeature("http://jabber.org/protocol/xhtml-im");
        discoStu.addFeature("jabber:x:data");
        discoStu.addFeature("http://jabber.org/protocol/disco#info");
        discoStu.addFeature("jabber:iq:privacy");
        discoStu.addFeature("http://jabber.org/protocol/si");
        discoStu.addFeature("http://jabber.org/protocol/bytestreams");
        discoStu.addFeature("http://jabber.org/protocol/ibb");
    }

    private void setInformation() {
        ServiceDiscoveryManager.getInstanceFor(connection).setNodeInformationProvider(
                "http://jabber.org/protocol/muc#rooms", new NodeInformationProvider() {

                    @Override
                    public List<String> getNodeFeatures() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public List<Identity> getNodeIdentities() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public List<Item> getNodeItems() {
                        List<Item> answer = new ArrayList<Item>();
                        List<String> rooms = new ArrayList<String>();
                        try {
                            rooms = MultiUserChat.getJoinedRooms(connection, null);
                        } catch (NoResponseException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        } catch (XMPPErrorException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        } catch (NotConnectedException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        }
                        for (String room : rooms) {
                            answer.add(new DiscoverItems.Item(room));
                        }
                        return null;
                    }

                    @Override
                    public List<PacketExtension> getNodePacketExtensions() {
                        // TODO Auto-generated method stub
                        return null;
                    }

                });

    }

    public void performLogin(String username, String password) throws Exception {
        if (connection != null && connection.isConnected()) {
            connection.login(username, password);

            // Create a MultiUserChat using a XMPPConnection for a room

            // User joins the new room using a password and specifying
            // the amount of history to receive. In this example we are requesting the last 5 messages.
            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(5);

            mucManagementChannel = new MultiUserChat(connection, managementChannel + "@" + bus
                    + "." + domain);
            mucManagementChannel.join(roomManagerUser, roomManagerPassword, history,
                    connection.getPacketReplyTimeout());

            for (String channel : serviceChannels) {
                MultiUserChat serviceChannel = new MultiUserChat(connection, channel + "@" + bus
                        + "." + domain);
                serviceChannel.join(roomManagerUser, roomManagerPassword, history,
                        connection.getPacketReplyTimeout());
                mucServiceChannels.add(serviceChannel);
            }

            setStatus(true, "Orchestrator Active");

            /**
             * String buddyJID = "afabiani"; String buddyName = "afabiani"; createEntry(buddyJID, buddyName);
             * 
             * sendMessage("afabiani@whale.nurc.nato.int", "Hello mate");
             * 
             * printRoster();
             **/

            setupListeners();
            setupRosterListener();
        }
    }

    public void setStatus(boolean available, String status) throws Exception {
        Presence.Type type = available ? Type.available : Type.unavailable;
        Presence presence = new Presence(type);

        presence.setStatus(status);
        connection.sendPacket(presence);
    }

    public void destroy() throws Exception {
        if (connection != null && connection.isConnected()) {
            stopPingTask();
            connection.disconnect();
        }
    }

    public void printRoster() throws Exception {
        Roster roster = connection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            System.out.println(String.format("Buddy:%1$s - Status:%2$s", entry.getName(),
                    entry.getStatus()));
        }
    }

    public void createEntry(String user, String name) throws Exception {
        System.out.println(String.format("Creating entry for buddy '%1$s' with name %2$s", user,
                name));
        Roster roster = connection.getRoster();
        roster.createEntry(user, name, null);
    }

    // @Override
    // public void processMessage(Chat chat, Message message) {
    // String from = message.getFrom();
    // String body = message.getBody();
    // System.out.println(String.format("Received message '%1$s' from %2$s", body, from));
    // }

    /*
     * This handles the chat listener. We can't simply listen to chats for some reason, and intead have to grab the chats from the packets. The other
     * listeners work properly in SMACK
     */
    public void setupListeners() {
        /*
         * This is the actual code that handles what happens with XMPP users
         */
        packetListener = new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                if (packet instanceof Presence) {
                    Presence p = (Presence) packet;

                    try {
                        if (p.isAvailable()) {
                            if (p.getFrom().indexOf("@") > 0) {
                                final String channel = p.getFrom().substring(0,
                                        p.getFrom().indexOf("@"));
                                if (serviceChannels.contains(channel))
                                    handleMemberJoin(p);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                } else if (packet instanceof Message) {
                    Message message = (Message) packet;
                    String origin = message.getFrom().split("/")[0];
                    Chat chat = openChat.get(origin);
                    if (chat == null)
                        setupChat(origin);
                    
                    if (message.getBody() != null) {
                        System.out.println("ReceivedMessage('" + message.getBody() + "','" + origin
                                + "','" + message.getPacketID() + "');");

                        Map<String, String> signalArgs = new HashMap<String, String>();
                        try {
                            String[] messageParts = message.getBody().split("&");
                            for (String mp : messageParts) {
                                String[] signalArg = mp.split("=");
                                signalArgs.put(signalArg[0], signalArg[1]);
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Wrong message! [" + message.getBody() + "]");
                            signalArgs.clear();
                        }

                        if (!signalArgs.isEmpty() && signalArgs.containsKey("topic")) {
                            
                            /**
                             * REGISTER Signal
                             */
                            if (signalArgs.get("topic").equals("register")) {
                                final String serviceName[] = signalArgs.get("service").split("\\.");
                                final Name name = new NameImpl(serviceName[0], serviceName[1]);

                                try {
                                    String serviceDescriptorString = URLDecoder.decode(
                                            signalArgs.get("message"), "UTF-8");
                                    JSONObject serviceDescriptorJSON = (JSONObject) JSONSerializer
                                            .toJSON(serviceDescriptorString);

                                    final String title = (String) serviceDescriptorJSON
                                            .get("title");
                                    final String description = (String) serviceDescriptorJSON
                                            .get("description");

                                    JSONArray input = (JSONArray) serviceDescriptorJSON
                                            .get("input");
                                    JSONArray output = (JSONArray) serviceDescriptorJSON
                                            .get("output");

                                    // INPUTS
                                    Map<String, Parameter<?>> inputs = new HashMap<String, Parameter<?>>();
                                    for (int ii = 0; ii < input.size(); ii++) {
                                        Object obj = input.get(ii);
                                        if (obj instanceof JSONArray) {
                                            JSONArray jsonArray = (JSONArray) obj;

                                            String paramName = (String) jsonArray.get(0);
                                            String ss = ((String) jsonArray.get(1));
                                            ss = ss.substring(1, ss.length() - 1);
                                            JSONObject paramType = (JSONObject) JSONSerializer
                                                    .toJSON(ss);
                                            String className = (String) paramType.get("type");
                                            Class clazz = convertToJavaClass(className,
                                                    XMPPClient.class.getClassLoader());

                                            inputs.put(
                                                    paramName,
                                                    new Parameter(
                                                            paramName,
                                                            clazz,
                                                            Text.text(paramName),
                                                            Text.text((String) paramType
                                                                    .get("description")),
                                                            paramType.get("min") == null
                                                                    || (Integer) paramType
                                                                            .get("min") > 0,
                                                            paramType.get("min") != null ? (Integer) paramType
                                                                    .get("min") : 1,
                                                            paramType.get("max") != null ? (Integer) paramType
                                                                    .get("max") : -1, paramType
                                                                    .get("default"), null));
                                        }
                                    }

                                    // OUTPUTS
                                    Map<String, Parameter<?>> outputs = new HashMap<String, Parameter<?>>();
                                    for (int oo = 0; oo < output.size(); oo++) {
                                        Object obj = output.get(oo);
                                        if (obj instanceof JSONArray) {
                                            JSONArray jsonArray = (JSONArray) obj;

                                            String paramName = (String) jsonArray.get(0);
                                            String ss = ((String) jsonArray.get(1));
                                            ss = ss.substring(1, ss.length() - 1);
                                            JSONObject paramType = (JSONObject) JSONSerializer
                                                    .toJSON(ss);
                                            String className = (String) paramType.get("type");
                                            Class clazz = convertToJavaClass(className,
                                                    XMPPClient.class.getClassLoader());

                                            outputs.put(
                                                    paramName,
                                                    new Parameter(
                                                            paramName,
                                                            clazz,
                                                            Text.text(paramName),
                                                            Text.text((String) paramType
                                                                    .get("description")),
                                                            paramType.get("min") == null
                                                                    || (Integer) paramType
                                                                            .get("min") > 0,
                                                            paramType.get("min") != null ? (Integer) paramType
                                                                    .get("min") : 1,
                                                            paramType.get("max") != null ? (Integer) paramType
                                                                    .get("max") : 0, paramType
                                                                    .get("default"), null));
                                        }
                                    }

                                    // NOTIFY LISTENERS
                                    Map<String, Object> metadata = new HashMap<String, Object>();
                                    metadata.put("serviceJID", packet.getFrom());
                                    for (RemoteProcessClientListener listener : listeners) {
                                        listener.registerService(name, title, description, inputs,
                                                outputs, metadata);
                                    }
                                } catch (Exception e) {
                                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    
                                    // NOTIFY LISTENERS
                                    for (RemoteProcessClientListener listener : listeners) {
                                        
                                        Map<String, Object> metadata = new HashMap<String, Object>();
                                        metadata.put("serviceJID", packet.getFrom());
                                        
                                        final String pID = (signalArgs != null ? signalArgs.get("id") : null);
                                        
                                        listener.exceptionOccurred(pID, e, metadata);
                                    }
                                }
                            }
                            
                            /**
                             * UNREGISTER Signal
                             */
                            if (signalArgs.get("topic").equals("unregister")) {
                                try {
                                    handleMemberLeave(packet);
                                } catch (Exception e) {
                                    // NOTIFY LISTENERS
                                    for (RemoteProcessClientListener listener : listeners) {
                                        
                                        Map<String, Object> metadata = new HashMap<String, Object>();
                                        metadata.put("serviceJID", packet.getFrom());
                                        
                                        final String pID = (signalArgs != null ? signalArgs.get("id") : null);
                                        
                                        listener.exceptionOccurred(pID, e, metadata);
                                    }
                                }
                            }
                            
                            /**
                             * PROGRESS Signal
                             */
                            if (signalArgs.get("topic").equals("progress")) {
                                final String pID = signalArgs.get("id");
                                final Double progress = Double.parseDouble(signalArgs.get("message"));
                                
                                // NOTIFY LISTENERS
                                for (RemoteProcessClientListener listener : listeners) {
                                    listener.progress(pID, progress);
                                }
                            }
                            
                            /**
                             * COMPLETED Signal
                             */
                            if (signalArgs.get("topic").equals("completed")) {
                                final String pID = signalArgs.get("id");
                                final String type = signalArgs.get("message");
                                
                                // NOTIFY LISTENERS
                                if ( "textual".equals(type) ) {
                                    Object outputs;
                                    try {
                                        String serviceResultString = URLDecoder.decode(signalArgs.get("result"), "UTF-8");
                                        JSONObject serviceResultJSON = (JSONObject) JSONSerializer.toJSON(serviceResultString);
                                        outputs = U(P(serviceResultJSON));
                                        for (RemoteProcessClientListener listener : listeners) {
                                            listener.complete(pID, outputs);
                                        }
                                    } catch (PickleException e) {
                                        LOGGER.log(Level.FINER, e.getMessage(), e);
                                    } catch (IOException e) {
                                        LOGGER.log(Level.FINER, e.getMessage(), e);
                                    }
                                }

                                // NOTIFY SERVICE
                                final String serviceJID = message.getFrom();
                                sendMessage(serviceJID, "topic=finish");
                            }
                            
                            /**
                             * ERROR Signal
                             */
                            if (signalArgs.get("topic").equals("error")) {
                                Map<String, Object> metadata = new HashMap<String, Object>();
                                metadata.put("serviceJID", packet.getFrom());
                                
                                Exception cause = null;
                                try {
                                    cause = new Exception(URLDecoder.decode(signalArgs.get("message"), "UTF-8"));
                                } catch (UnsupportedEncodingException e) {
                                    cause = e;
                                }
                                final String pID = (signalArgs != null ? signalArgs.get("id") : null);

                                // NOTIFY SERVICE
                                final String serviceJID = message.getFrom();
                                sendMessage(serviceJID, "topic=abort");

                                // NOTIFY LISTENERS
                                for (RemoteProcessClientListener listener : listeners) {
                                    listener.exceptionOccurred(pID, cause, metadata);
                                }
                            }
                            
                        }
                    }
                }
            }
        };

        // PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        connection.addPacketListener(packetListener, null);
    }

    protected void sendInvitations() throws Exception {
        for (MultiUserChat mucServiceChannel : mucServiceChannels) {
            for (String occupant : mucServiceChannel.getOccupants()) {
                final Name serviceName = extractServiceName(occupant);
                if (!registeredServices.contains(serviceName)) {
                    sendMessage(occupant, "topic=invite");
                    registeredServices.add(serviceName);
                }
            }
        }
    }

    protected void handleMemberJoin(Presence p) throws Exception {
        System.out.println("Member " + p.getFrom() + " joined the chat.");
        final Name serviceName = extractServiceName(p.getFrom());
        if (!registeredServices.contains(serviceName)) {
            sendMessage(p.getFrom(), "topic=invite");
            registeredServices.add(serviceName);
        }
    }

    protected void handleMemberLeave(Packet p) throws Exception {
        System.out.println("Member " + p.getFrom() + " leaved the chat.");
        final Name serviceName = extractServiceName(p.getFrom());
        if (registeredServices.contains(serviceName)) {
            registeredServices.remove(serviceName);
        }
        for (RemoteProcessClientListener listener : listeners) {
            listener.deregisterService(serviceName);
        }
    }

    /**
     * @param p
     * @return
     */
    private static NameImpl extractServiceName(String person) throws Exception {
        String occupantFlatName = null;
        if (person.lastIndexOf("@") < person.indexOf("/")) {
            occupantFlatName = person.substring(person.indexOf("/") + 1);
        } else {
            occupantFlatName = person.substring(person.indexOf("/") + 1);
            occupantFlatName = occupantFlatName.substring(0, occupantFlatName.indexOf("@"));
        }
        
        if ( occupantFlatName.indexOf(".") > 0) {
            final String serviceName[] = occupantFlatName.split("\\.");
            return new NameImpl(serviceName[0], serviceName[1]);
        } else {
            return new NameImpl(occupantFlatName, occupantFlatName);
        }
    }

    /*
     * Conversation setup!
     * 
     * Messages should be moved here once we get this working properly
     */
    public Chat setupChat(final String origin/* , final String person */) {
        MessageListener listener = new MessageListener() {
            public void processMessage(Chat chat, Message message) {
                // TODO: Fix this so that this actually does something!
            }
        };

        Chat chat = chatManager.createChat(origin, listener);
        openChat.put(origin, chat);
        return chat;
    }

    /*
     * This is the code that handles HTML messages
     */
    public void sendMessage(String person, String message) {
        Chat chat = openChat.get(person);
        if (chat == null)
            chat = setupChat(person);
        try {
            chat.sendMessage(message);
        } catch (XMPPException e) {
            LOGGER.log(Level.SEVERE, "xmppClient._ReceiveError", e);
        } catch (NotConnectedException e) {
            LOGGER.log(Level.SEVERE, "xmppClient._ReceiveError", e);
        }
    }

    /*
     * Roster Code
     */
    public void getRoster() {
        if (roster != null) {
            Collection<RosterEntry> entries = roster.getEntries();
            for (RosterEntry entry : entries) {
                // Access the WebView and pass the entries back to the Javascript
                // Most likely to the EventBroadcaster
                String name = entry.getName();
                String user = entry.getUser();
                ItemStatus entry_status = entry.getStatus();
                String status = "unknown";
                if (entry_status != null)
                    status = entry_status.toString();
            }
        }
    }

    /*
     * This handles changes in the roster, and all presence information
     */
    public void setupRosterListener() {
        if (roster == null)
            roster = connection.getRoster();
        RosterListener rListen = new RosterListener() {

            public void entriesAdded(Collection<String> arg0) {
                for (String str : arg0) {
                }
            }

            public void entriesDeleted(Collection<String> arg0) {
                for (String str : arg0) {

                }

            }

            public void entriesUpdated(Collection<String> arg0) {
                for (String str : arg0) {

                }
            }

            public void presenceChanged(Presence arg0) {
                String presence = arg0.getFrom();
            }

        };
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return the bus
     */
    public String getBus() {
        return bus;
    }

    /**
     * @param bus the bus to set
     */
    public void setBus(String bus) {
        this.bus = bus;
    }

    /**
     * @return the roomManagerUser
     */
    public String getRoomManagerUser() {
        return roomManagerUser;
    }

    /**
     * @param roomManagerUser the roomManagerUser to set
     */
    public void setRoomManagerUser(String roomManagerUser) {
        this.roomManagerUser = roomManagerUser;
    }

    /**
     * @return the roomManagerPassword
     */
    public String getRoomManagerPassword() {
        return roomManagerPassword;
    }

    /**
     * @param roomManagerPassword the roomManagerPassword to set
     */
    public void setRoomManagerPassword(String roomManagerPassword) {
        this.roomManagerPassword = roomManagerPassword;
    }

    /**
     * @return the managementChannel
     */
    public String getManagementChannel() {
        return managementChannel;
    }

    /**
     * @param managementChannel the managementChannel to set
     */
    public void setManagementChannel(String managementChannel) {
        this.managementChannel = managementChannel;
    }

    /**
     * @return the serviceChannels
     */
    public List<String> getServiceChannels() {
        return serviceChannels;
    }

    /**
     * @param serviceChannels the serviceChannels to set
     */
    public void setServiceChannels(List<String> serviceChannels) {
        this.serviceChannels = serviceChannels;
    }

    @Override
    public void registerListener(RemoteProcessClientListener listener) {
        listeners.add(listener);
    }

    @Override
    public void deregisterListener(RemoteProcessClientListener listener) {
        listeners.remove(listener);
    }

    /**
     * Keep connection alive and check for network changes by sending ping packets
     */

    Thread pingThread;

    private static int ping_task_generation = 1;

    void startPingTask() {
        // Schedule a ping task to run.
        PingTask task = new PingTask();
        pingThread = new Thread(task);
        task.setThread(pingThread);
        pingThread.setDaemon(true);
        pingThread.setName("XmppConnection Pinger " + ping_task_generation);
        ping_task_generation++;
        pingThread.start();
    }

    void stopPingTask() {
        pingThread = null;
    }

    class PingTask implements Runnable {

        private static final int INITIAL_PING_DELAY = 20000;

        private static final int PING_INTERVAL = 30000;

        private static final long PING_TIMEOUT = 10000;

        private long delay;

        private Thread thread;

        public PingTask() {
            this.delay = PING_INTERVAL;
        }

        protected void setThread(Thread thread) {
            this.thread = thread;
        }

        private boolean sendPing() throws NotConnectedException {
            IQ req = new IQ() {
                public String getChildElementXML() {
                    return "<ping xmlns='urn:xmpp:ping'/>";
                }
            };
            req.setType(IQ.Type.GET);
            PacketFilter filter = new AndFilter(new PacketIDFilter(req.getPacketID()),
                    new PacketTypeFilter(IQ.class));
            PacketCollector collector = connection.createPacketCollector(filter);
            connection.sendPacket(req);
            IQ result = (IQ) collector.nextResult(PING_TIMEOUT);
            if (result == null) {
                LOGGER.warning("ping timeout");
                return false;
            }
            collector.cancel();
            return true;
        }

        public void run() {
            try {
                // Sleep before sending first heartbeat. This will give time to
                // properly finish logging in.
                Thread.sleep(INITIAL_PING_DELAY);
            } catch (InterruptedException ie) {
                // Do nothing
            }
            while (connection != null && pingThread == thread) {
                if (connection.isConnected() && connection.isAuthenticated()) {
                    LOGGER.log(Level.FINER, "ping");
                    try {
                        if (!sendPing()) {
                            LOGGER.severe("ping failed - close connection");
                            try {
                                connection.disconnect();
                            } catch (NotConnectedException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                    } catch (NotConnectedException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
                try {
                    // Sleep until we should write the next keep-alive.
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    // Do nothing
                }
            }
            LOGGER.log(Level.FINER, "pinger exit");
        }
    }

    public void disconnect() throws NotConnectedException {
        connection.disconnect();
    }

    public void processMessage(Chat chat, Message message) {
        System.out.println("Received something: " + message.getBody());
        if (message.getType() == Message.Type.chat)
            System.out.println(chat.getParticipant() + " says: " + message.getBody());
    }

    static Object U(String strdata) throws PickleException, IOException {
        return U(PickleUtils.str2bytes(strdata));
    }

    static Object U(byte[] data) throws PickleException, IOException {
        Unpickler u = new Unpickler();
        Object o = u.loads(data);
        u.close();
        return o;
    }

    static byte[] B(String s) throws IOException {
        try {
            byte[] bytes = PickleUtils.str2bytes(s);
            byte[] result = new byte[bytes.length + 3];
            result[0] = (byte) Opcodes.PROTO;
            result[1] = 2;
            result[result.length - 1] = (byte) Opcodes.STOP;
            System.arraycopy(bytes, 0, result, 2, bytes.length);
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    static byte[] B(short[] shorts) {
        byte[] result = new byte[shorts.length + 3];
        result[0] = (byte) Opcodes.PROTO;
        result[1] = 2;
        result[result.length - 1] = (byte) Opcodes.STOP;
        for (int i = 0; i < shorts.length; ++i) {
            result[i + 2] = (byte) shorts[i];
        }
        return result;
    }

    static byte[] P(Object unpickled) throws PickleException, IOException {
        Pickler p = new Pickler();
        return p.dumps(unpickled);
    }

    /**
     * 
     * @param message
     * @return
     */
    public static String md5Java(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            // converting byte array to Hexadecimal String
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.toString();
        } catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return digest;
    }
    
    /**
     * Convert a byte array to a URL encoded string
     * 
     * @param in byte[]
     * @return String
     */
    public static String byteArrayToURLString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0)
            return null;

        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D",
                "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            // First check to see if we need ASCII or HEX
            if ((in[i] >= '0' && in[i] <= '9') || (in[i] >= 'a' && in[i] <= 'z')
                    || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$' || in[i] == '-'
                    || in[i] == '_' || in[i] == '.' || in[i] == '!') {
                out.append((char) in[i]);
                i++;
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0); // Strip off high nibble
                ch = (byte) (ch >>> 4); // shift the bits down
                ch = (byte) (ch & 0x0F); // must do this is high order bit is
                // on!
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                ch = (byte) (in[i] & 0x0F); // Strip off low nibble
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                i++;
            }
        }

        String rslt = new String(out);

        return rslt;
    }
}
