/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.codec.ImageDecoder;
import org.geowebcache.io.codec.ImageDecoderContainer;
import org.geowebcache.io.codec.ImageDecoderImpl;
import org.geowebcache.io.codec.ImageEncoder;
import org.geowebcache.io.codec.ImageEncoderContainer;
import org.geowebcache.io.codec.ImageEncoderImpl;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/** Unit test suite for {@link TileStackAssembler}. */
public class TileStackAssemblerTest {

    private MimeType png;

    private TileStackAssembler assembler;

    @Before
    public void setUp() throws Exception {
        png = MimeType.createFromFormat("image/png");

        ImageDecoderContainer decoders = new ImageDecoderContainer();
        ApplicationContext decoderContext = mock(ApplicationContext.class);
        when(decoderContext.getBeansOfType(ImageDecoder.class))
                .thenReturn(Map.of("d", new ImageDecoderImpl(false, List.of("image/png"))));
        decoders.setApplicationContext(decoderContext);

        ImageEncoderContainer encoders = new ImageEncoderContainer();
        ApplicationContext encoderContext = mock(ApplicationContext.class);
        when(encoderContext.getBeansOfType(ImageEncoder.class))
                .thenReturn(Map.of("e", new ImageEncoderImpl(false, List.of("image/png"), Map.of())));
        encoders.setApplicationContext(encoderContext);

        assembler = new TileStackAssembler(decoders, encoders);
    }

    @Test
    public void testAssembleStacksMembersInLayersOrder() throws Exception {
        GWC.TileLayerMember bottom = member(solidTile(Color.RED));
        GWC.TileLayerMember top = member(solidTile(Color.BLUE));

        BufferedImage result = decode(assembler.assemble(List.of(bottom, top), png));

        assertEquals(Color.BLUE.getRGB(), result.getRGB(0, 0));
    }

    @Test
    public void testAssembleAppliesAlphaOverCompositing() throws Exception {
        GWC.TileLayerMember bottom = member(solidTile(Color.RED));
        GWC.TileLayerMember transparentTop = member(solidTile(new Color(0, 0, 255, 0)));

        BufferedImage result = decode(assembler.assemble(List.of(bottom, transparentTop), png));

        assertEquals(Color.RED.getRGB(), result.getRGB(0, 0));
    }

    private byte[] solidTile(Color color) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 2, 2);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private BufferedImage decode(byte[] encoded) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(encoded));
    }

    /** A member whose {@code TileLayer.getTile} just stamps the given PNG bytes as the tile's blob. */
    private GWC.TileLayerMember member(byte[] pngBytes) throws Exception {
        ConveyorTile tile = new ConveyorTile(null, "member", "TEST", new long[] {0, 0, 0}, png, Map.of(), null, null);
        TileLayer tileLayer = mock(TileLayer.class);
        doAnswer(invocation -> {
                    ConveyorTile t = invocation.getArgument(0);
                    t.setBlob(new ByteArrayResource(pngBytes));
                    return t;
                })
                .when(tileLayer)
                .getTile(any(ConveyorTile.class));
        return new GWC.TileLayerMember(tileLayer, tile);
    }
}
