/*
 * Copyright 2022 Aleksei Balan
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

package ab.nyancat;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * There's more than one way to draw a cat.
 */
public class NyanCat implements Runnable {

  public static final int[] LINES_CLASSIC = {1, 10, 22, 44, 56, 66};
  public static final int[] LINES_NFT = {17, 22, 34, 44, 47, 56};

  public static final NyanCat CLASSIC = new NyanCat(true, LINES_CLASSIC, 0);
  public static final NyanCat NFT = new NyanCat(false, LINES_NFT, 1);
  public static final NyanCat STARRY = new NyanCat(true, Stream.of(NyanCat.LINES_CLASSIC, NyanCat.LINES_NFT)
      .flatMapToInt(Arrays::stream).distinct().sorted().toArray(), 0);

  byte[] screen = new byte[70 * 70 * 3]; // FIXME: 2022-03-18 a screen must be provided by the user
  private final boolean rainbowTransparent;
  private final int[] starLines;
  private final int starThreshold;

  public NyanCat(boolean rainbowTransparent, int[] starLines, int starThreshold) {
    this.rainbowTransparent = rainbowTransparent;
    this.starLines = starLines;
    this.starThreshold = starThreshold;
  }

  public byte[] draw(int frame) {
    sky();
    if (rainbowTransparent) rainbow1(frame);
    stars(starLines, starThreshold, frame);
    if (!rainbowTransparent) rainbow2(frame);
    cat(frame);
    return screen;
  }

  public void sky() {
    for (int i = 0; i < screen.length;) {
      screen[i++] = 0x00;
      screen[i++] = 0x33;
      screen[i++] = 0x66;
    }
  }

  public void cat(int frame) {
    frame = frame % CAT_CHART.length;
    int[] c = CAT_CHART[frame];
    image(c[0], c[1], POPTART_IMAGE, POPTART_COLOR);
    image(c[2], c[3], HEAD_IMAGE, CAT_COLOR);
    image(c[4], c[5], TAIL_IMAGE[frame], CAT_COLOR);
    image(c[6], c[7], LEGS_IMAGE[frame], CAT_COLOR);
  }

  public void rainbow1(int frame) {
    int sx = (frame >> 1) & 1;
    for (int x = 0; x < 32; x++) {
      int sy = ((x >> 3) ^ (frame >> 1) ^ 1) & 1;
      for (int y = 0; y < 18; y++) {
        pixel(x - sx - 5, y + 26 + sy, RAINBOW_COLOR[y / 3]);
      }
    }
  }

  public void rainbow2(int frame) {
    int sx = frame & 1;
    for (int x = 0; x < 32; x++) {
      int sy = ((x >> 3) ^ (frame >> 1) ^ 1) & 1;
      for (int y = 0; y < 18; y++) {
        pixel(x - sx, y + 26 - sy, RAINBOW_COLOR[y / 3]);
      }
    }
  }

  public void star(int x, int y, int frame) {
    byte[][] image = STAR_IMAGE[frame];
    for (int iy = 0, ny = y - image.length / 2; iy < image.length; iy++, ny++) {
      byte[] line = image[iy];
      for (int ix = 0, nx = x - line.length / 2; ix < line.length; ix++, nx++) {
        if (line[ix] != 0) pixel(nx, ny, -1);
      }
    }
  }

  public void stars(int[] lines, int threshold, int frame) {
    for (int i = 0, j = 0; i < lines.length; i++) {
      int y = lines[i];
      for (;STAR_CHART[j][0] != y; j += 3);
      int f = STAR_CHART[j + 2][frame];
      if (f > threshold) {
        star(STAR_CHART[j + 1][frame], y, f - 1);
      }
    }
  }

  void image(int x, int y, byte[][] image, int[] color) {
    for (int iy = 0, ny = y; iy < image.length; iy++, ny++) {
      byte[] line = image[iy];
      for (int ix = 0, nx = x; ix < line.length; ix++, nx++) {
        byte b = line[ix];
        if (b == 0) continue;
        pixel(nx, ny, color[b]);
      }
    }
  }

  void pixel(int x, int y, int color) {
    if ((x < 0) || (x >= 70) || (y < 0) || (y >= 70)) return;
    int i = (y * 70 + x) * 3;
    screen[i] = (byte) (color >> 16);
    screen[i+1] = (byte) (color >> 8);
    screen[i+2] = (byte) color;
  }

  private String ansi24(byte r, byte g, byte b) {
    return "2;" + (r & 0xFF) + ";" + (g & 0xFF) + ";" + (b & 0xFF) + "m";
  }

  private String ansi8(byte r, byte g, byte b) {
    return "5;" + ((r & 0xFF) / 0x33 * 36 + (g & 0xFF) / 0x33 * 6 + (b & 0xFF) / 0x33 + 16) + "m";
  }

  @Override
  public void run() {
    draw(4);
    for (int y = 23; y < 47; y += 2) {
      int i = 210 * y;
      int j = i + 210;
      StringBuilder sb = new StringBuilder();
      for (int x = 0; x < 70; x++) {
        sb.append("\u001B[38;").append(ansi24(screen[i++], screen[i++], screen[i++]));
        sb.append("\u001B[48;").append(ansi24(screen[j++], screen[j++], screen[j++]));
        sb.append("\u2580\u001B[0m");
      }
      System.out.println(sb.toString());
    }
  }

  public static final int[][] STAR_CHART = {
      { 1}, {42, 36, 28, 19, 10,  4,  0,  0, 68, 60, 52, 46}, {2, 3, 4, 5, 6, 1, 2, 0, 4, 5, 6, 1}, // c
      {10}, {68, 60, 52, 46, 42, 36, 28, 19, 10,  4,  0,  0}, {4, 5, 6, 1, 2, 3, 4, 5, 6, 1, 2, 0}, // c
      {17}, {68, 60, 52,  0, 42, 36, 28, 19, 10,  0,  0,  0}, {4, 5, 6, 0, 2, 3, 4, 5, 6, 0, 2, 0}, // n
      {22}, { 1,  0, 69, 61, 53, 47, 43, 37, 29, 20, 11,  5}, {2, 0, 4, 5, 6, 1, 2, 3, 4, 5, 6, 1},
      {34}, { 0,  0,  0,  0,  0,  0,  0,  0, 68, 60, 52,  0}, {0, 0, 0, 0, 0, 0, 0, 0, 4, 5, 6, 0}, // n
      {44}, {11,  5,  1,  0,  0, 69, 61, 53, 43, 37, 29, 20}, {6, 1, 2, 0, 0, 4, 5, 6, 2, 3, 4, 5},
      {47}, {38, 30, 21, 12,  0,  2,  0,  0,  0, 54,  0, 44}, {3, 4, 5, 6, 0, 2, 0, 0, 0, 6, 0, 2}, // n
      {56}, {69, 66, 60, 51, 42, 34, 28, 24, 18, 10,  2, -1}, {1, 1, 6, 5, 4, 3, 2, 1, 6, 5, 4, 5},
      {66}, {38, 30, 21, 12,  6,  2,  0, 70, 62, 54, 48, 44}, {3, 4, 5, 6, 1, 2, 0, 4, 5, 6, 1, 2}, // c
  };

  public static final int[][] CAT_CHART = {
      // tart, head, tail, legs
      {25, 25, 35, 30, 18, 32, 23, 40},
      {25, 25, 36, 30, 18, 33, 24, 40},
      {25, 26, 36, 31, 18, 36, 25, 41},
      {25, 26, 36, 31, 18, 36, 24, 41},
      {25, 26, 35, 31, 18, 34, 22, 41},
      {25, 26, 35, 30, 18, 33, 22, 41},
  };
  public static final byte[][][] STAR_IMAGE = {
      {{1}},
      {{1},{1,0,1},{1}},
      {{1},{1},{1,1,0,1,1},{1},{1}},
      {{1},{1},{},{1,1,0,1,0,1,1},{},{1},{1}},
      {{1},{1,0,0,0,1},{},{1,0,0,0,0,0,1},{},{1,0,0,0,1},{1}},
      {{1},{},{},{1,0,0,0,0,0,1},{},{},{1}},
  };
  public static final int[] RAINBOW_COLOR = {0xFF0000, 0xFF9900, 0xFFFF00, 0x33FF00, 0x0099FF, 0x6633FF};
  public static final int[] POPTART_COLOR = {0, 0x000000, 0xFFCC99, 0xFF99FF, 0xFF3399, 0x040404};
  public static final byte[][] POPTART_IMAGE = {
      {0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
      {0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0},
      {1,2,2,2,3,3,3,3,3,3,3,3,3,3,3,3,3,2,2,2,1},
      {1,2,2,3,3,3,3,3,3,4,3,3,4,3,3,3,3,3,2,2,1},
      {1,2,3,3,4,3,3,3,3,3,3,3,3,3,3,3,3,3,3,2,1},
      {1,2,3,3,3,3,3,3,3,3,3,3,3,5,3,3,4,3,3,2,1},
      {1,2,3,3,3,3,3,3,3,3,3,3,5,5,5,3,3,3,3,2,1},
      {1,2,3,3,3,3,3,3,4,3,3,3,5,5,5,5,3,3,3,2,1},
      {1,2,3,3,3,3,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,4,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,3,3,3,3,4,3,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,4,3,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,3,3,4,3,3,3,5,5,5,5,5,5,5,5,5,5},
      {1,2,2,3,4,3,3,3,3,3,3,4,5,5,5,5,5,5,5,5,5},
      {1,2,2,2,3,3,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5},
      {0,1,2,2,2,2,2,2,2,2,2,2,2,5,5,5,5,5,5,5,5},
      {0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
  };
  public static final int[] CAT_COLOR = {0, 0x000000, 0x999999, 0xFFFFFF, 0xFF9999};
  public static final byte[][] HEAD_IMAGE = {
      {0,0,1,1,0,0,0,0,0,0,0,0,1,1,0,0},
      {0,1,2,2,1,0,0,0,0,0,0,1,2,2,1,0},
      {0,1,2,2,2,1,0,0,0,0,1,2,2,2,1,0},
      {0,1,2,2,2,2,1,1,1,1,2,2,2,2,1,0},
      {0,1,2,2,2,2,2,2,2,2,2,2,2,2,1,0},
      {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
      {1,2,2,2,3,1,2,2,2,2,2,3,1,2,2,1},
      {1,2,2,2,1,1,2,2,2,1,2,1,1,2,2,1},
      {1,2,4,4,2,2,2,2,2,2,2,2,2,4,4,1},
      {1,2,4,4,2,1,2,2,1,2,2,1,2,4,4,1},
      {0,1,2,2,2,1,1,1,1,1,1,1,2,2,1,0},
      {0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
      {0,0,0,1,1,1,1,1,1,1,1,1,1,0,0,0},
  };
  public static final byte[][][] TAIL_IMAGE = {{
      {0,1,1,1,1,0,0},
      {0,1,2,2,1,1,0},
      {0,1,1,2,2,1,1},
      {0,0,1,1,2,2,1},
      {0,0,0,1,1,2,2},
      {0,0,0,0,1,1,1},
      {0,0,0,0,0,0,1},
  }, {
      {0,0,1,1,0,0,0},
      {0,1,2,2,1,0,0},
      {0,1,2,2,1,1,1},
      {0,0,1,2,2,2,2},
      {0,0,0,1,1,2,2},
      {0,0,0,0,0,1,1},
  }, {
      {0,0,0,0,0,0,1},
      {0,0,0,1,1,1,1},
      {0,1,1,2,2,2,2},
      {0,1,2,2,2,1,1},
      {0,0,1,1,1,1,0},
  }, {
      {0,0,0,0,0,1,1},
      {0,0,0,1,1,2,2},
      {0,0,1,2,2,2,2},
      {0,1,2,2,1,1,1},
      {0,1,2,2,1,0,0},
      {0,0,1,1,0,0,0},
  }, {
      {0,1,1,1,1,0,0},
      {1,2,2,2,1,1,1},
      {1,1,2,2,2,2,1},
      {0,0,1,1,1,1,2},
      {0,0,0,0,0,1,1},
  }, {
      {0,0,1,1,0,0,0},
      {0,1,2,2,1,0,0},
      {0,1,2,2,1,1,1},
      {0,0,1,2,2,2,2},
      {0,0,0,1,1,2,2},
      {0,0,0,0,0,1,1},
  }};
  public static final byte[][][] LEGS_IMAGE = {{
      {},
      {0,1,1},
      {1,2,2,2},
      {1,2,2,1,1,0,1,2,2,1,0,0,0,0,0,1,2,2,1,0,1,2,2,1},
      {1,1,1,1,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,1,1,0},
  }, {
      {},
      {0,1},
      {1,2,2},
      {1,2,2,1,0,1,2,2,1,0,0,0,0,0,0,1,2,2,1,0,1,2,2,1},
      {1,1,1,0,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,1,1,1},
  }, {
      {},
      {1},
      {1,2},
      {1,2,2,1,0,1,2,2,1,0,0,0,0,0,0,1,2,2,1,0,1,2,2,1},
      {1,1,1,0,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,1,1,1},
  }, {
      {},
      {0,1},
      {1,2,2},
      {1,2,2,1,0,1,2,2,1,0,0,0,0,0,0,1,2,2,1,0,1,2,2,1},
      {1,1,1,0,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,1,1,1},
  }, {
      {0,0,1},
      {0,1,1,1},
      {1,2,2,2,1},
      {1,2,2,1,0,1,2,2,1,0,0,0,0,0,0,1,2,2,1,0,1,2,2,1},
      {1,1,1,0,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,1,1,1},
  }, {
      {0,0,1},
      {0,1,2,1},
      {1,2,2,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,1},
      {1,2,2,1,0,1,2,2,1,0,0,0,0,0,0,1,2,2,1,0,1,2,2,1},
      {1,1,1,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,0,1,1,1},
  }};

  /**
   * mvn compile exec:java -Dexec.mainClass=ab.nyancat.NyanCat
   */
  public static void main(String[] args) {
    NyanCat.NFT.run();
  }

}
