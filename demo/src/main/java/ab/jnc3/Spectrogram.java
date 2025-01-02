/*
 * Copyright (C) 2024 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import java.util.Arrays;

public class Spectrogram {

  public static final AudioFormat AUDIO_CD_MONO = new AudioFormat(44100, 16, 1, true, false);
  public static final double BUFFER_MILLISECONDS = 50;
  public static final FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);
  public static final int FFT_LENGTH = 2048;
  public static final int FFT_HEIGHT = 160;
  public static final double[] LOG = new double[FFT_HEIGHT];

  int mode;

  static {
    for (int i = 0, d = -FFT_HEIGHT; i < FFT_HEIGHT; i++) {
      LOG[i] = Math.exp(++d / 30.0) * (FFT_LENGTH / 2.0 - 0.01);
    }
  }

  public static double[] logarithmic(double[] f) {
    int n = LOG.length;
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

  public static void drawDoubles(BufferedImage image, double[] doubles, double mul,
      int x, int xi, int y, int yi, int w, int color) {
    int di = Math.max(1, doubles.length / w);
    for (int i0 = 0; i0 < w; i0++) {
      int i = i0 * doubles.length / w;
      double sum = Math.abs(doubles[i]);
      for (int i1 = 1; i1 < di; i1++) sum += Math.abs(doubles[++i]);
      sum = Math.min(sum * mul / di, 1);
      int c = (int) ((color & 0xFF0000) * sum) & 0xFF0000
          | (int) ((color & 0x00FF00) * sum) & 0x00FF00
          | (int) ((color & 0x0000FF) * sum) & 0x0000FF;
      image.setRGB(x, y, c);
      x += xi;
      y += yi;
    }

  }

  public static double[] fftTransform(double[] audio) {
    int n = audio.length;
    int n2 = n / 2;
    Complex[] fftComplex = FFT.transform(audio, TransformType.FORWARD);
    double[] fft = new double[n];
    for (int i = 0; i < n2; i++) {
      Complex complex = fftComplex[i];
      fft[i] = complex.abs();
      fft[n2 + i] = complex.getArgument() / (2 * Math.PI);
    }
    return fft;
  }

  public static void main(String[] args) throws LineUnavailableException {
    Screen screen = new Screen();
    Spectrogram sp = new Spectrogram();
    screen.keyListener = key -> {
      if (key.equals("Tab")) sp.mode = (sp.mode + 1) % 6;
      if (key.equals("Esc")) System.exit(0);
    };
    BufferedImage image = screen.image;
    Graphics graphics = image.getGraphics();
    int h = image.getHeight();
    int w = image.getWidth();
    int w1 = w - 1;

    AudioFormat format = AUDIO_CD_MONO;
    final int frameSize = format.getFrameSize();
    int fftSize = FFT_LENGTH;
    byte[] bytes = new byte[0x10000];

    TargetDataLine line = AudioSystem.getTargetDataLine(format);
    line.open(format, (int) (format.getFrameRate() * BUFFER_MILLISECONDS / 1000) * frameSize);
    line.start();

    while (line.read(bytes, 0, fftSize * frameSize) == fftSize * frameSize) {
      double[] audio = new double[fftSize];
      for (int i = 0, j = 0; i < fftSize; i++) {
        final int l = bytes[j++] & 0xFF;
        audio[i] = (bytes[j++] << 8 | l) / 32768.0;
      }
      double[] fft = fftTransform(audio);
      if (sp.mode == 0) fft = logarithmic(fft);
      graphics.drawImage(image, -1, 0, null);
      if (sp.mode == 0) {
        drawDoubles(image, fft, 0.05, w1, 0, FFT_HEIGHT - 1, -1, FFT_HEIGHT, 0xFF8080);
        drawDoubles(image, Arrays.copyOf(audio, h - FFT_HEIGHT), 1.0, w1, 0, FFT_HEIGHT, 1, h - FFT_HEIGHT, 0xFF8080);
      } else {
        drawDoubles(image, Arrays.copyOf(fft, fft.length / 2), 0.05, w1, 0, 127, -1, 128, 0xFFFF80);
        drawDoubles(image, Arrays.copyOf(audio, h - 128), 1.0, w1, 0, 128, 1, h - 128, 0xFFFF80);
      }
      screen.update();
      switch (sp.mode) {
        case 0: fftSize = FFT_LENGTH; break;
        case 1: fftSize = 128; break;
        case 2: fftSize = 512; break;
        case 3: fftSize = 2048; break;
        case 4: fftSize = 4096; break;
        case 5: fftSize = 8192; break;
      }
    }
  }
}
