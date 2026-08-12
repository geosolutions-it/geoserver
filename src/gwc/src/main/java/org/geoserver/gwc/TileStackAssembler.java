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
     * Fetches every member's tile, rendering on a cache miss exactly like a single-layer request, decodes and draws
     * each one onto a shared canvas in {@code LAYERS} order, then encodes the result.
     *
     * @return the assembled tile, encoded as {@code outputFormat}
     */
    byte[] assemble(List<GWC.TileLayerMember> members, MimeType outputFormat) throws Exception {
        BufferedImage canvas = null;
        Graphics2D graphics = null;
        try {
            for (GWC.TileLayerMember member : members) {
                ConveyorTile tile = member.tile();
                member.tileLayer().getTile(tile);

                String mimeType = tile.getMimeType().getMimeType();
                BufferedImage memberImage = decoders.decode(
                        mimeType, tile.getBlob(), decoders.isAggressiveInputStreamSupported(mimeType), null);

                if (canvas == null) {
                    canvas = new BufferedImage(
                            memberImage.getWidth(), memberImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    graphics = canvas.createGraphics();
                }
                graphics.drawImage(memberImage, 0, 0, null);
                // flush now rather than leaving it to GC: a group can hold many decoded members at once
                memberImage.flush();
            }
        } finally {
            if (graphics != null) {
                graphics.dispose();
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encoders.encode(canvas, outputFormat, out, false, null);
        return out.toByteArray();
    }
}
