/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.utils;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

/**
 * Utility to work with compressed files
 * 
 * Based on Importer {@link VFSWorker} by Gabriel Roldan
 * 
 * @author groldan
 * @autho Alessio Fabiani, GeoSolutions
 */
public class BackupUtils {

    private static final Logger LOGGER = Logging.getLogger(BackupUtils.class);
    
    public static Resource tmpDir() throws IOException {
        Resource root = Resources.fromPath(System.getProperty("java.io.tmpdir", "."));
        Resource directory = Resources.createRandom("tmp", "", root);
        return Files.asResource(directory.dir());
    }
    
    /**
     * Extracts the archive file {@code archiveFile} to {@code targetFolder}; both shall previously
     * exist.
     */
    public static void extractTo(Resource archiveFile, Resource targetFolder) throws IOException {
        FileSystemManager manager = VFS.getManager();
        String sourceURI = resolveArchiveURI(archiveFile);
        // String targetURI = resolveArchiveURI(targetFolder);
        FileObject source = manager.resolveFile(sourceURI);
        if (manager.canCreateFileSystem(source)) {
            source = manager.createFileSystem(source);
        }
        FileObject target = manager.createVirtualFileSystem(manager.resolveFile(targetFolder
                .dir().getAbsolutePath()));

        FileSelector selector = new AllFileSelector() {
            @Override
            public boolean includeFile(FileSelectInfo fileInfo) {
                LOGGER.fine("Uncompressing " + fileInfo.getFile().getName().getFriendlyURI());
                return true;
            }
        };
        target.copyFrom(source, selector);
        source.close();
        target.close();
        manager.closeFileSystem(source.getFileSystem());
    }
    
    /**
     * 
     * @param archiveFile
     * @return
     */
    public static String resolveArchiveURI(final Resource archiveFile) {
        String archivePrefix = getArchiveURLProtocol(archiveFile);
        String absolutePath = archivePrefix + archiveFile.file().getAbsolutePath();
        return absolutePath;
    }

    /**
     * 
     * @param file
     * @return
     */
    public static String getArchiveURLProtocol(final Resource file) {
        if (file.getType() == Type.DIRECTORY) {
            return "file://";
        }
        String name = file.name().toLowerCase();
        if (name.endsWith(".zip") || name.endsWith(".kmz")) {
            return "zip://";
        }
        if (name.endsWith(".tar")) {
            return "tar://";
        }
        if (name.endsWith(".tgz") || name.endsWith(".tar.gz")) {
            return "tgz://";
        }
        if (name.endsWith(".tbz2") || name.endsWith(".tar.bzip2") || name.endsWith(".tar.bz2")) {
            return "tbz2://";
        }
        if (name.endsWith(".gz")) {
            return "gz://";
        }
        if (name.endsWith(".bz2")) {
            return "bz2://";
        }
        if (name.endsWith(".jar")) {
            return "jar://";
        }
        return null;
    }
}
