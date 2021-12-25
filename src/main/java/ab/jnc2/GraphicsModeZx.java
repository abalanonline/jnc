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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

  private final int[] colorMap;

  public final BufferedImage ink;
  public final BufferedImage ink1;
  public final BufferedImage paper;
  public final BufferedImage pixel;

  public Graphics2D fg;
  public Graphics2D fg1;
  public Graphics2D bg;
  public Graphics2D pg;

  public GraphicsModeZx() {
    colorMap = GraphicsMode.ZX.colorMap;
    IndexColorModel colorModel = new IndexColorModel(8, colorMap.length, colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);

    pixel = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
    ink = new BufferedImage(AW, AH, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    ink1 = new BufferedImage(AW, AH, BufferedImage.TYPE_BYTE_INDEXED,
        // transparent bright black
        new IndexColorModel(8, colorMap.length, colorMap, 0, false, 8, DataBuffer.TYPE_BYTE));
    paper = new BufferedImage(AW, AH, BufferedImage.TYPE_BYTE_INDEXED, colorModel);

    // create graphics for convenience
    pg = pixel.createGraphics();
    fg = ink.createGraphics();
    fg1 = ink1.createGraphics();
    bg = paper.createGraphics();
    cls();
  }

  public void cls(int ink, int paper) {
    pg.setBackground(new Color(colorMap[0]));
    pg.clearRect(0, 0, WIDTH, HEIGHT);
    fg.setBackground(new Color(colorMap[ink]));
    fg.clearRect(0, 0, AW, AH);
    fg1.setBackground(new Color(0, true));
    fg1.clearRect(0, 0, AW, AH);
    bg.setBackground(new Color(colorMap[paper]));
    bg.clearRect(0, 0, AW, AH);
  }

  public void cls() {
    cls(7, 0);
  }

  public void guessInkColor(BufferedImage sourceImage) {
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

  public void draw(BufferedImage targetImage) {
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
}
