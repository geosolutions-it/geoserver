/**
 * 
 */
package org.geoserver.wps.gs.resource;

import java.util.logging.Logger;

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
    	return "pippo!";
    }

}
