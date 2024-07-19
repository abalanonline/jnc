package ab.jnc3;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Spectrogram {

  public static final AudioFormat AUDIO_CD_MONO = new AudioFormat(44100, 16, 1, true, false);
  public static final double BUFFER_MILLISECONDS = 50;
  public static final FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);
  public static final int FFT_LENGTH = 2048;
  public static final int FFT_HEIGHT = 160;
  public static final double[] LOG = new double[FFT_HEIGHT];

  static {
    for (int i = 0, d = -FFT_HEIGHT; i < FFT_HEIGHT; i++) {
      LOG[i] = Math.exp(++d / 30.0) * (FFT_LENGTH / 2.0 - 0.01);
    }
  }

  public static double[] logarithmic(Complex[] source) {
    int n = source.length / 2;
    int n1 = source.length - 1;
    double[] f = new double[n];
    for (int i = 0; i < n; i++) f[i] = source[i].abs() + source[n1 - i].abs();
    n = LOG.length;
    double[] result = new double[n];
    double pivot = 0;
    for (int i = 0; i < n; i++) {
      double p = LOG[i];
      int i0 = (int) Math.floor(pivot);
      int i1 = (int) Math.floor(p);
      double sum = f[i1] * (p - i1) - f[i0] * (pivot - i0);
      while (i0 < i1) sum += f[i0++];
      result[i] = sum / (p - pivot);
      pivot = p;
    }
    return result;
  }

  public static void main(String[] args) throws LineUnavailableException {
    Screen screen = new Screen();
    screen.keyListener = key -> {
      if (key.equals("Esc")) System.exit(0);
    };
    BufferedImage image = screen.image;
    Graphics graphics = image.getGraphics();
    int h = image.getHeight();
    int w = image.getWidth();
    int w1 = w - 1;

    AudioFormat format = AUDIO_CD_MONO;
    int length = FFT_LENGTH * format.getFrameSize();
    byte[] bytes = new byte[length];
    double[] doubles = new double[FFT_LENGTH];

    TargetDataLine line = AudioSystem.getTargetDataLine(format);
    line.open(format, (int) (format.getFrameRate() * BUFFER_MILLISECONDS / 1000) * format.getFrameSize());
    line.start();

    while (line.read(bytes, 0, length) == length) {
      for (int i = 0, j = 0; i < FFT_LENGTH; i++) {
        final byte l = bytes[j++];
        doubles[i] = (bytes[j++] << 8 | l) / 32768.0;
      }
      Complex[] fft = FFT.transform(doubles, TransformType.FORWARD);
      double[] f = logarithmic(fft);
      graphics.drawImage(image, -1, 0, null);
      for (int i = 0, y = FFT_HEIGHT - 1, x = 0; i < FFT_HEIGHT; i++, y--, x++) {
        int v = (int) Math.min(Math.round(f[x] * 5), 127);
        image.setRGB(w1, y, v * 0x020101);
      }
      for (int i = h - FFT_HEIGHT - 1, y = FFT_HEIGHT, j = 1; i >= 0; i--, y++, j += 2) {
        int v = bytes[j];
        if (v < 0) v = - v - 1;
        image.setRGB(w1, y, v * 0x020101);
      }
      screen.update();
    }
  }
}
