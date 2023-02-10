/*
 * Copyright 2021 Aleksei Balan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ab.jnc2;

import ab.font.DifferentCharsets;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Text can load font and print text.
 * For printing it use BufferedImage and fourth quadrant coordinates native to java.
 */
public class TextFont {

  public static final Supplier<TextFont> ZX =
      () -> new TextFont("/48.rom", 0x3D00, 0x0300, 0x20, 8, 8);
  public static final Supplier<TextFont> PICO8 =
      () -> new TextFont("/pico-8.fnt", 0, 0x0400, 0, 8, 8).width(4).height(6);
  public static final Supplier<TextFont> VGA8 = () -> new TextFont("/vga.fnt", 0, 0x0800, 0, 8, 8);
  public static final Supplier<TextFont> VGA14 = () -> new TextFont("/vga.fnt", 0x0800, 0x0E00, 0, 8, 14);
  public static final Supplier<TextFont> VGA16 = () -> new TextFont("/vga.fnt", 0x172D, 0x1000, 0, 8, 16);

  public final byte[] font;

  private final int intw;
  private final int inth;
  public int width;
  public int height;
  public Charset charset; // charset aware font

  public TextFont(byte[] data, int charStart, int width, int height) {
    font = new byte[height * 0x100];
    System.arraycopy(data, 0, font, charStart * height, data.length);
    this.intw = width;
    this.inth = height;
    this.width = width;
    this.height = height;
    this.charset = DifferentCharsets.IBM437; // Charset.forName("IBM437");
  }

  public TextFont(String resource, int byteStart, int byteSize, int charStart, int width, int height) {
    this(new byte[0], charStart, width, height);
    InputStream input = TextFont.class.getResourceAsStream(resource);
    byte[] buffer = new byte[byteSize];
    try {
      if ((input.skip(byteStart) != byteStart) || (input.read(buffer) != byteSize))
        throw new IllegalStateException("buffer error");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    System.arraycopy(buffer, 0, font, charStart * height, byteSize);
  }

  public TextFont width(int width) {
    this.width = width;
    return this;
  }

  public TextFont height(int height) {
    this.height = height;
    return this;
  }

  /**
   * Because FontMetrics.getStringBounds lies.
   * @return baseline y offset or -1 if overshot
   */
  private int getBounds(Font font, int width, int height) {
    List<Integer> pymin = new ArrayList<>(), pymax = new ArrayList<>(), pxmax = new ArrayList<>();
    List<Character> targetChars = Stream.of(
        IntStream.rangeClosed('A', 'Z'),
        IntStream.rangeClosed('a', 'z'),
        IntStream.rangeClosed('0', '9'))
        .flatMapToInt(c -> c).mapToObj(c -> (char) c).filter(font::canDisplay).collect(Collectors.toList());

    BufferedImage image = new BufferedImage(width * 2, height * 2, BufferedImage.TYPE_BYTE_BINARY);
    Graphics graphics = image.getGraphics();
    graphics.setFont(font);
    for (char c : targetChars) {
      graphics.clearRect(0, 0, width * 2, height * 2);
      graphics.drawString(String.valueOf(c), 0, height);
      int ymin = height * 2;
      int ymax = 0;
      int xmin = width * 2;
      int xmax = 0;
      for (int y = height * 2 - 1; y >= 0; y--) {
        for (int x = width * 2 - 1; x >= 0; x--) {
          if ((image.getRGB(x, y) & 0xFFFFFF) != 0) {
            xmin = Math.min(xmin, x);
            xmax = Math.max(xmax, x);
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
          }
        }
      }
      if ((xmax >= xmin) && (ymax >= ymin)) {
        pymin.add(ymin);
        pymax.add(ymax);
        pxmax.add(xmax - xmin);
      }
    }
    pymin.sort(Comparator.comparingInt(Integer::intValue));
    pymax.sort(Comparator.comparingInt(i -> -i));
    pxmax.sort(Comparator.comparingInt(i -> -i));
    int ymin = pymin.get(pymin.size() / 7); // 85 percentile
    int ymax = pymax.get(pymax.size() / 7);
    int xmax = pxmax.get(pxmax.size() / 7);
    return (xmax >= width - 1) || (ymax - ymin >= height - 1)
        ? -1 : height - ymin + (height - ymax + ymin + 1) / 2 - 1;
  }

  public TextFont(String fontName, int width, int height) {
    this(new byte[0], 0, width, height);
    int size;
    Font font = new Font(fontName, Font.PLAIN, 1);
    int baseline = 0;
    // increase size by 1
    for (size = 20; size < width * 40; size += 10) {
      font = new Font(fontName, Font.PLAIN, size / 10);
      baseline = getBounds(font, width, height);
      if (baseline < 0) {
        break;
      }
    }
    // decrease size by 0.1
    for (; size > 1; size--) {
      font = font.deriveFont(size / 10.0F);
      baseline = getBounds(font, width, height);
      if (baseline >= 0) {
        break;
      }
    }
    // best size found, make font
    BufferedImage image = new BufferedImage(width * 2, height, BufferedImage.TYPE_BYTE_BINARY);
    Graphics graphics = image.getGraphics();
    graphics.setFont(font);
    for (int i = 0x00; i < 0x100; i++) {
      char c = charset.decode(ByteBuffer.wrap(new byte[]{(byte) i})).get();
      if (font.canDisplay(c)) {
        graphics.clearRect(0, 0, width * 2, height);
        graphics.drawString(String.valueOf(c), width / 2, baseline);
        int xmin = width * 2;
        int xmax = 0;
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width * 2; x++) {
            if ((image.getRGB(x, y) & 0xFFFFFF) != 0) {
              xmin = Math.min(xmin, x);
              xmax = Math.max(xmax, x);
            }
          }
        }
        int xBaseline = (xmax < xmin ? 0 : (xmax + xmin - width) / 2 + 1);
        for (int y = 0; y < height; y++) {
          byte b = 0;
          for (int x = 0; x < width; x++) {
            int rgb = image.getRGB(x + xBaseline, y);
            b = (byte) (b ^ ((rgb & 0x80) >> x));
          }
          this.font[i * height + y] = b;
        }
      }
    }
  }

  /**
   * Create bitmap font from vector font installed in system.
   */
  public TextFont(int width, int height) {
    this(Font.MONOSPACED, width, height);
  }

  private void print(BufferedImage image, String s, int x, int y, int color, int bgColor, boolean withBackground,
      boolean centered) {
    if (centered) x -= this.width * s.length() / 2;
    int width = image.getWidth();
    int height = image.getHeight();
    byte[] chars = s.getBytes(charset);
    for (int i = 0; i < s.length(); i++) {
      int c = chars[i] & 0xFF;
      c *= this.inth;
      for (int iy = 0; iy < this.inth; iy++) {
        for (int ix = 0; ix < this.intw; ix++) {
          int jx = x + ix;
          int jy = y + iy;
          if (jx < 0 || jy < 0 || jx >= width || jy >= height) continue;
          if ((font[c + iy] >> (7 - ix) & 1) == 1) {
            image.setRGB(jx, jy, color);
          } else if (withBackground) {
            image.setRGB(jx, jy, bgColor);
          }
        }
      }
      x += this.width;
    }
  }

  public void print(BufferedImage image, String s, int x, int y, int color) {
    print(image, s, x, y, color, 0, false, false);
  }

  public void printCentered(BufferedImage image, String s, int x, int y, int color) {
    print(image, s, x, y, color, 0, false, true);
  }

  public void print(BufferedImage image, String s, int x, int y, int color, int bgColor) {
    print(image, s, x, y, color, bgColor, true, false);
  }

  public void preview(BufferedImage image) {
    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 32; x++) {
        print(image, new String(new byte[]{(byte) (y * 32 + x)}, charset), x * intw, y * inth, 0xFFFF00, 0x0000AA);
        int xr = x * intw + intw - 1;
        int yd = y * inth + inth - 1;
        if (xr < image.getWidth() && yd < image.getHeight() && (image.getRGB(xr, yd) & 0xFFFF00) == 0)
        image.setRGB(xr, yd, 0x00AA00);
      }
    }
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.ZX);
    new TextFont(8, 8).preview(screen.image);
    screen.repaint();
  }

}
