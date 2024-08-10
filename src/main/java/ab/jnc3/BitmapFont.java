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
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class BitmapFont {
  public byte[] bitmap;
  public int[] bitmapCache = new int[0];
  public final short[][] unicode = new short[0x100][];
  public char[] unicodeCache;
  public int length;
  public int byteSize;
  public int height;
  public int width;

  public BitmapFont(int width, int height) {
    length = 0x100;
    byteSize = (width + 7) / 8 * height;
    this.height = height;
    this.width = width;
    this.bitmap = new byte[length * byteSize];
    for (int i = 0x20; i < 0x7F; i++) put((char) i, i); // default ascii
  }

  public BitmapFont() {
    this(8, 0);
  }

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
    unicodeCache = null;
  }

  public int get(char c) {
    testValidCharacter(c);
    int h = c >> 8 & 0xFF;
    if (unicode[h] == null) return -1;
    return unicode[h][c & 0xFF];
  }

  private void fromPsf1Unicode(char[] chars) {
    int i = 0;
    for (char c : chars) if (c == '\uFFFF') i++; else put(c, i);
    unicodeCache = chars;
  }

  public static BitmapFont fromPsf1(byte[] b) {
    assert b.length >= 4 && b[0] == 0x36 && b[1] == 0x04 : "PSF1 header";
    // font
    BitmapFont font = new BitmapFont(8, b[3]);
    boolean isUnicode = (b[2] & 6) != 0;
    font.length = (b[2] & 1) == 0 ? 0x100 : 0x200;
    // bitmap
    int end = 4 + font.length * font.byteSize;
    assert b.length >= end + (isUnicode ? font.length * 2 : 0): "PSF1 file size";
    font.bitmap = Arrays.copyOfRange(b, 4, end);
    // unicode
    CharBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(b, end, b.length))
        .order(ByteOrder.LITTLE_ENDIAN).asCharBuffer();
    if (isUnicode) {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < font.length;) {
        char c = buffer.get();
        s.append(c);
        if (c == '\uFFFE') throw new UnsupportedCharsetException("FIXME series of characters");
        if (c == '\uFFFF') i++;
      }
      font.fromPsf1Unicode(s.toString().toCharArray());
    } else throw new UnsupportedCharsetException("default charset not defined");
    assert !buffer.hasRemaining() : "PSF1 file size";
    font.cacheBitmap();
    return font;
  }

  public static BitmapFont fromPsf2(byte[] b) {
    int[] header = new int[8];
    ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(header);
    assert header[0] == 0x864AB572 && header[1] == 0 && header[2] == 0x20 : "PSF2 header";
    // font
    BitmapFont font = new BitmapFont(header[7], header[6]);
    boolean isUnicode = (header[3] & 1) == 1;
    font.length = header[4];
    font.byteSize = header[5];
    font.height = header[6];
    font.width = header[7];
    assert font.byteSize == (font.width + 7) / 8 * font.height : "PSF2 height width";
    // bitmap
    int end = 0x20 + font.length * font.byteSize;
    assert b.length >= end + (isUnicode ? font.length : 0) : "PSF2 file size";
    font.bitmap = Arrays.copyOfRange(b, 0x20, end);
    // unicode
    if (isUnicode) {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < font.length; i++) {
        int s0 = end;
        for (byte c = b[end++]; c != -1; c = b[end++]) {
          if (c == (byte) 0xFE) throw new UnsupportedCharsetException("FIXME series of characters");
        }
        s.append(new String(Arrays.copyOfRange(b, s0, end - 1), StandardCharsets.UTF_8)).append('\uFFFF');
      }
      font.fromPsf1Unicode(s.toString().toCharArray());
    }
    assert end == b.length : "PSF2 file size";
    font.cacheBitmap();
    return font;
  }

  private char[] toPsf1Unicode() {
    char[][] u = new char[length][16];
    int[] us = new int[length];
    int ts = length;
    for (int h = 0; h < 0x100; h++) {
      short[] uh = unicode[h];
      if (uh == null) continue;
      for (int l = 0; l < 0x100; l++) {
        short uhl = uh[l];
        if (uhl < 0) continue;
        u[uhl][us[uhl]++] = (char) (h << 8 | l);
        ts++;
      }
    }
    char[] chars = new char[ts];
    for (int h = 0, i = 0; h < length; h++) {
      char[] uh = u[h];
      for (int l = 0; l < us[h]; l++) chars[i++] = uh[l];
      chars[i++] = '\uFFFF';
    }
    return chars;
  }

  public byte[] toPsf() {
    if (width != 8 || (length != 0x100 && length != 0x200) || height != byteSize) return toPsf2();
    char[] chars = unicodeCache == null ? toPsf1Unicode() : unicodeCache;
    ByteBuffer buffer = ByteBuffer.allocate(chars.length * 2).order(ByteOrder.LITTLE_ENDIAN);
    for (char c : chars) buffer.putChar(c);
    byte[] bytes = new byte[4 + bitmap.length + chars.length * 2];
    bytes[0] = 0x36;
    bytes[1] = 0x04;
    bytes[2] = (byte) (length == 0x200 ? 3 : 2);
    bytes[3] = (byte) height;
    System.arraycopy(bitmap, 0, bytes, 4, bitmap.length);
    byte[] bufferArray = buffer.array();
    System.arraycopy(bufferArray, 0, bytes, 4 + bitmap.length, bufferArray.length);
    return bytes;
  }

  public byte[] toPsf2() {
    char[] chars = unicodeCache == null ? toPsf1Unicode() : unicodeCache;
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      for (char c : chars) stream.write(c == '\uFFFF' ? new byte[]{-1}
          : new String(new char[]{c}).getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    byte[] unicode = stream.toByteArray();
    ByteBuffer header = ByteBuffer.allocate(0x20).order(ByteOrder.LITTLE_ENDIAN);
    header.putInt(0x864AB572);
    header.putInt(0);
    header.putInt(0x20);
    header.putInt(1);
    header.putInt(length);
    header.putInt(byteSize);
    header.putInt(height);
    header.putInt(width);

    byte[] bytes = new byte[0x20 + bitmap.length + unicode.length];
    System.arraycopy(header.array(), 0, bytes, 0, 0x20);
    System.arraycopy(bitmap, 0, bytes, 0x20, bitmap.length);
    System.arraycopy(unicode, 0, bytes, 0x20 + bitmap.length, unicode.length);
    return bytes;
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
  public void drawString(String s, int x, int y, BufferedImage image, int rgb, int... bgColor) {
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
      if (i >= 0) drawChar(i, x, y, image, rgb, bgColor);
    }
  }

  private void drawChar(int i, int x, int y, BufferedImage image, int rgb, int... bgColor) {
    boolean bg = bgColor.length > 0;
    int bgc = bg ? bgColor[0] : 0;
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
        if ((b & 0x80) != 0 && xx >= 0 && xx < w) image.setRGB(xx, y, rgb); else if (bg) image.setRGB(xx, y, bgc);
      }
    }
  }

  /**
   * Draws chars without clipping, width limit 32 pixels,
   * doesn't support BufferedImage.TYPE_BYTE_BINARY with more than 1 pixel per byte.
   */
  public void drawStringSimple(String s, int x, int y, BufferedImage image, int color, int... bgColor) {
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
      if (i >= 0) drawCharSimple(i, xy, ww, buffer, color, bgColor);
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

  public void multiply(int mw, int mh) {
    int byteSize = (width * mw + 7) / 8;
    byte[] bitmap = new byte[length * height * mh * byteSize];
    for (int c = 0, bmi = 0, bmo = 0; c < length; c++) {
      for (int y = 0; y < height; y++) {
        byte[] bity = new byte[byteSize];
        byte b = 0;
        for (int x = 0, mx = 0; x < width; x++) {
          int x0 = x & 7;
          if (x0 == 0) b = this.bitmap[bmi++];
          boolean on = (b & (1 << 7 - x0)) != 0;
          for (int i = 0; i < mw; i++, mx++) bity[mx >> 3] |= (on ? 1 : 0) << 7 - (mx & 7);
        }
        for (int i = 0; i < mh; i++) {
          System.arraycopy(bity, 0, bitmap, bmo, byteSize);
          bmo += byteSize;
        }
      }
    }
    this.bitmap = bitmap;
    width *= mw;
    height *= mh;
    this.byteSize = byteSize * height;
    cacheBitmap();
  }

}
