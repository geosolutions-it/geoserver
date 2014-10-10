/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.data.DataUtilities;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class TranslateItemUtils {

    /**
     * @param context
     * @param store
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    public static File getFileFromUrl(TranslateContext context, Map<String, String> store)
            throws MalformedURLException, IOException {

        final GeoServerDataDirectory dd = new GeoServerDataDirectory(context.getCatalog()
                .getResourceLoader());

        File file = null;

        final String urlTxt = store.get("url");

        // trying to parse a local file
        if (urlTxt.startsWith("file:")) {
            if (!urlTxt.startsWith("file:/")) {
                final String location = urlTxt.substring(urlTxt.indexOf(":") + 1);
                file = dd.findFile(location);

                if (file == null) {
                    dd.findDataFile(location);
                }
            } else {
                file = DataUtilities.urlToFile(new URL(urlTxt));
            }
        }

        // trying to download a resource from an HTTP Stream (or FTP simple mode)
        /**
         * The FTP url must be something like 
         *   <url>ftp://tom:secret@www.myserver.com/project/2014/Project.zip;type=i</url>
         */
        else if (urlTxt.startsWith("http:") || urlTxt.startsWith("ftp:")) {
            final File parent = dd.findOrCreateDataDir("wps-resource_" + System.nanoTime());

            FileOutputStream fos = null;
            try {
                final URL remoteFile = new URL(urlTxt);
                ReadableByteChannel rbc = Channels.newChannel(remoteFile.openStream());
                file = new File(parent, FilenameUtils.getBaseName(urlTxt));
                fos = new FileOutputStream(file);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } finally {
                IOUtils.closeQuietly(fos);
            }
        }

        // trying to download a resource from an FTP Stream
        /**
         * ADVANCED FTP TRANSFER USING apache.commons-net @TODO
         * 
         * http://commons.apache.org/proper/commons-net/
         * 
         * else if (urlTxt.startsWith("ftp:")) {
         *
         *   final FTPClient ftp = new FTPClient();
         *   String host;
         *   String port;
         *   String username;
         *   String password;
         *     
         * }
         */

        return file;
    }

}
