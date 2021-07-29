package omeis.providers.re;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.minio.errors.InvalidArgumentException;
import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.lut.LutReader;
import omeis.providers.re.quantum.QuantumStrategy;

public class PixelShaderFactory {
    
    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(PixelShaderFactory.class);
    
    public static String HSB_SHADER = "HSBPixelShader";
    public static String GREYSCALE_SHADER = "GreyscaleShader";


    Optimizations optimizations;
    List<int[]> colors;
    List<LutReader> readers;
    List<CodomainChain> chains;
    List<QuantumStrategy> strategies;
    
    public PixelShaderFactory(
                Optimizations optimizations,
                List<int[]> colors,
                List<LutReader> readers,
                List<CodomainChain> chains,
                List<QuantumStrategy> strategies) {
        this.optimizations = optimizations;
        this.colors = colors;
        this.readers = readers;
        this.chains = chains;
        this.strategies = strategies;
    }
    
    public PixelShader getShader(String type, int i) {
        QuantumStrategy qs = strategies.get(i);
        CodomainChain cc = chains.get(i);
        int[] color = colors.get(i);
        LutReader reader = readers.get(i);
        if(type.equals(HSB_SHADER)) {
            return new HSBPixelShader(qs, cc, reader, color, optimizations);
        } else if (type.equals(GREYSCALE_SHADER)) {
            return new GreyscalePixelShader(qs, cc, color[ColorsFactory.ALPHA_INDEX]);
        } else {
            log.error("Invalid shader type specified - defaulting to HSBPixelShader");
            return new HSBPixelShader(qs, cc, reader, color, optimizations);
        }        
    }

}
