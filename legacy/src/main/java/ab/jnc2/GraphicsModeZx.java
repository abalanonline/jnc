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
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ZX graphic mode.
 * No flash: brightness bit 6 == paper bit 3 == ink bit 3, flash bit 7 == 0
 * Flash: brightness bit 6 == paper bit 3 != ink bit 3, flash bit 7 == paper bit 3 ^ ink bit 3
 */
public class GraphicsModeZx {

  public static final int WIDTH = 256;
  public static final int HEIGHT = 192;
  public static final int AW = 32;
  public static final int AH = 24;

  public static final int BLACK = 8;
  public static final int BLUE = 9;
  public static final int RED = 10;
  public static final int GREEN = 12;
  public static final int CYAN = 13;
  public static final int YELLOW = 14;
  public static final int WHITE = 15;
  public static final int[] RAINBOW = new int[]{BLACK, RED, YELLOW, GREEN, CYAN, BLACK, BLACK};

  public final BufferedImage pixel;
  public final BufferedImage ink;
  public final BufferedImage paper;
  public Graphics2D pg;

  public GraphicsModeZx() {
    int[] colorMap = GraphicsMode.ZX.colorMap;
    IndexColorModel colorModel = new IndexColorModel(8, colorMap.length, colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);

    pixel = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
    ink = new BufferedImage(AW, AH, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    paper = new BufferedImage(AW, AH, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    pg = pixel.createGraphics();

    cls();
  }

  /**
   * Fills the rectangle with the indexed color.
   */
  public void clearRect(int x, int y, int width, int height, BufferedImage i, int color) {
    byte[] buffer = new byte[]{(byte) color};
    Rectangle r = new Rectangle(x, y, width, height).intersection(new Rectangle(0, 0, i.getWidth(), i.getHeight()));
    if (r.width < 0 || r.height < 0) {
      return;
    }
    for (int yy = r.y; yy < r.y + r.height; yy++) {
      for (int xx = r.x; xx < r.x + r.width; xx++) {
        i.getRaster().setDataElements(xx, yy, buffer);
      }
    }
  }

  public void attrRect(int x, int y, int width, int height, int ink, int paper) {
    clearRect(x, y, width, height, this.ink, ink);
    clearRect(x, y, width, height, this.paper, paper);
  }

  public void uiRainbow(int x, int y) {
    for (int i = 0; i < RAINBOW.length - 1; i++) {
      this.attrRect(x + i, y, 1, 1, RAINBOW[i | 1], RAINBOW[(i + 1) & 0xFFFE]);
    }
    for (int i = 0; i < 8; i++) {
      int xx = x * 8 + 7 - i;
      int yy = y * 8 + i;
      this.clearRect(xx, yy, 8, 1, this.pixel, -1);
      this.clearRect(xx + 16, yy, 8, 1, this.pixel, -1);
      this.clearRect(xx + 32, yy, i + 1, 1, this.pixel, -1);
    }
  }

  public void cls(int ink, int paper) {
    clearRect(0, 0, AW, AH, this.ink, ink);
    clearRect(0, 0, AW, AH, this.paper, paper);
    //clearRect(0, 0, WIDTH, HEIGHT, pixel, 0);
    // use native method, it probably work faster with 1-bit color model
    pg.setBackground(Color.BLACK);
    pg.clearRect(0, 0, WIDTH, HEIGHT);
  }

  public void cls() {
    cls(7, 0);
  }

  public byte[] toScr() {
    byte[] bytes = new byte[6912];
    DataBuffer buffer = this.pixel.getRaster().getDataBuffer();
    for (int y = 0; y < GraphicsModeZx.HEIGHT; y++) {
      int yy = (y & 0xC0) + ((y & 0x38) >> 3) + ((y & 7) << 3);
      for (int x = 0; x < 32; x++) {
        bytes[y * 32 + x] = (byte) buffer.getElem(yy * 32 + x);
      }
    }
    DataBuffer i = this.ink.getRaster().getDataBuffer();
    DataBuffer p = this.paper.getRaster().getDataBuffer();
    for (int x = 0; x < 24 * 32; x++) {
      bytes[6144 + x] = (byte) (i.getElem(x) & 7 | p.getElem(x) << 3 & 0x78
          | (i.getElem(x) ^ p.getElem(x)) << 4 & 0x80); // see flash support note
    }
    return bytes;
  }

  public void fromScr(byte[] bytes) {
    DataBuffer buffer = this.pixel.getRaster().getDataBuffer();
    for (int y = 0; y < GraphicsModeZx.HEIGHT; y++) {
      int yy = (y & 0xC0) + ((y & 0x38) >> 3) + ((y & 7) << 3);
      for (int x = 0; x < 32; x++) {
        buffer.setElem(yy * 32 + x, bytes[y * 32 + x]);
      }
    }
    DataBuffer i = this.ink.getRaster().getDataBuffer();
    DataBuffer p = this.paper.getRaster().getDataBuffer();
    for (int x = 0; x < 24 * 32; x++) {
      byte col = bytes[6144 + x];
      int bright = col >> 3 & 8;
      int flash = col >> 4 & 8; // see flash support note
      i.setElem(x, bright ^ flash | col & 7);
      p.setElem(x, bright | col >> 3 & 7);
    }
  }

  public void guessInkColorSlow(BufferedImage sourceImage) {
    for (int y8 = 0; y8 < HEIGHT; y8 += 8) {
      for (int x8 = 0; x8 < WIDTH; x8 += 8) {
        Map<Integer, AtomicInteger> h = new HashMap<>();
        for (int y1 = 0; y1 < 8; y1++) {
          for (int x1 = 0; x1 < 8; x1++) {
            h.computeIfAbsent(sourceImage.getRGB(x8 + x1, y8 + y1) & 0xFFFFFF, k -> new AtomicInteger())
                .incrementAndGet();
          }
        }
        Optional.ofNullable(h.get(0)).ifPresent(atomicInteger -> atomicInteger.set(0));
        int rgb = h.entrySet().stream().sorted(Comparator.comparingInt(v -> -v.getValue().get()))
            .map(Map.Entry::getKey).findFirst().orElse(0);
        ink.setRGB(x8 >> 3, y8 >> 3, rgb);
      }
    }
  }

  /**
   * Calculate the most suitable ink color from source.
   * @param sourceImage to pick colors from
   * @param useTransparent pixel 8 will be replaced, full image otherwise
   */
  public void guessInkColorFrom(BufferedImage sourceImage, boolean useTransparent) {
    byte[] buffer = new byte[1];
    int[] hst = new int[16];
    for (int y8 = 0; y8 < HEIGHT; y8 += 8) {
      for (int x8 = 0; x8 < WIDTH; x8 += 8) {
        if (useTransparent) {
          ink.getRaster().getDataElements(x8 >> 3, y8 >> 3, buffer);
          if (buffer[0] != 8) {
            continue;
          }
        }
        Arrays.fill(hst, 0);
        for (int y1 = y8; y1 < y8 + 8; y1++) {
          for (int x1 = x8; x1 < x8 + 8; x1++) {
            sourceImage.getRaster().getDataElements(x1, y1, buffer);
            hst[buffer[0]]++;
          }
        }
        buffer[0] = 0;
        for (int i = 1, v = 0; i < 16; i++) {
          if (hst[i] > v) {
            v = hst[i];
            buffer[0] = (byte) i;
          }
        }
        ink.getRaster().setDataElements(x8 >> 3, y8 >> 3, buffer);
      }
    }
  }

  public void drawSlow(BufferedImage targetImage) {
    for (int y8 = 0; y8 < HEIGHT; y8 += 8) {
      for (int x8 = 0; x8 < WIDTH; x8 += 8) {
        int f = ink.getRGB(x8 >> 3, y8 >> 3);
        int b = paper.getRGB(x8 >> 3, y8 >> 3);
        for (int y1 = 0; y1 < 8; y1++) {
          for (int x1 = 0; x1 < 8; x1++) {
            targetImage.setRGB(x8 + x1, y8 + y1, (pixel.getRGB(x8 + x1, y8 + y1) & 0xFFFFFF) == 0 ? b : f);
          }
        }
      }
    }
  }

  public void draw(BufferedImage targetImage) {
    byte[] f = new byte[1];
    byte[] b = new byte[1];
    byte[] p = new byte[1];
    for (int y8 = 0; y8 < HEIGHT; y8 += 8) {
      for (int x8 = 0; x8 < WIDTH; x8 += 8) {
        ink.getRaster().getDataElements(x8 >> 3, y8 >> 3, f);
        paper.getRaster().getDataElements(x8 >> 3, y8 >> 3, b);
        for (int y1 = y8; y1 < y8 + 8; y1++) {
          for (int x1 = x8; x1 < x8 + 8; x1++) {
            pixel.getRaster().getDataElements(x1, y1, p);
            targetImage.getRaster().setDataElements(x1, y1, p[0] == 0 ? b : f);
          }
        }
      }
    }
  }
}
