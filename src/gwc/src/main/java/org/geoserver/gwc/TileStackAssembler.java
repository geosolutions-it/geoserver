/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageEncoderContainer;
import org.geowebcache.mime.MimeType;

/**
 * Assembles one coalesced tile by fetching each {@code LAYERS} member's own tile and stacking them alpha-over in
 * request order. Single-use, single-threaded: build one instance per tile request. Modeled on
 * {@code org.geowebcache.service.wms.WMSTileFuser}, dropping the spatial resampling it needs and this doesn't: every
 * member shares the same grid location and zoom.
 */
class TileStackAssembler {

    private final ImageDecoderContainer decoders;

    private final ImageEncoderContainer encoders;

    TileStackAssembler(ImageDecoderContainer decoders, ImageEncoderContainer encoders) {
        this.decoders = decoders;
        this.encoders = encoders;
    }

    /**
     * Fetches every cached segment's tile, rendering on a cache miss exactly like a single-layer request, and
     * live-renders every non-cacheable run as a single sub-request; draws each segment's image onto a shared canvas in
     * {@code LAYERS} order, then encodes the result.
     *
     * @param deadline wall-clock time (as per {@link System#currentTimeMillis()}) by which encoding must start, or
     *     {@code <= 0} for no deadline; matches the WMS {@code maxRenderingTime} contract, which no single member's own
     *     render can enforce on its own since it only sees its own elapsed time, not this whole operation's
     * @return the assembled tile, encoded as {@code outputFormat}
     * @throws ServiceException if {@code deadline} has passed before encoding could start
     */
    byte[] assemble(GWC gwc, GetMapRequest request, List<GWC.Segment> segments, MimeType outputFormat, long deadline)
            throws Exception {
        BufferedImage canvas = null;
        Graphics2D graphics = null;
        try {
            for (GWC.Segment segment : segments) {
                BufferedImage segmentImage = segment instanceof GWC.CachedSegment cached
                        ? decodeCachedSegment(cached)
                        : gwc.renderLiveSegment(request, (GWC.LiveSegment) segment, deadline);

                if (canvas == null) {
                    canvas = new BufferedImage(
                            segmentImage.getWidth(), segmentImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    graphics = canvas.createGraphics();
                } else if (segmentImage.getWidth() != canvas.getWidth()
                        || segmentImage.getHeight() != canvas.getHeight()) {
                    // members share one footprint, gridloc and zoom (enforced in GWC.classifyCoalescedMembers); a
                    // live segment's own render is the one segment kind that isn't grid-checked, so a mismatch here
                    // would otherwise silently misalign the stack instead of failing loudly
                    throw new IllegalStateException(
                            "Coalesced segment image size " + segmentImage.getWidth() + "x" + segmentImage.getHeight()
                                    + " does not match the tile size " + canvas.getWidth() + "x" + canvas.getHeight());
                }
                graphics.drawImage(segmentImage, 0, 0, null);
                // drops cached/accelerated surface copies now rather than waiting for GC; does not free the raster
                // itself, so it's no substitute for letting segmentImage go out of scope after each segment
                segmentImage.flush();
            }
        } finally {
            if (graphics != null) {
                graphics.dispose();
            }
        }

        if (deadline > 0 && System.currentTimeMillis() > deadline) {
            throw new ServiceException("This request used more time than allowed and has been forcefully stopped.");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encoders.encode(canvas, outputFormat, out, false, null);
        return out.toByteArray();
    }

    private BufferedImage decodeCachedSegment(GWC.CachedSegment cached) throws Exception {
        ConveyorTile tile = cached.member().tile();
        cached.member().tileLayer().getTile(tile);
        String mimeType = tile.getMimeType().getMimeType();
        return decoders.decode(mimeType, tile.getBlob(), decoders.isAggressiveInputStreamSupported(mimeType), null);
    }
}
