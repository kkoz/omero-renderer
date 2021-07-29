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
            buf[i] = shader.shadePixel(plane.getPixelValue(i));
        }
    }
    
    /*
    public RGBIntBuffer renderAsPackedInt(Renderer renderer, PlaneDef planeDef) {
     // Set the context and retrieve objects we're gonna use.
        //renderer = ctx;
        // Initialize sizeX1 and sizeX2 according to the plane definition and
        // create the RGB buffer.
        Pixels metadata = renderer.getMetadata();
        initAxesSize(planeDef, metadata);
        if (!findFirstActiveChannelBinding())
        {
            return getIntBuffer();
        }
        PixelBuffer pixels = renderer.getPixels();
        RenderingStats performanceStats = renderer.getStats();
        QuantumStrategy qs = 
            renderer.getQuantumManager().getStrategyFor(channel);
        CodomainChain cc = renderer.getCodomainChain(channel);
        
        // Retrieve the planar data to render
        
        Plane2D plane;
        try {
            performanceStats.startIO(channel);
            plane = PlaneFactory.createPlane(planeDef, channel, metadata, pixels);
            performanceStats.endIO(channel);
        } finally
        {
            try
            {
                pixels.close();
            } 
            catch (IOException e)
            {
                log.error("Pixels could not be closed successfully.", e);
                throw new ResourceError(
                        e.getMessage() + " Please check server log.");
            }
        }
       
        RGBIntBuffer dataBuf = getIntBuffer();
        
        int alpha = channelBinding.getAlpha();
        int[] buf = ((RGBIntBuffer) dataBuf).getDataBuffer();
        int x1, x2, discreteValue, pixelIndex;
        boolean hasMapContext = cc.hasMapContext();
        if (plane.isXYPlanar())
        {
            int planeSize = sizeX1 * sizeX2;
            for (int i = 0; i < planeSize; i++)
            {
                discreteValue = qs.quantize(plane.getPixelValue(i));
                if (hasMapContext) {
                    discreteValue = cc.transform(discreteValue);
                }
                buf[i] = alpha << 24 | discreteValue << 16
                        | discreteValue << 8 | discreteValue;
            }
        }
        else
        {
            for (x2 = 0; x2 < sizeX2; ++x2) {
                pixelIndex = sizeX1 * x2;
                for (x1 = 0; x1 < sizeX1; ++x1) {
                    discreteValue = qs.quantize(plane.getPixelValue(x1, x2));
                    if (hasMapContext) {
                        discreteValue = cc.transform(discreteValue);
                    }
                    buf[pixelIndex + x1] = alpha << 24 | discreteValue << 16
                    | discreteValue << 8 | discreteValue;
                }
            }
        }
        return dataBuf;
    }
    */
    

}
