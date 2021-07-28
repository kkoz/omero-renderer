package omeis.providers.re;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.lut.LutReader;
import omeis.providers.re.quantum.BinaryMaskQuantizer;
import omeis.providers.re.quantum.QuantizationException;
import omeis.providers.re.quantum.QuantumStrategy;

public class HSBPixelShader {

    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(HSBPixelShader.class);

    QuantumStrategy qs;
    CodomainChain cc;
    float alpha;
    LutReader reader;
    int[] color;
    Optimizations optimizations;

    public HSBPixelShader(QuantumStrategy qs,
            CodomainChain cc,
            float alpha,
            LutReader reader,
            int[] color,
            Optimizations optimizations) {
        this.qs = qs;
        this.cc = cc;
        this.alpha = alpha;
        this.reader = reader;
        this.color = color;
        this.optimizations = optimizations;
    }

    public int shadePixel(double pixelValue, int startingValue) throws QuantizationException {
        int discreteValue = qs.quantize(pixelValue);
        if (cc.hasMapContext()) {
            discreteValue = cc.transform(discreteValue);
        }
        if (reader != null) {
            return shadePixel(discreteValue, startingValue,
                    reader.getRed(discreteValue),
                    reader.getGreen(discreteValue),
                    reader.getBlue(discreteValue));
        }
        int colorOffset = 24;
        if (optimizations.isPrimaryColorEnabled() && reader == null)
            colorOffset = getColorOffset(color);
        // Primary colour optimization is in effect, we don't need
        // to do any of the sillyness below just shift the value
        // into the correct colour component slot and move on to
        // the next pixel value.
        if (colorOffset != 24)
        {
            return shadePixel(discreteValue, startingValue, colorOffset);
        }
        double redRatio = color[ColorsFactory.RED_INDEX] > 0 ?
                color[ColorsFactory.RED_INDEX] / 255.0 : 0.0;
        double greenRatio = color[ColorsFactory.GREEN_INDEX] > 0 ?
                 color[ColorsFactory.GREEN_INDEX] / 255.0 : 0.0;
        double blueRatio = color[ColorsFactory.BLUE_INDEX] > 0 ?
                 color[ColorsFactory.BLUE_INDEX] / 255.0 : 0.0;
        boolean isMask = qs instanceof BinaryMaskQuantizer? true : false;
        return shadePixel(discreteValue, startingValue,
                redRatio, greenRatio, blueRatio, alpha, isMask);
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

    public int shadePixel(int discreteValue, int startingValue, int lutr, int lutg, int lutb) {
            int r1 = ((startingValue & 0x00FF0000) >> 16);
            int r2 = lutr & 0xFF;
            int g1 = ((startingValue & 0x0000FF00) >> 8);
            int g2 = lutg & 0xFF;
            int b1 = (startingValue & 0x000000FF);
            int b2 = lutb & 0xFF;
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
            return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public int shadePixel(int discreteValue, int startingValue, int colorOffset) {

        // Primary colour optimization is in effect, we don't need
        // to do any of the sillyness below just shift the value
        // into the correct colour component slot and move on to
        // the next pixel value.
        startingValue |= 0xFF000000;  // Alpha.
        startingValue |= discreteValue << colorOffset;
        return startingValue;
    }

    public int shadePixel(int discreteValue, int startingValue,
            double redRatio,
            double greenRatio,
            double blueRatio,
            float alpha,
            boolean isMask) {
        int newRValue = (int) (redRatio * discreteValue);
        int newGValue = (int) (greenRatio * discreteValue);
        int newBValue = (int) (blueRatio * discreteValue);

        // Pre-multiply the alpha for each colour component if the
        // image has a non-1.0 alpha component.
        if (!optimizations.isAlphalessRendering())
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
            startingValue = 0xFF000000 | newRValue << 16
                       | newGValue << 8 | newBValue;
            return startingValue;
        }
        // Add the existing colour component values to the new
        // colour component values.
        int rValue = ((startingValue & 0x00FF0000) >> 16) + newRValue;
        int gValue = ((startingValue & 0x0000FF00) >> 8) + newGValue;
        int bValue = (startingValue & 0x000000FF) + newBValue;

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
        return 0xFF000000 | rValue << 16 | gValue << 8 | bValue;
    }
}
