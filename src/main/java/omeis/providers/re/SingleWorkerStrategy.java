package omeis.providers.re;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ome.util.PixelData;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.quantum.QuantizationException;

public class SingleWorkerStrategy {
    
    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(SingleWorkerStrategy.class);
    
    PixelShaderFactory shaderFactory;
    String shaderType;
    boolean isXYPlanar;
    int sizeX1;
    int sizeX2;
    int[] buf;
    Plane2D plane;
    List<Plane2D> wData;
    
    public SingleWorkerStrategy(PixelShaderFactory shaderFactory,
            String shaderType,
            int sizeX1,
            int sizeX2,
            int[] buf,
            Plane2D plane,
            List<Plane2D> wData) {
        this.shaderFactory = shaderFactory;
        this.shaderType = shaderType;
        this.sizeX1 = sizeX1;
        this.sizeX2 = sizeX2;
        this.buf = buf;
        this.plane = plane;
        this.wData = wData;
    }
    
    public void workOld() throws QuantizationException {
        int planeSize = sizeX1 * sizeX2;
        PixelShader shader = shaderFactory.getShader(shaderType, 0);
        for (int i = 0; i < planeSize; i++)
        {
            buf[i] = shader.shadePixel(plane.getPixelValue(i), buf[i]);
        }
    }
    
    public void work() throws QuantizationException {
        int i = 0;
        //int[] buf = ((RGBIntBuffer) dataBuffer).getDataBuffer();

        for (Plane2D plane : wData) {
            fillBufferFromPlane(plane, 0, sizeX1, 0, sizeX2, i, buf);
            i++;
        }
    };
    
    public void fillBufferFromPlane(Plane2D plane,
            int x1Start, int x1End, int x2Start, int x2End,
            int i, int[] buf) throws QuantizationException {
        log.info("fillBufferFromPlane");
        boolean isXYPlanar = plane.isXYPlanar();
        PixelData data = plane.getData();
        int bytesPerPixel = data.bytesPerPixel();

        PixelShader shader = shaderFactory.getShader(shaderType, i);
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
