package omeis.providers.re;

import omeis.providers.re.data.Plane2D;
import omeis.providers.re.quantum.QuantizationException;

public class SingleWorkerStrategy {
    
    PixelShader shader;
    boolean isXYPlanar;
    int sizeX1;
    int sizeX2;
    int[] buf;
    Plane2D plane;
    
    public SingleWorkerStrategy(PixelShader shader,
            int sizeX1,
            int sizeX2,
            int[] buf,
            Plane2D plane) {
        this.shader = shader;
        this.sizeX1 = sizeX1;
        this.sizeX2 = sizeX2;
        this.buf = buf;
        this.plane = plane;
    }
    
    public void work() throws QuantizationException {
        int planeSize = sizeX1 * sizeX2;
        for (int i = 0; i < planeSize; i++)
        {
            buf[i] = shader.shadePixel(plane.getPixelValue(i), buf[i]);
        }
    }
}
