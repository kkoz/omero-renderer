package omeis.providers.re;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ome.util.PixelData;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.quantum.QuantizationException;

public class MultiWorkerTask implements Callable{
    
    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(MultiWorkerTask.class);
    
    PixelShaderFactory shaderFactory;
    String shaderType;
    int x1Start;
    int x1End;
    int x2Start;
    int x2End;
    RGBBuffer dataBuffer;
    List<Plane2D> wData;
    
    public MultiWorkerTask(
            PixelShaderFactory shaderFactory,
            String shaderType,
            int x1Start,
            int x1End,
            int x2Start,
            int x2End,
            RGBBuffer dataBuffer,
            List<Plane2D> wData) {
        this.shaderFactory = shaderFactory;
        this.shaderType = shaderType;
        this.x1Start = x1Start;
        this.x1End = x1End;
        this.x2Start = x2Start;
        this.x2End = x2End;
        this.dataBuffer = dataBuffer;
        this.wData = wData;
    }
            

    @Override
    public Object call() throws Exception {
        int i = 0;
        int[] buf = ((RGBIntBuffer) dataBuffer).getDataBuffer();

        for (Plane2D plane : wData) {
            fillBufferFromPlane(plane, x1Start, x1End, x2Start, x2End, i, buf);
            i++;
        }
        return null;
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
