/*
 * Copyright (C) 2021 Aleksei Balan
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

package ab.jnc2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
      () -> new TextFont("/jnc2/48.rom", 0x3D00, 0x0300, 0x20, 8, 8).charset(StandardCharsets.ISO_8859_1);
  public static final Supplier<TextFont> PICO8 =
      () -> new TextFont("/jnc2/pico-8.fnt", 0, 0x0400, 0, 8, 8).width(4).height(6);
  public static final Supplier<TextFont> VGA8 = () -> new TextFont("/jnc2/vga.fnt", 0, 0x0800, 0, 8, 8);
  public static final Supplier<TextFont> VGA14 = () -> new TextFont("/jnc2/vga.fnt", 0x0800, 0x0E00, 0, 8, 14);
  public static final Supplier<TextFont> VGA16 = () -> new TextFont("/jnc2/vga.fnt", 0x172D, 0x1000, 0, 8, 16);
  public static final Supplier<TextFont> XTRACKER0 = () -> new TextFont("/jnc2/XTRACKER.FNT", 0x0000, 0x0D00, 0, 8, 13).width(9);
  public static final Supplier<TextFont> XTRACKER1 = () -> new TextFont("/jnc2/XTRACKER.FNT", 0x0D00, 0x0D00, 0, 8, 13).width(9);
  public static final Supplier<TextFont> XTRACKER2 = () -> new TextFont("/jnc2/XTRACKER.FNT", 0x1A00, 0x0D00, 0, 8, 13).width(9);
  public static final Supplier<TextFont> XTRACKER3 = () -> new TextFont("/jnc2/XTRACKER.FNT", 0x2700, 0x0D00, 0, 8, 13).width(9);
  public static final Supplier<TextFont> XTRACKER4 = () -> new TextFont("/jnc2/XTRACKER.FNT", 0x3400, 0x0D00, 0, 8, 13).width(9);

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
    this.charset = Charset.forName("IBM437"); // DifferentCharsets.IBM437;
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

  public TextFont charset(Charset charset) {
    this.charset = charset;
    return this;
  }

  /**
   * Because FontMetrics.getStringBounds lies.
   * @return baseline y offset or -1 if overshot
   */
  public static BufferedImage debug = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
  public static int debugx = 0;
  public static int debugy = 0;

  private static void drawChar(Graphics graphics, Dimension size, Point point, char c) {
    graphics.clearRect(0, 0, size.width, size.height);
    graphics.drawString(String.valueOf(c), point.x, point.y);
  }

  private Rectangle getBounds(Font font, BufferedImage image, Point point) {
    int width = image.getWidth();
    int height = image.getHeight();
    List<Integer> pymin = new ArrayList<>(), pymax = new ArrayList<>(), pxmax = new ArrayList<>();
    int axmin = Integer.MAX_VALUE, axmax = 0, aymin = Integer.MAX_VALUE, aymax = 0;
    List<Character> targetChars = Stream.of(
        IntStream.rangeClosed('A', 'Z'),
        IntStream.rangeClosed('a', 'z'),
        IntStream.rangeClosed('0', '9'))
        .flatMapToInt(c -> c).mapToObj(c -> (char) c).filter(font::canDisplay).collect(Collectors.toList());
    targetChars = IntStream.rangeClosed('\u2550', '\u256C')
        .mapToObj(c -> (char) c).filter(font::canDisplay).collect(Collectors.toList());

    Graphics graphics = image.getGraphics();
    Dimension size = new Dimension(image.getWidth(), image.getHeight());
    graphics.setFont(font);
    for (char c : targetChars) {
      drawChar(graphics, size, point, c);
      for (int y = height - 1; y >= 0; y--) {
        for (int x = width - 1; x >= 0; x--) {
          if ((image.getRGB(x, y) & 0xFFFFFF) != 0) {
            axmin = Math.min(axmin, x);
            axmax = Math.max(axmax, x);
            aymin = Math.min(aymin, y);
            aymax = Math.max(aymax, y);
          }
        }
      }
    }
    if (axmax < axmin || aymax < aymin || axmin == 0 || aymin == 0 || axmax >= width - 1 || aymax >= height - 1)
      return null;
    return new Rectangle(axmin, aymin, axmax - axmin + 1, aymax - aymin + 1);
//    int rx = width1 - (axmax - axmin + 1);
//    int ry = height1 - (aymax - aymin + 1);
//    return Math.min(rx, ry) < 0 ? null : new Rectangle(axmin, axmax, axmax - axmin + 1, aymax - aymin + 1);
//    return (axmax - axmin >= width - 1) || (aymax - aymin >= height - 1)
//        ? -1 : height - ymin + (height - ymax + ymin + 1) / 2 - 1;
  }

  public TextFont(String fontName, int width, int height) {
    this(new byte[0], 0, width, height);
    Dimension imageSize = new Dimension(width * 3, height * 3);
    Point point = new Point(width, height);
    BufferedImage image = new BufferedImage(imageSize.width, imageSize.height, BufferedImage.TYPE_BYTE_BINARY);

    int size;
    Font font = new Font(fontName, Font.PLAIN, 1);
    Rectangle baseline = null;
    // increase size by 1
    for (size = 20; size < width * 40; size += 10) {
      font = new Font(fontName, Font.PLAIN, size / 10);
      baseline = getBounds(font, image, point);
      if (baseline == null || baseline.width > width || baseline.height > height) {
        break;
      }
    }
    debugx = 0;
    debugy += height * 2;
    // decrease size by 0.1
    for (; size > 1; size--) {
      font = font.deriveFont(size / 10.0F);
      baseline = getBounds(font, image, point);
      if (baseline != null && baseline.width <= width && baseline.height <= height) {
        break;
      }
    }
    debugx = 0;
    debugy += height * 2;
    // best size found, make font
    Graphics graphics = image.getGraphics();
    graphics.setFont(font);
    for (int i = 0x00; i < 0x100; i++) {
      char c = charset.decode(ByteBuffer.wrap(new byte[]{(byte) i})).get();
      if (font.canDisplay(c)) {
        drawChar(graphics, imageSize, point, c);
        for (int y = 0; y < height; y++) {
          byte b = 0;
          for (int x = 0; x < width; x++) {
            int rgb = image.getRGB(x + baseline.x, y + baseline.y);
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
    if (width == 8 && height == 8) System.arraycopy(ZX.get().font, 0, font, 0, font.length);
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
