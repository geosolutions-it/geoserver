/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0049_privatedata.PrivateDataModule;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.remote.plugin.MockRemoteClient;
import org.geoserver.wps.remote.plugin.XMPPClient;
import org.geotools.factory.FactoryIteratorProvider;
import org.geotools.factory.GeoTools;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.junit.Test;
import org.opengis.feature.type.Name;

/**
 * This class tests checks if the RemoteProcess class behaves correctly.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class RemoteProcessTest extends WPSTestSupport {

    private RemoteProcessFactory factory;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("testRemoteAppContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // add limits properties file
        testData.copyTo(
                RemoteProcessTest.class.getClassLoader().getResourceAsStream(
                        "remote-process/remoteProcess.properties"), "remoteProcess.properties");

        testData.copyTo(
                RemoteProcessTest.class.getClassLoader().getResourceAsStream(
                        "remote-process/bogus_mina_tls.cert"), "bogus_mina_tls.cert");
    }

    @Test
    public void testNames() {
        setupFactory();

        assertNotNull(factory);
        Set<Name> names = factory.getNames();
        assertNotNull(names);
        assertTrue(names.size() == 0);

        final NameImpl name = new NameImpl("default", "Service");
        factory.registerService(name, "Service", "A test service", null, null, null);
        assertTrue(names.size() == 1);
        assertTrue(names.contains(name));

        factory.deregisterService(name);
        assertTrue(names.size() == 0);
    }

    @Test
    public void testListeners() {
        setupFactory();

        assertNotNull(factory);
        RemoteProcessClient remoteClient = factory.getRemoteClient();
        assertNotNull(remoteClient);
        assertTrue(remoteClient instanceof MockRemoteClient);

        Set<Name> names = factory.getNames();
        assertNotNull(names);
        assertTrue(names.size() == 0);

        final NameImpl name = new NameImpl("default", "Service");
        try {
            remoteClient.execute(name, null, null, null);
            assertTrue(names.size() == 1);
            assertTrue(names.contains(name));

            factory.deregisterService(name);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        } finally {
            assertTrue(names.size() == 0);
        }
    }

    @Test
    public void testXMPPClient() {
        setupFactory();

        try {
            // Start Server
            StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

            final AccountManagement accountManagement = (AccountManagement) providerRegistry
                    .retrieve(AccountManagement.class);

            final RemoteProcessFactoryConfiguration configuration = factory.getRemoteClient()
                    .getConfiguration();
            final String xmppDomain = configuration.get("xmpp_domain");
            final String xmppUserName = configuration.get("xmpp_manager_username");
            final String xmppUserPassword = configuration.get("xmpp_manager_password");

            if (!accountManagement.verifyAccountExists(EntityImpl.parse(xmppUserName + "@"
                    + xmppDomain))) {
                accountManagement.addUser(EntityImpl.parse(xmppUserName + "@" + xmppDomain),
                        xmppUserPassword);
            }

            XMPPServer server = new XMPPServer(xmppDomain);
            TCPEndpoint tcpEndpoint = new TCPEndpoint();
            tcpEndpoint.setPort(Integer.parseInt(configuration.get("xmpp_port")));
            server.addEndpoint(tcpEndpoint);
            server.setStorageProviderRegistry(providerRegistry);

            // setup CA
            final File certfile = new File(testData.getDataDirectoryRoot(), "bogus_mina_tls.cert");
            char[] password = "boguspw".toCharArray();

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream instream = new FileInputStream(certfile);
            try {
                trustStore.load(instream, password);
            } finally {
                instream.close();
            }

            // Trust own CA and all self-signed certs
            SSLContext sslcontext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();

            server.setTLSCertificateInfo(certfile, "boguspw");
            
            server.start();
            
            // other initialization
            server.addModule(new SoftwareVersionModule());
            server.addModule(new EntityTimeModule());
            server.addModule(new VcardTempModule());
            server.addModule(new XmppPingModule());
            server.addModule(new PrivateDataModule());

            Conference conference = new Conference(configuration.get("xmpp_bus"));
            server.addModule(new MUCModule(configuration.get("xmpp_bus"), conference));
            
            /**
            Entity managementRoomJID = EntityImpl.parseUnchecked(configuration.get("xmpp_management_channel") + "@" + xmppDomain);
            
            Room management = conference.findOrCreateRoom(managementRoomJID, configuration.get("xmpp_management_channel"));
            management.setPassword(configuration.get("xmpp_management_channel_pwd"));
            
            String[] serviceChannels = configuration.get("xmpp_service_channels").split(",");
            if (serviceChannels != null) {
                for (String channel : serviceChannels) {
                    Entity serviceRoomJID = EntityImpl.parseUnchecked(channel + "@" + xmppDomain);
                    conference.findOrCreateRoom(serviceRoomJID, channel);                    
                }
            }
            **/

            // /
            XMPPClient xmppRemoteClient = (XMPPClient) applicationContext
                    .getBean("xmppRemoteProcessClient");
            assertNotNull(xmppRemoteClient);

            xmppRemoteClient.init(sslcontext);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        }
    }

    /**
     * 
     */
    protected void setupFactory() {
        if (factory == null) {
            factory = new RemoteProcessFactory();

            // check SPI will see the factory if we register it using an iterator
            // provider
            GeoTools.addFactoryIteratorProvider(new FactoryIteratorProvider() {

                public <T> Iterator<T> iterator(Class<T> category) {
                    if (ProcessFactory.class.isAssignableFrom(category)) {
                        return (Iterator<T>) Collections.singletonList(factory).iterator();
                    } else {
                        return null;
                    }
                }
            });
        }
    }

    /**
     * 
     * @param fname
     * @return
     * @throws IOException
     */
    private static InputStream fullStream(File fname) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }

}
