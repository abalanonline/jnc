/*
 * Copyright 2024 Aleksei Balan
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

package ab.jnc3;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class BitmapFont {
  public static final String BROWN_FOX = "The quick brown fox jumps over the lazy dog";
  public byte[] bitmap = new byte[0];
  public int[] bitmapCache = new int[0];
  public final short[][] unicode = new short[0x100][];
  public int length = 0;
  public int byteSize = 0;
  public int height = 0;
  public int width = 8;

  public static void testValidCharacter(char c) {
    if (Character.isSurrogate(c)) throw new UnsupportedCharsetException("FIXME unicode supplementary planes");
  }

  public void put(char c, int i) {
    testValidCharacter(c);
    int h = c >> 8 & 0xFF;
    if (unicode[h] == null) {
      short[] a = new short[0x100];
      Arrays.fill(a, (short) -1);
      unicode[h] = a;
    }
    unicode[h][c & 0xFF] = (short) i;
  }

  public int get(char c) {
    testValidCharacter(c);
    int h = c >> 8 & 0xFF;
    if (unicode[h] == null) return -1;
    return unicode[h][c & 0xFF];
  }

  public static BitmapFont fromPsf1(byte[] b) {
    assert b.length >= 4 && b[0] == 0x36 && b[1] == 0x04 : "PSF1 header";
    // font
    BitmapFont font = new BitmapFont();
    boolean isUnicode = (b[2] & 6) != 0;
    font.length = (b[2] & 1) == 0 ? 0x100 : 0x200;
    font.byteSize = b[3];
    font.height = b[3];
    // bitmap
    int end = 4 + font.length * font.byteSize;
    assert b.length >= end + (isUnicode ? font.length * 2 : 0): "PSF1 file size";
    font.bitmap = Arrays.copyOfRange(b, 4, end);
    // unicode
    CharBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(b, end, b.length))
        .order(ByteOrder.LITTLE_ENDIAN).asCharBuffer();
    if (isUnicode) for (short i = 0; i < font.length; i++) {
      for (char c = buffer.get(); c != '\uFFFF'; c = buffer.get()) {
        if (c == '\uFFFE') throw new UnsupportedCharsetException("FIXME series of characters");
        font.put(c, i);
      }
    } else for (short i = 0; i < 0x100; i++) font.put((char) i, i);
    assert !buffer.hasRemaining() : "PSF1 file size";
    font.cacheBitmap();
    return font;
  }

  public static BitmapFont fromPsf2(byte[] b) {
    int[] header = new int[8];
    ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(header);
    assert header[0] == 0x864AB572 && header[1] == 0 && header[2] == 0x20 : "PSF2 header";
    // font
    BitmapFont font = new BitmapFont();
    boolean isUnicode = (header[3] & 1) == 1;
    font.length = header[4];
    font.byteSize = header[5];
    font.height = header[6];
    font.width = header[7];
    assert font.byteSize == font.height * ((font.width + 7) >> 3) : "PSF2 height width";
    // bitmap
    int end = 0x20 + font.length * font.byteSize;
    assert b.length >= end + (isUnicode ? font.length : 0) : "PSF2 file size";
    font.bitmap = Arrays.copyOfRange(b, 0x20, end);
    // unicode
    if (isUnicode) for (short i = 0; i < font.length; i++) {
      int s0 = end;
      for (byte c = b[end++]; c != -1; c = b[end++]) {
        if (c == (byte) 0xFE) throw new UnsupportedCharsetException("FIXME series of characters");
      }
      String s = new String(Arrays.copyOfRange(b, s0, end - 1), StandardCharsets.UTF_8);
      for (char c : s.toCharArray()) font.put(c, i);
    } else for (short i = 0; i < 0x100; i++) font.put((char) i, i);
    assert end == b.length : "PSF2 file size";
    font.cacheBitmap();
    return font;
  }

  public void cacheBitmap() {
    int cacheSize = this.length * this.height;
    this.bitmapCache = new int[cacheSize];
    int bytesPerLine = this.byteSize / this.height;
    for (int i = 0; i < cacheSize; i++) {
      for (int x = Math.min(bytesPerLine, 4), j = i * bytesPerLine, s = 24; x > 0; x--, j++, s -= 8) {
        this.bitmapCache[i] |= (this.bitmap[j] & 0xFF) << s;
      }
    }
  }

  public static BitmapFont fromPsf(byte[] b) {
    if (b.length > 2 && b[0] == 0x1F && b[1] == (byte) 0x8B) b = gunzip(b);
    if (b.length > 2 && b[0] == 0x36 && b[1] == 0x04) return fromPsf1(b);
    if (b.length > 4 && b[0] == 0x72 && b[1] == (byte) 0xB5 && b[2] == 0x4A && b[3] == (byte) 0x86)
      return fromPsf2(b);
    b = Arrays.copyOf(b, 4);
    throw new IllegalStateException(String.format("PSF header %02X %02X %02X %02X", b[0], b[1], b[2], b[3]));
  }

  public static byte[] gunzip(byte[] bytes) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
         ByteArrayInputStream input = new ByteArrayInputStream(bytes);
         GZIPInputStream gzip = new GZIPInputStream(input)) {
      gzip.transferTo(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * upper left corner coordinate of (0, 0)
   */
  public void drawString(String s, int x, int y, BufferedImage image, int rgb) {
    int w = image.getWidth();
    int h = image.getHeight();
    char[] chars = s.toCharArray();
    if (x >= w || y >= h || -x >= chars.length * this.width || -y >= this.height) return;
    int xn = x; // next
    for (char c : chars) {
      x = xn;
      xn = x + this.width;
      if (xn <= 0) continue;
      if (x >= w) break;
      int i = get(c);
      if (i >= 0) drawChar(i, x, y, image, rgb);
    }
  }

  private void drawChar(int i, int x, int y, BufferedImage image, int rgb) {
    i *= this.byteSize;
    int yn = Math.min(y + this.height, image.getHeight()); // next
    if (y < 0) {
      i += this.byteSize / this.height * -y;
      y = 0;
    }
    int w = image.getWidth();
    byte b = 0;
    for (; y < yn; y++) {
      for (int xi = 0, xx = x; xi < this.width; xi++, xx++, b <<= 1) {
        if ((xi & 7) == 0) b = this.bitmap[i++];
        if ((b & 0x80) != 0 && xx >= 0 && xx < w) image.setRGB(xx, y, rgb);
      }
    }
  }

  /**
   * Draws chars without clipping, width limit 32 pixels,
   * doesn't support BufferedImage.TYPE_BYTE_BINARY with more than 1 pixel per byte.
   */
  public void drawStringSimple(String s, int x, int y, BufferedImage image, int color) {
    int w = image.getWidth();
    int h = image.getHeight();
    final int ww = w - this.width;
    char[] chars = s.toCharArray();
    if (x > ww || y > h - this.height || -x > (chars.length - 1) * this.width || y < 0) return;
    DataBuffer buffer = image.getRaster().getDataBuffer();
    int xn = x; // next
    int xy = x + y * w;
    for (char c : chars) {
      x = xn;
      xn += this.width;
      if (x < 0) continue;
      if (xn > w) break;
      int i = get(c);
      if (i >= 0) drawCharSimple(i, xy, ww, buffer, color);
      xy += this.width;
    }
  }

  /**
   * No clipping, fully visible.
   * @param xy = x + y * w
   * @param ww = w - this.width
   */
  public void drawCharSimple(int i, int xy, int ww, DataBuffer buffer, int color, int... bgColor) {
    boolean bg = bgColor.length > 0;
    int bgc = bg ? bgColor[0] : 0;
    i *= this.height;
    for (int y = 0; y < this.height; y++, xy += ww) {
      int b = this.bitmapCache[i++];
      for (int x = 0; x < this.width; x++, xy++, b <<= 1) {
        if (b < 0) buffer.setElem(xy, color); else if (bg) buffer.setElem(xy, bgc);
      }
    }
  }

  public static void testFont(Screen screen, BitmapFont font) {
    int w = screen.image.getWidth();
    int h = screen.image.getHeight();
    DataBuffer buffer = screen.image.getRaster().getDataBuffer();
    for (int i = buffer.getSize() - 1; i >= 0; i--) buffer.setElem(i, 0);
    for (int i = 0; i < font.length; i++) {
      int cx = i % 32 * font.width;
      int cy = (i / 32 + 1) * font.height;
      if (cx + font.width > w || cy + font.height > h) continue;
      //font.drawChar(i, cx, cy, screen.image, 0xFFCCCCCC);
      font.drawCharSimple(i, cx + cy * w, w - font.width, buffer, 0xFFCCCCCC, 0xFF333333);
    }
  }

  public static void main(String[] args) throws IOException {
    Screen screen = new Screen();
    List<Path> paths = Files.find(Path.of("/usr/share/kbd/consolefonts/"), 3, (path, attributes) -> {
      String s = path.getFileName().toString();
      if (s.equals("README.psfu")) return false;
      if (s.endsWith(".gz")) s = s.substring(0, s.length() - 3);
      return s.endsWith(".psf") || s.endsWith(".psfu");
    }).collect(Collectors.toList());
    final AtomicInteger iFont = new AtomicInteger();
    final AtomicInteger xText = new AtomicInteger();
    final AtomicInteger yText = new AtomicInteger();
    final AtomicLong ts0 = new AtomicLong();
    final AtomicLong tf0 = new AtomicLong();
    AtomicReference<BitmapFont> font = new AtomicReference<>();
    font.set(BitmapFont.fromPsf(Files.readAllBytes(paths.get(0))));
    Runnable update = () -> {
      testFont(screen, font.get());
      //font.get().drawStringSimple(BROWN_FOX, xText.get() / 3, yText.get() / 3, screen.image, 0xFF8000);
      BitmapFont font1 = font.get();
      BufferedImage image1 = screen.image;
      int iter1 = 100;
      int iter2 = 2000;
      for (int j = 0; j < iter1; j++) font1.drawStringSimple(BROWN_FOX, 0, 100, image1, 1);
      long timeSimple = System.nanoTime();
      for (int j = 0; j < iter2; j++) font1.drawStringSimple(BROWN_FOX, 0, 100, image1, 1);
      timeSimple -= System.nanoTime();
      for (int j = 0; j < iter1; j++) font1.drawString(BROWN_FOX, 0, 100, image1, 1);
      long timeFull = System.nanoTime();
      for (int j = 0; j < iter2; j++) font1.drawString(BROWN_FOX, 0, 100, image1, 1);
      timeFull -= System.nanoTime();
      ts0.addAndGet(timeSimple);
      tf0.addAndGet(timeFull);
      font1.drawStringSimple(
          String.format("%d / %d = %.3f", -timeSimple / 1_000_000, -timeFull / 1_000_000,
              (double) timeSimple / timeFull), 160, 0, image1, 0x00FF00);
      font1.drawStringSimple(
          String.format("%d / %d = %.3f", -ts0.get() / 1_000_000, -tf0.get() / 1_000_000,
              (double) ts0.get() / tf0.get()), 0, 0, image1, 0xFFFF00);
      screen.update();
    };
    screen.eventSupplier.addMouseMotionListener(new MouseMotionListener() {
      int x;
      int y;
      @Override
      public void mouseMoved(MouseEvent e) {
        x = e.getX();
        y = e.getY();
      }
      @Override
      public void mouseDragged(MouseEvent e) {
        xText.addAndGet(e.getX() - x);
        yText.addAndGet(e.getY() - y);
        x = e.getX();
        y = e.getY();
        update.run();
      }
    });
    screen.keyListener = k -> {
      switch (k) {
        case "PageUp": if (iFont.decrementAndGet() < 0) iFont.incrementAndGet(); break;
        case "PageDown": if (iFont.incrementAndGet() >= paths.size()) iFont.decrementAndGet(); break;
        case "Esc": System.exit(0); break;
      }
      try {
        font.set(BitmapFont.fromPsf(Files.readAllBytes(paths.get(iFont.get()))));
        update.run();
      } catch (IOException ignore) {}
    };
    for (int i = 0; i < paths.size(); i++) {
      font.set(BitmapFont.fromPsf(Files.readAllBytes(paths.get(i))));
      update.run();
    }
    //update.run();
  }
}
