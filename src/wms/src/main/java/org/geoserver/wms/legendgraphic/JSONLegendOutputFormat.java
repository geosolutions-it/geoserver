/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Response;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.springframework.util.Assert;

/**
 * OWS {@link Response} that encodes a {@link BufferedImageLegendGraphic} to the image/jpeg MIME
 * Type
 * 
 * @author Carlo Cancellieri - GeoSolutions
 * @version $Id$
 */
public class JSONLegendOutputFormat extends Response implements GetLegendGraphicOutputFormat{



    /**
     * The MIME type of the format this response produces, supported formats see {@link JSONType}
     */
    private final JSONType type;

    protected final WMS wms;

    public JSONLegendOutputFormat(final WMS wms, final String format) {
        super(LegendGraphicModel.class,format);
        this.wms = wms;
        this.type = JSONType.getJSONType(format);
        if (type == null)
            throw new IllegalArgumentException("Not supported mime type for:" + format);
    }

    /**
     * Evaluates if this DescribeLayer producer can generate the format specified by
     * <code>format</code>, where <code>format</code> is the MIME type of the requested
     * response.
     * 
     * @param format
     *            the MIME type of the required output format, might be {@code null}
     * 
     * @return true if class can produce a DescribeLayer in the passed format
     */
    public boolean canProduce(String format) {
        return type.equals(JSONType.getJSONType(format));
    }

    public String getContentType() {
        return type.getMimeType();
    }

    @Override
    public String getMimeType(Object value, org.geoserver.platform.Operation operation)
            throws ServiceException {
        Assert.isInstanceOf(LegendGraphicModel.class, value);
        return type.getMimeType();
    }

    @Override
    public void write(Object value, OutputStream output, org.geoserver.platform.Operation operation)
            throws IOException, ServiceException {
        
        Assert.notNull(operation.getParameters());
        Assert.isTrue(operation.getParameters()[0] instanceof GetLegendGraphicRequest);
        
        final GetLegendGraphicRequest request = (GetLegendGraphicRequest) operation.getParameters()[0];

        Assert.isTrue(value instanceof LegendGraphicModel);
        final LegendGraphicModel model = (LegendGraphicModel) value;
        try {
            write(model, request, output);
        } finally {
            if (output != null) {
                try {
                    output.flush();
                } catch (IOException ioe) {
                }
                IOUtils.closeQuietly(output);
            }
        }
    }

    public void write(LegendGraphicModel model, GetLegendGraphicRequest output, OutputStream operation) throws IOException,
    ServiceException{
        OutputStreamWriter outWriter=null;
        try {
            outWriter = new OutputStreamWriter(operation, wms.getGeoServer().getSettings()
                    .getCharset());
            JSONLegendGraphicBuilder.buildLegendGraphic(outWriter, model);
        } finally {

            if (outWriter != null) {
                outWriter.flush();
                IOUtils.closeQuietly(outWriter);
            }
        }
    }

    @Override
    public Object produceLegendGraphic(GetLegendGraphicRequest request) throws ServiceException {
        return new LegendGraphicModel(request);
    }
 
 
}
