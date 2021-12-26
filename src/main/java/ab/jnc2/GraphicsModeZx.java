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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ZX graphic mode.
 */
// TODO: 2021-12-25 support the flashing bit
public class GraphicsModeZx {

  public static final int WIDTH = 256;
  public static final int HEIGHT = 192;
  public static final int AW = 32;
  public static final int AH = 24;

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
    Rectangle r = new Rectangle(x, y, width, height).intersection(new Rectangle(0, 0, AW, AH));
    for (int yy = y; yy < y + height; yy++) {
      for (int xx = x; xx < x + width; xx++) {
        i.getRaster().setDataElements(xx, yy, buffer);
      }
    }
  }

  public void cls(int ink, int paper) {
    clearRect(0, 0, WIDTH, HEIGHT, pixel, 0);
    clearRect(0, 0, AW, AH, this.ink, ink);
    clearRect(0, 0, AW, AH, this.paper, paper);
  }

  public void cls() {
    cls(7, 0);
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
