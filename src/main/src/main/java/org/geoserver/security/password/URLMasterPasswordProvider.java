/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.SecurityUtils.scramble;
import static org.geoserver.security.SecurityUtils.toBytes;
import static org.geoserver.security.SecurityUtils.toChars;
import static org.geoserver.security.password.URLMasterPasswordProviderException.URL_LOCATION_NOT_READABLE;
import static org.geoserver.security.password.URLMasterPasswordProviderException.URL_REQUIRED;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.URLs;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;

/**
 * Master password provider that retrieves and optionally stores the master password from a url.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public final class URLMasterPasswordProvider extends MasterPasswordProvider {

    /** base encryption key */
    // static final char[] BASE = new char[]{ 'a', 'f', '8', 'd', 'f', 's', 's', 'v', 'j', 'K',
    // 'L',
    // '0', 'I', 'H', '(', 'a', 'd', 'f', '2', 's', '0', '0', 'd', 's', '9', 'f', '2', 'o',
    // 'f',
    // '(', '4', ']' };

    static final char[] BASE = new char[] { 'U', 'n', '6', 'd', 'I', 'l', 'X', 'T', 'Q', 'c', 'L',
            ')', '$', '#', 'q', 'J', 'U', 'l', 'X', 'Q', 'U', '!', 'n', 'n', 'p', '%', 'U', 'r',
            '5', 'U', 'u', '3', '5', 'H', '`', 'x', 'P', 'F', 'r', 'X' };

    /**
     * permutation indices, this permutation has a cycle of 169 --> more than 168 iterations have no effect
     */
    // static final int[] PERM = new int[]{25, 10, 5, 21, 14, 27, 23, 4, 3, 31, 16, 29, 20, 11,
    // 0, 26,
    // 24, 22, 13, 12, 1, 8, 18, 19, 7, 2, 17, 6, 9, 28, 30, 15};
    static final int[] PERM = new int[] { 32, 19, 30, 11, 34, 26, 3, 21, 9, 37, 38, 13, 23, 2, 18,
            4, 20, 1, 29, 17, 0, 31, 14, 36, 12, 24, 15, 35, 16, 39, 25, 5, 10, 8, 7, 6, 33, 27, 28,
            22 };

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    URLMasterPasswordProviderConfig config;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        this.config = (URLMasterPasswordProviderConfig) config;
    }

    @Override
    protected char[] doGetMasterPassword() throws Exception {
        try {
            return readInputPassword();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    protected char[] readInputPassword() throws IOException {
        InputStream in = null;
        try {
            if (!this.config.reset) {
                // JD: for some reason the decrypted passwd comes back sometimes with null chars
                // tacked on
                // MCR, was a problem with toBytes and toChar in SecurityUtils
                // return trimNullChars(toChars(decode(IOUtils.toByteArray(in))));
                in = input(config.getURL(), getConfigDir());
                return toChars(decode(IOUtils.toByteArray(in)));
            } else {
                final char[] newPasswdConfirm = randomAlphaNumeric(64).toCharArray();
                try {
                    in = input(config.getURL(), getConfigDir());
                    char[] currPasswd = toChars(decode(IOUtils.toByteArray(in)));
                    in.close();

                    // KeyStoreProvider ksProvider = getSecurityManager().getKeyStoreProvider();
                    KeyStoreProvider ksProvider = GeoServerExtensions.bean(KeyStoreProvider.class);
                    if (ksProvider == null) {
                        // use default key store provider
                        ksProvider = new KeyStoreProviderImpl();
                    }

                    ksProvider.setSecurityManager(getSecurityManager());
                    ksProvider.prepareForMasterPasswordChange(currPasswd, newPasswdConfirm);

                    if (!config.isReadOnly()) {
                        // write it back first
                        try {
                            doSetMasterPassword(newPasswdConfirm);
                        } catch (Exception e) {
                            throw new IOException(e);
                        } finally {
                            config.setReset(false);
                        }
                    }

                    // save the new Master Password config
                    getSecurityManager().saveMasterPasswordProviderConfig(config);

                    // redigest
                    GeoServerDigestPasswordEncoder pwEncoder = getSecurityManager()
                            .loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
                    String masterPasswdDigest = pwEncoder.encodePassword(newPasswdConfirm, null);
                    OutputStream fout = getSecurityManager().security()
                            .get(getSecurityManager().MASTER_PASSWD_DIGEST_FILENAME).out();
                    try {
                        IOUtils.write(masterPasswdDigest, fout);
                    } finally {
                        fout.close();
                    }

                    // commit the password change to the keystore
                    ksProvider.commitMasterPasswordChange();
                } catch (Exception e) {
                    throw new IOException(e);
                }
                return newPasswdConfirm;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    protected void doSetMasterPassword(char[] passwd) throws Exception {
        OutputStream out = output(config.getURL(), getConfigDir());
        try {
            out.write(encode(passwd));
        } finally {
            out.close();
        }
    }

    Resource getConfigDir() throws IOException {
        return getSecurityManager().masterPasswordProvider().get(getName());
    }

    byte[] encode(char[] passwd) {

        if (!config.isEncrypting()) {
            return toBytes(passwd);
        }

        // encrypt the password
        StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();

        char[] key = key();
        try {
            encryptor.setPasswordCharArray(key);
            return Base64.encodeBase64(encryptor.encrypt(toBytes(passwd)));
        } finally {
            scramble(key);
        }
    }

    byte[] decode(byte[] passwd) {
        if (!config.isEncrypting()) {
            return passwd;
        }

        // decrypt the password
        StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
        char[] key = key();
        try {
            encryptor.setPasswordCharArray(key);
            return encryptor.decrypt(Base64.decodeBase64(passwd));
        } finally {
            scramble(key);
        }
    }

    char[] key() {
        // generate the key
        return SecurityUtils.permute(BASE, 32, PERM);
    }

    static OutputStream output(URL url, Resource configDir) throws IOException {
        // check for file url
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            File f = URLs.urlToFile(url);
            if (!f.isAbsolute()) {
                // make relative to config dir
                return configDir.get(f.getPath()).out();
            } else {
                return new FileOutputStream(f);
            }
        } else {
            URLConnection cx = url.openConnection();
            cx.setDoOutput(true);
            return cx.getOutputStream();
        }
    }

    static InputStream input(URL url, Resource configDir) throws IOException {
        // check for a file url
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            File f = URLs.urlToFile(url);
            // check if the file is relative
            if (!f.isAbsolute()) {
                // make it relative to the config directory for this password provider
                Resource res = configDir.get(f.getPath());
                if (res.getType() != Type.RESOURCE) { // file must already exist.
                    throw new FileNotFoundException();
                }
                return res.in();
            } else {
                return new FileInputStream(f);
            }
        } else {
            return url.openStream();
        }
    }

    public static class URLMasterPasswordProviderValidator extends SecurityConfigValidator {

        public URLMasterPasswordProviderValidator(GeoServerSecurityManager securityManager) {
            super(securityManager);
        }

        @Override
        public void validate(MasterPasswordProviderConfig config) throws SecurityConfigException {
            super.validate(config);

            URLMasterPasswordProviderConfig urlConfig = (URLMasterPasswordProviderConfig) config;
            URL url = urlConfig.getURL();

            if (url == null) {
                throw new URLMasterPasswordProviderException(URL_REQUIRED);
            }

            if (config.isReadOnly()) {
                // read-only, assure we can read from url
                try {
                    InputStream in = input(url,
                            manager.masterPasswordProvider().get(config.getName()));
                    try {
                        in.read();
                    } finally {
                        in.close();
                    }
                } catch (IOException ex) {
                    throw new URLMasterPasswordProviderException(URL_LOCATION_NOT_READABLE, url);
                }
            }
        }
    }

    public static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public void configure(XStreamPersister xp) {
            super.configure(xp);
            xp.getXStream().alias("urlProvider", URLMasterPasswordProviderConfig.class);
        }

        @Override
        public Class<? extends MasterPasswordProvider> getMasterPasswordProviderClass() {
            return URLMasterPasswordProvider.class;
        }

        @Override
        public MasterPasswordProvider createMasterPasswordProvider(
                MasterPasswordProviderConfig config) throws IOException {
            return new URLMasterPasswordProvider();
        }

        @Override
        public SecurityConfigValidator createConfigurationValidator(
                GeoServerSecurityManager securityManager) {
            return new URLMasterPasswordProviderValidator(securityManager);
        }
    }
}
