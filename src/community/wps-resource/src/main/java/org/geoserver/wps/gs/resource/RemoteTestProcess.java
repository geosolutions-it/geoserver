/**
 * 
 */
package org.geoserver.wps.gs.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * @author alessio.fabiani
 *
 */
@SuppressWarnings("deprecation")
@DescribeProcess(title = "Resource Loader Process", description = "Downloads Layer Stream and provides a ZIP.")
public class RemoteTestProcess implements GSProcess {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(ResourceLoaderProcess.class);

    @DescribeResult(name = "result", description = "XML describing the Resources to load")
    public String execute(final ProgressListener progressListener) throws ProcessException {
    	InputStream xmlResourceIS = RemoteTestProcess.class.getResourceAsStream("test.xml");
    	String xml;
		try {
			xml = IOUtils.toString(xmlResourceIS);
		} catch (IOException e) {
			throw new ProcessException(e);
		}
    	return xml;
    }
}