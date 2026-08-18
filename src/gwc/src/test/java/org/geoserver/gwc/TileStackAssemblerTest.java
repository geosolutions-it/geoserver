/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
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
import org.geoserver.platform.ServiceException;
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
        GWC.Segment bottom = segment(solidTile(Color.RED));
        GWC.Segment top = segment(solidTile(Color.BLUE));

        BufferedImage result = decode(assembler.assemble(null, null, List.of(bottom, top), png, -1));

        assertEquals(Color.BLUE.getRGB(), result.getRGB(0, 0));
    }

    @Test
    public void testAssembleAppliesAlphaOverCompositing() throws Exception {
        GWC.Segment bottom = segment(solidTile(Color.RED));
        GWC.Segment transparentTop = segment(solidTile(new Color(0, 0, 255, 0)));

        BufferedImage result = decode(assembler.assemble(null, null, List.of(bottom, transparentTop), png, -1));

        assertEquals(Color.RED.getRGB(), result.getRGB(0, 0));
    }

    @Test
    public void testAssembleFailsWhenDeadlineHasPassed() throws Exception {
        GWC.Segment segment = segment(solidTile(Color.RED));
        long deadlineAlreadyPassed = System.currentTimeMillis() - 1000;

        assertThrows(
                ServiceException.class,
                () -> assembler.assemble(null, null, List.of(segment), png, deadlineAlreadyPassed));
    }

    @Test
    public void testAssembleFailsWhenASegmentSizeDoesNotMatchTheCanvas() throws Exception {
        GWC.Segment bottom = segment(solidTile(Color.RED, 2, 2));
        GWC.Segment mismatched = segment(solidTile(Color.BLUE, 3, 3));

        assertThrows(
                IllegalStateException.class,
                () -> assembler.assemble(null, null, List.of(bottom, mismatched), png, -1));
    }

    private byte[] solidTile(Color color) throws Exception {
        return solidTile(color, 2, 2);
    }

    private byte[] solidTile(Color color, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private BufferedImage decode(byte[] encoded) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(encoded));
    }

    /** A cached segment whose {@code TileLayer.getTile} just stamps the given PNG bytes as the tile's blob. */
    private GWC.CachedSegment segment(byte[] pngBytes) throws Exception {
        ConveyorTile tile = new ConveyorTile(null, "member", "TEST", new long[] {0, 0, 0}, png, Map.of(), null, null);
        TileLayer tileLayer = mock(TileLayer.class);
        doAnswer(invocation -> {
                    ConveyorTile t = invocation.getArgument(0);
                    t.setBlob(new ByteArrayResource(pngBytes));
                    return t;
                })
                .when(tileLayer)
                .getTile(any(ConveyorTile.class));
        return new GWC.CachedSegment(new GWC.TileLayerMember(tileLayer, tile));
    }
}
