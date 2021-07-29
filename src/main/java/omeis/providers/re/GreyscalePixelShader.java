package omeis.providers.re;

import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.quantum.QuantizationException;
import omeis.providers.re.quantum.QuantumStrategy;

public class GreyscalePixelShader implements PixelShader{
    
    QuantumStrategy qs;
    CodomainChain cc;
    int alpha;
    
    public GreyscalePixelShader(QuantumStrategy qs,
            CodomainChain cc,
            int alpha) {
        this.qs = qs;
        this.cc = cc;
        this.alpha = alpha;
    }

    @Override
    public int shadePixel(double pixelValue, int startingValue) throws QuantizationException {
        int discreteValue = qs.quantize(pixelValue);
        if (cc.hasMapContext()) {
            discreteValue = cc.transform(discreteValue);
        }
        return alpha << 24 | discreteValue << 16
                | discreteValue << 8 | discreteValue;
    }

}
