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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

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
import org.geoserver.wps.remote.RemoteProcessFactoryConfigurationWatcher;
import org.geoserver.wps.remote.RemoteProcessFactoryListener;
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
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * XMPP implementation of the {@link RemoteProcessClient}
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class XMPPClient extends RemoteProcessClient {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPClient.class.getPackage().getName());

    private static final int DEFAULT_PACKET_REPLY_TIMEOUT = 500; // millis

    /** The XMPP Server endpoint */
    private String server;

    /** The XMPP Server port */
    private int port;

    /**
     * XMPP specific parameters and properties
     */
    private XMPPConnection connection;

    private ConnectionConfiguration config;

    private ChatManager chatManager;

    private PacketListener packetListener;

    private ServiceDiscoveryManager discoStu;

    private Map<String, Chat> openChat = Collections.synchronizedMap(new HashMap<String, Chat>());

    private String domain;

    private String bus;

    private String managementChannelUser;

    private String managementChannelPassword;

    protected String managementChannel;

    protected List<String> serviceChannels;

    /**
     * Private structures
     */
    protected List<Name> registeredServices = Collections.synchronizedList(new ArrayList<Name>());

    protected List<MultiUserChat> mucServiceChannels = new ArrayList<MultiUserChat>();

    protected MultiUserChat mucManagementChannel;

    /**
     * Default Constructor
     * 
     * @param remoteProcessFactoryConfigurationWatcher
     * @param enabled
     */
    public XMPPClient(
            RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher,
            boolean enabled, int priority) {
        super(remoteProcessFactoryConfigurationWatcher, enabled, priority);
        this.server = getConfiguration().get("xmpp_server");
        this.port = Integer.parseInt(getConfiguration().get("xmpp_port"));
        this.domain = getConfiguration().get("xmpp_domain");
        this.bus = getConfiguration().get("xmpp_bus");
        this.managementChannelUser = getConfiguration().get("xmpp_management_channel_user");
        this.managementChannelPassword = getConfiguration().get("xmpp_management_channel_pwd");
        this.managementChannel = getConfiguration().get("xmpp_management_channel");

        this.serviceChannels = new ArrayList<String>();

        String[] serviceNamespaces = getConfiguration().get("xmpp_service_channels").split(",");
        for (int sc = 0; sc < serviceNamespaces.length; sc++) {
            this.serviceChannels.add(serviceNamespaces[sc].trim());
        }
    }

    @Override
    public void init(SSLContext customSSLContext) throws Exception {

        // Initializes the XMPP Client and starts the communication. It also register GeoServer as "manager" to the service channels on the MUC (Multi
        // User Channel) Rooms
        LOGGER.info(String.format("Initializing connection to server %1$s port %2$d", server, port));

        int packetReplyTimeout = DEFAULT_PACKET_REPLY_TIMEOUT;
        if (getConfiguration().get("xmpp_packet_reply_timeout") != null) {
            packetReplyTimeout = Integer.parseInt(getConfiguration().get(
                    "xmpp_packet_reply_timeout"));
        }
        SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTimeout);

        config = new ConnectionConfiguration(server, port);
        if (customSSLContext != null) {
            // config.setSASLAuthenticationEnabled(false);
            config.setSecurityMode(SecurityMode.enabled);
            config.setCustomSSLContext(customSSLContext);
        } else {
            config.setSecurityMode(SecurityMode.disabled);
        }

        connection = new XMPPTCPConnection(config);
        connection.connect();

        LOGGER.info("Connected: " + connection.isConnected());

        // check if the connection to the XMPP server is successful; the login and registration is not yet performed at this time
        if (connection.isConnected()) {
            chatManager = ChatManager.getInstanceFor(connection);
            discoStu = ServiceDiscoveryManager.getInstanceFor(connection);

            //
            discoProperties();

            //
            performLogin(getConfiguration().get("xmpp_manager_username"),
                    getConfiguration().get("xmpp_manager_password"));

            //
            startPingTask();

            //
            sendInvitations();
        } else {
            setEnabled(false);
        }
    }

    @Override
    public String execute(Name name, Map<String, Object> input, Map<String, Object> metadata,
            ProgressListener monitor) throws Exception {

        // Check for a free machine...
        final String serviceJID = getFlattestMachine(name, (String) metadata.get("serviceJID"));
        if (metadata != null && serviceJID != null) {
            // Extract the PID
            metadata.put("serviceJID", serviceJID);
            final String pid = md5Java(serviceJID + System.nanoTime()) + "_"
                    + md5Java(byteArrayToURLString(P(input)));

            String msg = "topic=request&id=" + pid + "&message=" + byteArrayToURLString(P(input));
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

    /**
     * Logins as manager to the XMPP Server and registers to the service channels management chat rooms
     * 
     * @param username
     * @param password
     * @throws Exception
     */
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
            mucManagementChannel.join(managementChannelUser, managementChannelPassword, history,
                    connection.getPacketReplyTimeout());

            for (String channel : serviceChannels) {
                MultiUserChat serviceChannel = new MultiUserChat(connection, channel + "@" + bus
                        + "." + domain);
                serviceChannel.join(managementChannelUser, managementChannelPassword, history,
                        connection.getPacketReplyTimeout());
                mucServiceChannels.add(serviceChannel);
            }

            //
            setStatus(true, "Orchestrator Active");

            //
            setupListeners();
        }
    }

    /**
     * Declare the status on the XMPP Chat
     * 
     * @param available
     * @param status
     * @throws Exception
     */
    public void setStatus(boolean available, String status) throws Exception {
        Presence.Type type = available ? Type.available : Type.unavailable;
        Presence presence = new Presence(type);

        presence.setStatus(status);
        connection.sendPacket(presence);
    }

    /**
     * Destroy the connection
     */
    public void destroy() throws Exception {
        if (connection != null && connection.isConnected()) {
            stopPingTask();
            connection.disconnect();
        }
    }

    /**
     * 
     * 
     * @param user
     * @param name
     * @throws Exception
     */
    public void createEntry(String user, String name) throws Exception {
        LOGGER.fine(String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
        Roster roster = connection.getRoster();
        roster.createEntry(user, name, null);
    }

    /**
     * This handles the chat listener. We can't simply listen to chats for some reason, and instead have to grab the chats from the packets. The other
     * listeners work properly in SMACK
     */
    public void setupListeners() {
        /*
         * This is the actual code that handles what happens with XMPP users
         */
        packetListener = new XMPPPacketListener(this);

        // PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        connection.addPacketListener(packetListener, null);
    }

    /**
     * Conversation setup!
     * 
     * Messages should be moved here once we get this working properly
     */
    public Chat setupChat(final String origin) {
        synchronized (openChat) {
            if (openChat.get(origin) != null) {
                return openChat.get(origin);
            }

            MessageListener listener = new MessageListener() {
                public void processMessage(Chat chat, Message message) {
                    // TODO: Fix this so that this actually does something!
                }
            };

            Chat chat = chatManager.createChat(origin, listener);
            openChat.put(origin, chat);
            return chat;
        }
    }

    /**
     * This is the code that handles HTML messages
     */
    public void sendMessage(String person, String message) {
        synchronized (openChat) {
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
    }

    /**
     * Close the XMPP connection
     * 
     * @throws NotConnectedException
     */
    public void disconnect() throws NotConnectedException {
        connection.disconnect();
    }

    /*
     * @param person
     * 
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

        if (occupantFlatName.indexOf(".") > 0) {
            final String serviceName[] = occupantFlatName.split("\\.");
            return new NameImpl(serviceName[0], serviceName[1]);
        } else {
            return new NameImpl(occupantFlatName, occupantFlatName);
        }
    }

    /*
     * Send an invitation to the new logged in member
     * 
     * @throws Exception
     */
    protected void sendInvitations() throws Exception {
        synchronized (registeredServices) {
            for (MultiUserChat mucServiceChannel : mucServiceChannels) {
                for (String occupant : mucServiceChannel.getOccupants()) {
                    final Name serviceName = extractServiceName(occupant);

                    // send invitation and register source JID
                    String[] serviceJIDParts = occupant.split("/");
                    if (serviceJIDParts.length == 3
                            && (serviceJIDParts[2].startsWith("master") || serviceJIDParts[2]
                                    .indexOf("@") < 0)) {
                        sendMessage(occupant, "topic=invite");
                    }
                    // register service on listeners
                    if (!registeredServices.contains(serviceName)) {
                        registeredServices.add(serviceName);
                    }
                }
            }
        }
    }

    /*
     * A new member joined one of the service chat-rooms; send an invitation and see if it is a remote service. If so, register it
     * 
     * @param p
     * 
     * @throws Exception
     */
    protected void handleMemberJoin(Presence p) throws Exception {
        synchronized (registeredServices) {
            LOGGER.finer("Member " + p.getFrom() + " joined the chat.");
            final Name serviceName = extractServiceName(p.getFrom());

            // send invitation and register source JID
            String[] serviceJIDParts = p.getFrom().split("/");

            if (serviceJIDParts.length == 3
                    && (serviceJIDParts[2].startsWith("master") || serviceJIDParts[2].indexOf("@") < 0)) {
                sendMessage(p.getFrom(), "topic=invite");
            }

            if (!registeredServices.contains(serviceName)) {
                registeredServices.add(serviceName);
            }
        }
    }

    /*
     * A member leaved one of the service chat-rooms; lets remove the service declaration and de-register it
     * 
     * @param p
     * 
     * @throws Exception
     */
    protected void handleMemberLeave(Packet p) throws Exception {
        final Name serviceName = extractServiceName(p.getFrom());

        synchronized (registeredServices) {
            LOGGER.finer("Member " + p.getFrom() + " leaved the chat.");
            if (registeredServices.contains(serviceName)) {
                registeredServices.remove(serviceName);
            }
        }

        for (RemoteProcessFactoryListener listener : getRemoteFactoryListeners()) {
            listener.deregisterService(serviceName);
        }
    }

    /*
     * Find the service by name with the smallest amount of processes running, channel is decoded in service name
     * 
     * e.g. debug.foo@bar/service@localhost
     * 
     * @param service name
     * 
     * @param candidateServiceJID
     * 
     * @return
     */
    private String getFlattestMachine(Name name, String candidateServiceJID) {
        final String serviceName = name.getLocalPart();

        Map<String, List<String>> availableServices = new HashMap<String, List<String>>();
        Map<String, List<String>> availableServiceJIDs = new HashMap<String, List<String>>();

        for (MultiUserChat muc : this.mucServiceChannels) {

            for (String occupant : muc.getOccupants()) {

                if (occupant.contains(serviceName)) {

                    // extracting the machine name
                    String[] serviceJIDParts = occupant.split("/");
                    if (serviceJIDParts.length > 1) {
                        String[] localizedServiceJID = serviceJIDParts[1].split("@");

                        if (localizedServiceJID.length == 2
                                && localizedServiceJID[0].contains(serviceName)) {
                            // final String machine = localizedServiceJID[1];
                            final String machine = occupant
                                    .substring(occupant.lastIndexOf("@") + 1);

                            if (availableServices.get(machine) == null) {
                                availableServices.put(machine, new ArrayList<String>());
                            }
                            if (availableServiceJIDs.get(machine) == null) {
                                availableServiceJIDs.put(machine, new ArrayList<String>());
                            }

                            availableServices.get(machine).add(occupant);
                            if (serviceJIDParts.length == 3
                                    && (serviceJIDParts[2].startsWith("master") || serviceJIDParts[2]
                                            .indexOf("@") < 0)) {
                                availableServiceJIDs.get(machine).add(occupant);
                            }
                        }
                    }
                }
            }
        }

        if (availableServices == null || availableServices.isEmpty())
            return candidateServiceJID;

        String targetMachine = null;
        String targetServiceJID = null;
        int targetMachineCounter = Integer.MAX_VALUE;
        for (String machine : availableServices.keySet()) {
            if (targetMachine == null
                    || targetMachineCounter < availableServices.get(machine).size()) {
                targetMachine = machine;
                targetServiceJID = availableServiceJIDs.get(machine).get(0);
                targetMachineCounter = availableServices.get(machine).size();
            }
        }

        return targetServiceJID;
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

        private static final long DEFAULT_INITIAL_PING_DELAY = 20000;

        private static final long DEFAULT_PING_INTERVAL = 30000;

        private static final long DEFAULT_PING_TIMEOUT = 10000;

        private long delay;

        private long timeout;

        private long start_delay;

        private Thread thread;

        /**
         * 
         */
        public PingTask() {
            this.delay = DEFAULT_PING_INTERVAL;
            if (getConfiguration().get("xmpp_connection_ping_interval") != null) {
                this.delay = Long
                        .parseLong(getConfiguration().get("xmpp_connection_ping_interval"));
            }

            this.timeout = DEFAULT_PING_TIMEOUT;
            if (getConfiguration().get("xmpp_connection_ping_timeout") != null) {
                this.timeout = Long.parseLong(getConfiguration()
                        .get("xmpp_connection_ping_timeout"));
            }

            this.start_delay = DEFAULT_INITIAL_PING_DELAY;
            if (getConfiguration().get("xmpp_connection_ping_initial_delay") != null) {
                this.start_delay = Long.parseLong(getConfiguration().get(
                        "xmpp_connection_ping_initial_delay"));
            }
        }

        /**
         * 
         * @param thread
         */
        protected void setThread(Thread thread) {
            this.thread = thread;
        }

        /**
         * 
         * @return
         * @throws NotConnectedException
         */
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
            IQ result = (IQ) collector.nextResult(timeout);
            if (result == null) {
                LOGGER.warning("ping timeout");
                return false;
            }
            collector.cancel();
            return true;
        }

        /**
         * 
         */
        public void run() {
            try {
                // Sleep before sending first heartbeat. This will give time to
                // properly finish logging in.
                Thread.sleep(start_delay);
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

    /**
     * 
     * @param strdata
     * @return
     * @throws PickleException
     * @throws IOException
     */
    static Object U(String strdata) throws PickleException, IOException {
        return U(PickleUtils.str2bytes(strdata));
    }

    /**
     * 
     * @param data
     * @return
     * @throws PickleException
     * @throws IOException
     */
    static Object U(byte[] data) throws PickleException, IOException {
        Unpickler u = new Unpickler();
        Object o = u.loads(data);
        u.close();
        return o;
    }

    /**
     * 
     * @param s
     * @return
     * @throws IOException
     */
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

    /**
     * 
     * @param shorts
     * @return
     */
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

    /** Primitive type name -> class map. */
    private static final Map<String, Object> PRIMITIVE_NAME_TYPE_MAP = new HashMap<String, Object>();

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
    final static Class convertToJavaClass(String name, ClassLoader cl)
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
}

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
class XMPPPacketListener implements PacketListener {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPPacketListener.class.getPackage()
            .getName());

    private XMPPClient xmppClient;

    public XMPPPacketListener(XMPPClient xmppClient) {
        this.xmppClient = xmppClient;
    }

    @Override
    public void processPacket(Packet packet) {
        if (packet instanceof Presence) {
            Presence p = (Presence) packet;

            try {
                if (p.isAvailable()) {
                    if (p.getFrom().indexOf("@") > 0) {
                        final String channel = p.getFrom().substring(0, p.getFrom().indexOf("@"));
                        if (xmppClient.serviceChannels.contains(channel))
                            xmppClient.handleMemberJoin(p);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        } else if (packet instanceof Message) {
            Message message = (Message) packet;
            String origin = message.getFrom().split("/")[0];
            Chat chat = xmppClient.setupChat(origin);

            if (message.getBody() != null) {
                LOGGER.fine("ReceivedMessage('" + message.getBody() + "','" + origin + "','"
                        + message.getPacketID() + "');");

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
                        handleRegisterSignal(packet, message, signalArgs);
                    }

                    /**
                     * UNREGISTER Signal
                     */
                    if (signalArgs.get("topic").equals("unregister")) {
                        handleUnRegisterSignal(packet, message, signalArgs);
                    }

                    /**
                     * PROGRESS Signal
                     */
                    if (signalArgs.get("topic").equals("progress")) {
                        handleProgressSignal(packet, message, signalArgs);
                    }

                    /**
                     * COMPLETED Signal
                     */
                    if (signalArgs.get("topic").equals("completed")) {
                        handleCompletedSignal(packet, message, signalArgs);
                    }

                    /**
                     * ERROR Signal
                     */
                    if (signalArgs.get("topic").equals("error")) {
                        handleErrorSignal(packet, message, signalArgs);
                    }

                }
            }
        }
    }

    /**
     * @param packet
     * @param message
     * @param signalArgs
     */
    protected void handleErrorSignal(Packet packet, Message message, Map<String, String> signalArgs) {
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
        xmppClient.sendMessage(serviceJID, "topic=abort");

        // NOTIFY LISTENERS
        for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
            listener.exceptionOccurred(pID, cause, metadata);
        }
    }

    /**
     * @param packet
     * @param message
     * @param signalArgs
     */
    protected void handleCompletedSignal(Packet packet, Message message,
            Map<String, String> signalArgs) {
        final String pID = signalArgs.get("id");
        final String type = signalArgs.get("message");

        // NOTIFY LISTENERS
        if ("textual".equals(type)) {
            Object outputs;
            try {
                String serviceResultString = URLDecoder.decode(signalArgs.get("result"), "UTF-8");
                JSONObject serviceResultJSON = (JSONObject) JSONSerializer
                        .toJSON(serviceResultString);
                outputs = xmppClient.U(xmppClient.P(serviceResultJSON));
                for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
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
        xmppClient.sendMessage(serviceJID, "topic=finish");
    }

    /**
     * @param packet
     * @param message
     * @param signalArgs
     * 
     * @throws NumberFormatException
     */
    protected void handleProgressSignal(Packet packet, Message message,
            Map<String, String> signalArgs) throws NumberFormatException {
        final String pID = signalArgs.get("id");
        final Double progress = Double.parseDouble(signalArgs.get("message"));

        // NOTIFY LISTENERS
        for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
            listener.progress(pID, progress);
        }
    }

    /**
     * @param packet
     * @param message
     * @param signalArgs
     */
    protected void handleUnRegisterSignal(Packet packet, Message message,
            Map<String, String> signalArgs) {
        try {
            xmppClient.handleMemberLeave(packet);
        } catch (Exception e) {
            // NOTIFY LISTENERS
            for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {

                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put("serviceJID", packet.getFrom());

                final String pID = (signalArgs != null ? signalArgs.get("id") : null);

                listener.exceptionOccurred(pID, e, metadata);
            }
        }
    }

    /**
     * @param packet
     * @param message
     * @param signalArgs
     */
    protected void handleRegisterSignal(Packet packet, Message message,
            Map<String, String> signalArgs) {
        final String serviceName[] = signalArgs.get("service").split("\\.");
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
            for (int ii = 0; ii < input.size(); ii++) {
                Object obj = input.get(ii);
                if (obj instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) obj;

                    String paramName = (String) jsonArray.get(0);
                    String ss = ((String) jsonArray.get(1));
                    ss = ss.substring(1, ss.length() - 1);
                    JSONObject paramType = (JSONObject) JSONSerializer.toJSON(ss);
                    String className = (String) paramType.get("type");
                    Class clazz = xmppClient.convertToJavaClass(className,
                            XMPPClient.class.getClassLoader());

                    inputs.put(paramName, new Parameter(paramName, clazz, Text.text(paramName),
                            Text.text((String) paramType.get("description")),
                            paramType.get("min") == null || (Integer) paramType.get("min") > 0,
                            paramType.get("min") != null ? (Integer) paramType.get("min") : 1,
                            paramType.get("max") != null ? (Integer) paramType.get("max") : -1,
                            paramType.get("default"), null));
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
                    JSONObject paramType = (JSONObject) JSONSerializer.toJSON(ss);
                    String className = (String) paramType.get("type");
                    Class clazz = xmppClient.convertToJavaClass(className,
                            XMPPClient.class.getClassLoader());

                    outputs.put(paramName, new Parameter(paramName, clazz, Text.text(paramName),
                            Text.text((String) paramType.get("description")),
                            paramType.get("min") == null || (Integer) paramType.get("min") > 0,
                            paramType.get("min") != null ? (Integer) paramType.get("min") : 1,
                            paramType.get("max") != null ? (Integer) paramType.get("max") : 0,
                            paramType.get("default"), null));
                }
            }

            // NOTIFY LISTENERS
            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("serviceJID", packet.getFrom());
            for (RemoteProcessFactoryListener listener : xmppClient.getRemoteFactoryListeners()) {
                listener.registerService(name, title, description, inputs, outputs, metadata);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);

            // NOTIFY LISTENERS
            for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {

                Map<String, Object> metadata = new HashMap<String, Object>();
                metadata.put("serviceJID", packet.getFrom());

                final String pID = (signalArgs != null ? signalArgs.get("id") : null);

                listener.exceptionOccurred(pID, e, metadata);
            }
        }
    }
}
