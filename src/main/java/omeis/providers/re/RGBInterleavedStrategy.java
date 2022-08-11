package omeis.providers.re;

import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loci.formats.FormatException;
import ome.io.bioformats.BfPixelBufferPlus;
import ome.io.nio.PixelBuffer;
import ome.model.core.Pixels;
import ome.model.display.ChannelBinding;
import ome.model.display.QuantumDef;
import ome.model.enums.PixelsType;
import ome.util.PixelData;
import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.data.PlaneDef;
import omeis.providers.re.data.RegionDef;
import omeis.providers.re.lut.LutReader;
import omeis.providers.re.quantum.BinaryMaskQuantizer;
import omeis.providers.re.quantum.QuantizationException;
import omeis.providers.re.quantum.QuantumStrategy;

public class RGBInterleavedStrategy extends RenderingStrategy {

    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(RGBInterleavedStrategy.class);

    @Override
    RGBBuffer render(Renderer ctx, PlaneDef planeDef)
            throws IOException, QuantizationException {
        // Set the context and retrieve objects we're gonna use.
        renderer = ctx;
        //RenderingStats performanceStats = renderer.getStats();
        Pixels metadata = renderer.getMetadata();

        // Initialize sizeX1 and sizeX2 according to the plane definition and
        // create the RGB buffer.
        initAxesSize(planeDef, metadata);
        RGBBuffer buf = getRgbBuffer();
        render(buf, planeDef);
        return buf;
    }

    @Override
    RGBIntBuffer renderAsPackedInt(Renderer ctx, PlaneDef planeDef)
            throws IOException, QuantizationException {
        // Set the context and retrieve objects we're gonna use.
        renderer = ctx;
        Pixels metadata = renderer.getMetadata();

        // Initialize sizeX1 and sizeX2 according to the plane definition and
        // create the RGB buffer.
        initAxesSize(planeDef, metadata);
        RGBIntBuffer buf = getIntBuffer();
        render(buf, planeDef);
        return buf;
    }

    /**
     * Retrieves the quantum strategy for each active channels
     *
     * @return the active channel color data.
     */
    private List<QuantumStrategy> getStrategies() {
        ChannelBinding[] channelBindings = renderer.getChannelBindings();
        QuantumManager qManager = renderer.getQuantumManager();
        List<QuantumStrategy> strats = new ArrayList<QuantumStrategy>();

        for (int w = 0; w < channelBindings.length; w++) {
            if (channelBindings[w].getActive()) {
                strats.add(qManager.getStrategyFor(w));
            }
        }
        Map<byte[], Integer> overlays = renderer.getOverlays();
        if (overlays != null)
        {
            QuantumDef def = new QuantumDef();  // Just to fulfill interface
            Pixels pixels = new Pixels();
            PixelsType bitType = new PixelsType();
            bitType.setValue(PixelsType.VALUE_BIT);
            bitType.setBitSize(1);
            pixels.setPixelsType(bitType);
            for (int i = 0; i < overlays.size(); i++)
            {
                strats.add(new BinaryMaskQuantizer(def, pixels));
            }
        }
        return strats;
    }

    /**
     * Retrieves the color for each active channels.
     *
     * @return the active channel color data.
     */
    private List<int[]> getColors() {
        ChannelBinding[] channelBindings = renderer.getChannelBindings();
        List<int[]> colors = new ArrayList<int[]>();

        for (int w = 0; w < channelBindings.length; w++) {
            ChannelBinding cb = channelBindings[w];
            if (cb.getActive()) {
                int[] theNewColor = new int[] {
                        cb.getRed(), cb.getGreen(),
                        cb.getBlue(), cb.getAlpha() };
                colors.add(theNewColor);
            }
        }
        Map<byte[], Integer> overlays = renderer.getOverlays();
        if (overlays != null)
        {
            for (byte[] overlay : overlays.keySet())
            {
                Integer packedColor = overlays.get(overlay);
                Color color = new Color(packedColor);
                colors.add(new int[] { color.getRed(), color.getBlue(),
                                       color.getGreen(), color.getAlpha() });
            }
        }
        return colors;
    }

    /**
     * Returns the collection of chains.
     *
     * @return See above.
     */
    private List<CodomainChain> getChains()
    {
        List<CodomainChain> chains = renderer.getCodomainChains();
        ChannelBinding[] channelBindings = renderer.getChannelBindings();
        List<CodomainChain> list = new ArrayList<CodomainChain>();
        for (int w = 0; w < channelBindings.length; w++) {
            ChannelBinding cb = channelBindings[w];
            if (cb.getActive()) {
                list.add(chains.get(w));
            }
        }
        return list;
    }

    private void render(RGBBuffer buf, PlaneDef planeDef) throws IOException,
    QuantizationException {
        RenderingStats performanceStats = renderer.getStats();

        BfPixelBufferPlus pixelBuffer = (BfPixelBufferPlus) renderer.getPixels();

        int discreteValue;
        double redRatio, greenRatio, blueRatio;
        int rValue, gValue, bValue;
        int newRValue, newGValue, newBValue;
        int colorOffset = 24;  // Only used when we're doing primary color.

        int[] intBuf = ((RGBIntBuffer) buf).getDataBuffer();
        boolean isPrimaryColor = renderer.getOptimizations().isPrimaryColorEnabled();
        boolean isAlphaless = renderer.getOptimizations().isAlphalessRendering();
        List<int[]> colors = getColors();
        List<LutReader> lutReaders = renderer.getLutProvider().getLutReaders(
                renderer.getChannelBindings());
        List<QuantumStrategy> strategies = getStrategies();
        List<CodomainChain> chains = getChains();
        LutReader lutReader;
        CodomainChain cc;
        RegionDef region = planeDef.getRegion();
        try {
            byte[] imageData = pixelBuffer.getInterleavedTile(
                    region.getX(),
                    region.getY(),
                    region.getWidth(),
                    region.getHeight());

            ByteBuffer bbuf = ByteBuffer.wrap(imageData);
            PixelData pixelData = new PixelData(renderer.getPixelsType().getValue(), bbuf);
            int bytesPerPixel = pixelData.bytesPerPixel();

            /*
            for (int i = 0; i < imageData.length; i += 3) {
                QuantumStrategy rQs = strategies.get(0);
                rValue = rQs.quantize(pixelData.getPixelValueDirect(i));
                QuantumStrategy gQs = strategies.get(1);
                gValue = gQs.quantize(pixelData.getPixelValueDirect(i+1));
                QuantumStrategy bQs = strategies.get(0);
                bValue = bQs.quantize(pixelData.getPixelValueDirect(i+2));
                intBuf[i/3] = 0xFF000000 | rValue << 16 | gValue << 8 | bValue;
            }
            */
            for (int i = 0; i < imageData.length/3; i++) {
                for (int j = 0; j < 3; j++) {
                    int[] color = colors.get(j);
                    lutReader = lutReaders.get(j);
                    cc = chains.get(j);
                    boolean hasMap = cc.hasMapContext();
                    QuantumStrategy qs = strategies.get(j);
                    boolean isMask = qs instanceof BinaryMaskQuantizer? true : false;
                    redRatio = color[ColorsFactory.RED_INDEX] > 0 ?
                            color[ColorsFactory.RED_INDEX] / 255.0 : 0.0;
                    greenRatio = color[ColorsFactory.GREEN_INDEX] > 0 ?
                             color[ColorsFactory.GREEN_INDEX] / 255.0 : 0.0;
                    blueRatio = color[ColorsFactory.BLUE_INDEX] > 0 ?
                             color[ColorsFactory.BLUE_INDEX] / 255.0 : 0.0;
                     boolean isXYPlanar = planeDef.getSlice() == PlaneDef.XY;
                  // Get our color offset if we've got the primary color optimization
                     // enabled.
                     if (isPrimaryColor && lutReader == null)
                         colorOffset = getColorOffset(color);
                     float alpha = new Integer(
                             color[ColorsFactory.ALPHA_INDEX]).floatValue() / 255;


                     //if (isXYPlanar)
                     discreteValue =
                     qs.quantize(
                             pixelData.getPixelValueDirect(((i*3) + j) * bytesPerPixel));
                    /*
                     else
                         discreteValue =
                             qs.quantize(plane.getPixelValue(x1, x2));
                             */
                     if (hasMap) {
                         discreteValue = cc.transform(discreteValue);
                     }
                     if (lutReader != null) {
                         int r1 = ((intBuf[i] & 0x00FF0000) >> 16);
                         int r2 = lutReader.getRed(discreteValue) & 0xFF;
                         int g1 = ((intBuf[i] & 0x0000FF00) >> 8);
                         int g2 = lutReader.getGreen(discreteValue) & 0xFF;
                         int b1 = (intBuf[i] & 0x000000FF);
                         int b2 = lutReader.getBlue(discreteValue) & 0xFF;
                         int r = r1+r2;
                         if (r > 255) {
                             r = 255;
                         }
                         int g = g1+g2;
                         if (g > 255) {
                             g = 255;
                         }
                         int b = b1+b2;
                         if (b > 255) {
                             b = 255;
                         }
                         intBuf[i] = 0xFF000000 | r << 16 | g << 8 | b;
                         continue;
                     }
                     // Primary colour optimization is in effect, we don't need
                     // to do any of the sillyness below just shift the value
                     // into the correct colour component slot and move on to
                     // the next pixel value.
                     if (colorOffset != 24)
                     {
                         intBuf[i] |= 0xFF000000;  // Alpha.
                         intBuf[i] |= discreteValue << colorOffset;
                         continue;
                     }
                     newRValue = (int) (redRatio * discreteValue);
                     newGValue = (int) (greenRatio * discreteValue);
                     newBValue = (int) (blueRatio * discreteValue);

                     // Pre-multiply the alpha for each colour component if the
                     // image has a non-1.0 alpha component.
                     if (!isAlphaless)
                     {
                         newRValue *= alpha;
                         newGValue *= alpha;
                         newBValue *= alpha;
                     }

                     if (isMask && discreteValue == 255) {
                         // Since the mask is a hard value, we do not want to
                         // compromise on colour fidelity. Packed each colour
                         // component along with a 1.0 alpha into the buffer so
                         // that buffered images that use this buffer can be
                         // type 1 (3 bands, pre-multiplied alpha) or type 2
                         // (4 bands, alpha component included).
                         intBuf[i] = 0xFF000000 | newRValue << 16
                                    | newGValue << 8 | newBValue;
                         continue;
                     }
                     // Add the existing colour component values to the new
                     // colour component values.
                     rValue = ((intBuf[i] & 0x00FF0000) >> 16) + newRValue;
                     gValue = ((intBuf[i] & 0x0000FF00) >> 8) + newGValue;
                     bValue = (intBuf[i] & 0x000000FF) + newBValue;

                     // Ensure that each colour component value is between 0 and
                     // 255 (byte). We must make *certain* that values do not
                     // wrap over 255 otherwise there will be corruption
                     // introduced into the rendered image. The value may be over
                     // 255 if we have mapped two high intensity channels to
                     // the same color.
                     if (rValue > 255) {
                         rValue = 255;
                     }
                     if (gValue > 255) {
                         gValue = 255;
                     }
                     if (bValue > 255) {
                         bValue = 255;
                     }

                     // Packed each colour component along with a 1.0 alpha into
                     // the buffer so that buffered images that use this buffer
                     // can be type 1 (3 bands, pre-multiplied alpha) or type 2
                     // (4 bands, alpha component included).
                     intBuf[i] = 0xFF000000 | rValue << 16 | gValue << 8 | bValue;
                }
            }
        } catch (FormatException e) {
            log.error("Error getting image data", e);
            return;
        }

        // End the performance metrics for this rendering event.
        performanceStats.endRendering();
    }

    /**
     * Returns a color offset based on which color component is 0xFF.
     * @param color the color to check.
     * @return an integer color offset in bits.
     */
    private int getColorOffset(int[] color)
    {
        if (color[ColorsFactory.RED_INDEX] == 255)
            return 16;
        if (color[ColorsFactory.GREEN_INDEX] == 255)
            return 8;
        if (color[ColorsFactory.BLUE_INDEX] == 255)
            return 0;
        throw new IllegalArgumentException(
                "Unable to find color component offset in color.");
    }

    private RenderingTask[] makeRenderingTasks(PlaneDef planeDef,
            RGBBuffer buf) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    RGBAIntBuffer renderAsPackedIntAsRGBA(Renderer ctx, PlaneDef pd)
            throws IOException, QuantizationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    int getImageSize(PlaneDef pd, Pixels pixels) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    String getPlaneDimsAsString(PlaneDef pd, Pixels pixels) {
        // TODO Auto-generated method stub
        return null;
    }

}
