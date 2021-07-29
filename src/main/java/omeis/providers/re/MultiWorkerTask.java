package omeis.providers.re;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ome.util.PixelData;
import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.lut.LutReader;
import omeis.providers.re.quantum.QuantizationException;
import omeis.providers.re.quantum.QuantumStrategy;

public class MultiWorkerTask implements Callable{
    
    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(MultiWorkerTask.class);
    
    int x1Start;
    int x1End;
    int x2Start;
    int x2End;
    RGBBuffer dataBuffer;
    Optimizations optimizations;
    List<Plane2D> wData;
    List<int[]> colors;
    List<LutReader> readers;
    List<CodomainChain> chains;
    List<QuantumStrategy> strategies;
    
    public MultiWorkerTask(int x1Start,
    int x1End,
    int x2Start,
    int x2End,
    RGBBuffer dataBuffer,
    Optimizations optimizations,
    List<Plane2D> wData,
    List<int[]> colors,
    List<LutReader> readers,
    List<CodomainChain> chains,
    List<QuantumStrategy> strategies) {
        this.x1Start = x1Start;
        this.x1End = x1End;
        this.x2Start = x2Start;
        this.x2End = x2End;
        this.dataBuffer = dataBuffer;
        this.optimizations = optimizations;
        this.wData = wData;
        this.colors = colors;
        this.readers = readers;
        this.chains = chains;
        this.strategies = strategies;
    }
            

    @Override
    public Object call() throws Exception {
        int i = 0;
        int[] buf = ((RGBIntBuffer) dataBuffer).getDataBuffer();
        boolean isPrimaryColor = optimizations.isPrimaryColorEnabled();
        boolean isAlphaless = optimizations.isAlphalessRendering();

        for (Plane2D plane : wData) {
            fillBufferFromPlane(plane, x1Start, x1End, x2Start, x2End, i, isPrimaryColor, buf, isAlphaless);
            i++;
        }
        return null;
    };
    
    public void fillBufferFromPlane(Plane2D plane,
            int x1Start, int x1End, int x2Start, int x2End,
            int i, boolean isPrimaryColor, int[] buf, boolean isAlphaless) throws QuantizationException {
        log.info("fillBufferFromPlane");
        int[] color = colors.get(i);
        LutReader reader = readers.get(i);
        CodomainChain cc = chains.get(i);
        QuantumStrategy qs = strategies.get(i);
        boolean isXYPlanar = plane.isXYPlanar();
        PixelData data = plane.getData();
        int bytesPerPixel = data.bytesPerPixel();

        float alpha = new Integer(
                color[ColorsFactory.ALPHA_INDEX]).floatValue() / 255;
        HSBPixelShader shader = new HSBPixelShader(qs, cc, reader, color, optimizations);
        for (int x2 = x2Start; x2 < x2End; ++x2) {
            for (int x1 = x1Start; x1 < x1End; ++x1) {
                int width = x1End - x1Start;
                int pix = width * x2 + x1;
                double pixelValue;
                if (isXYPlanar)
                    pixelValue = data.getPixelValueDirect(pix * bytesPerPixel);
                else
                    pixelValue = plane.getPixelValue(x1, x2);
                buf[pix] = shader.shadePixel(pixelValue, buf[pix]);
            }
        }
    }

}
