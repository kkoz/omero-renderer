package omeis.providers.re;

import omeis.providers.re.quantum.QuantizationException;

public interface PixelShader {
    
    public abstract int shadePixel(double pixelValue, int startingValue) throws QuantizationException;

}
