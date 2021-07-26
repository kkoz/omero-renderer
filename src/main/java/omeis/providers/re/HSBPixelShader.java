package omeis.providers.re;

import java.util.Optional;

public class HSBPixelShader {

    public HSBPixelShader() {

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
            Optional<Float> alpha,
            boolean isMask) {
        int newRValue = (int) (redRatio * discreteValue);
        int newGValue = (int) (greenRatio * discreteValue);
        int newBValue = (int) (blueRatio * discreteValue);

        // Pre-multiply the alpha for each colour component if the
        // image has a non-1.0 alpha component.
        if (alpha.isPresent())
        {
            newRValue *= alpha.get();
            newGValue *= alpha.get();
            newBValue *= alpha.get();
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
