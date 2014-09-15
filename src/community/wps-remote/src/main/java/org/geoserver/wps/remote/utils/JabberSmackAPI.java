package org.geoserver.wps.remote.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import net.razorvine.pickle.Opcodes;
import net.razorvine.pickle.PickleException;
import net.razorvine.pickle.PickleUtils;
import net.razorvine.pickle.Pickler;
import net.razorvine.pickle.Unpickler;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class JabberSmackAPI implements MessageListener {

    private XMPPConnection connection;

    private final String mHost = "whale.nurc.nato.int"; // server IP address or the
                                                   // host

    public void login(String userName, String password) throws XMPPException, SmackException, IOException {
        String service = StringUtils.parseServer(userName);
        String user_name = StringUtils.parseName(userName);

        ConnectionConfiguration config = new ConnectionConfiguration(mHost, 5222, service);

        config.setSendPresence(true);
        config.setDebuggerEnabled(false);

        connection = new XMPPTCPConnection(config);
        connection.connect();
        connection.login(user_name, password);
    }

    public void sendMessage(String message, String to) throws XMPPException, NotConnectedException {
        Chat chat = ChatManager.getInstanceFor(connection).createChat(to, this);
        chat.sendMessage(message);
    }

    public void displayBuddyList() {
        Roster roster = connection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();

        System.out.println("\n\n" + entries.size() + " buddy(ies):");
        for (RosterEntry r : entries) {
            System.out.println(r.getUser());
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
        Pickler p=new Pickler();
        return p.dumps(unpickled);
    }

    public static void main(String args[]) throws XMPPException, IOException, SmackException {
        // declare variables
        JabberSmackAPI c = new JabberSmackAPI();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String msg;

        Object unpickled = U("(dp0\nS'child'\np1\n(lp2\nVAlex\np3\nasS'name'\np4\nVHans\np5\nsS'surname'\np6\nVMeier\np7\ns.");
        System.out.println(unpickled);
        
        c.login("admin@whale.nurc.nato.int", "Crociera100!");
        c.displayBuddyList();

        for ( String service : MultiUserChat.getServiceNames(c.connection) ) {
            System.out.println(" --- Service: " + service);
            
            for ( HostedRoom room : MultiUserChat.getHostedRooms(c.connection, service) ) {
                System.out.println(" ------ ROOM ("+room.getName()+") : " + room.getJid());                
            }
        }
        
        MultiUserChat muc = new MultiUserChat(c.connection, "default@conference.whale.nurc.nato.int");
        muc.join("admin");
        
        for ( String occupant : muc.getOccupants() ) {
            msg = "topic=invite";
            c.sendMessage(msg, occupant);
            
            for (int cnt=31; cnt<35; cnt++) {
                msg = "topic=request&id="+cnt+"d4a_5a147f4f3e123bbd0728b29da3557504&message="+byteArrayToURLString(P(unpickled));
                c.sendMessage(msg, occupant);
                
                System.out.println("[SENT] \""+msg+"\" to: " + occupant);                
            }
            
        }
        
        // turn on the enhanced debugger
        //XMPPCPConnection.DEBUG_ENABLED = true;

        // Enter your login information here
        System.out.println("-----");
        /*System.out.println("Login information:");

        System.out.print("username: ");
        String login_username = br.readLine();

        System.out.print("password: ");
        String login_pass = br.readLine();

        c.login(login_username, login_pass);

        c.displayBuddyList();

        System.out.println("-----");

        System.out.println("Who do you want to talk to? - Type contacts full email address:");
        String talkTo = br.readLine();

        System.out.println("-----");
        System.out.println("All messages will be sent to " + talkTo);
        System.out.println("Enter your message in the console:");
        System.out.println("-----\n");

        while (!(msg = br.readLine()).equals("bye")) {
            c.sendMessage(msg, talkTo);
        }*/
        
        try {
            Thread.sleep(60000);
            for ( String occupant : muc.getOccupants() ) {
                msg = "topic=finish";
                c.sendMessage(msg, occupant);
                
                System.out.println("[SENT] \""+msg+"\" to: " + occupant);
            }
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            c.disconnect();            
        }
        
        System.exit(0);
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