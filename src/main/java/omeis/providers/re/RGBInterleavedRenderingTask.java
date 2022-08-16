package omeis.providers.re;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ome.util.PixelData;
import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.lut.LutReader;
import omeis.providers.re.quantum.QuantizationException;
import omeis.providers.re.quantum.QuantumStrategy;

public class RGBInterleavedRenderingTask implements RenderingTask {

    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(RGBInterleavedRenderingTask.class);

    private PixelData pixelData;

    /** How to quantize a pixel intensity value. */
    private List<QuantumStrategy> strategies;

    /**
     * The spatial transformations to apply to the quantized data.
     * One per channel.
     */
    private List<CodomainChain> chains;

    /**
     * The color components used when mapping a quantized value onto the color
     * space.
     */
    private List<int[]> colors;

    /** The <i>X1</i>-axis start */
    private int x1Start;

    /** The <i>X1</i>-axis end */
    private int x1End;

    /** The <i>X2</i>-axis start */
    private int x2Start;

    /** The <i>X2</i>-axis end */
    private int x2End;

    /** The optimizations that the renderer has turned on for us. */
    private Optimizations optimizations;

    /** The collection of readers.*/
    private List<LutReader> readers;

    RGBInterleavedRenderingTask(PixelData pixelData,
            List<QuantumStrategy> strategies, List<CodomainChain> chains,
            List<int[]> colors, Optimizations optimizations,
            int x1Start, int x1End, int x2Start, int x2End,
            List<LutReader> readers) {
        this.pixelData = pixelData;
        this.strategies = strategies;
        this.chains = chains;
        this.colors = colors;
        this.optimizations = optimizations;
        this.x1Start = x1Start;
        this.x1End = x1End;
        this.x2Start = x2Start;
        this.x2End = x2End;
        this.readers = readers;
    }

    @Override
    public Object call() throws QuantizationException {
        // TODO Auto-generated method stub
        return null;
    }

}
