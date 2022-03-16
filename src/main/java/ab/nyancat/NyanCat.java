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

/**
 * There's more than one way to draw a cat.
 */
public class NyanCat {

  byte[] screen = new byte[70 * 70 * 3];

  public byte[] draw(int frame) {
    cls();
    rainbow(0);
    star(42, 1, 1);
    star(68, 10, 3);
    star(1, 22, 1);
    star(11, 44, 5);
    star(70, 56, 1);
    star(38, 66, 2);
    image(25, 25, POPTART_IMAGE, POPTART_COLOR);
    image(35, 30, HEAD_IMAGE, CAT_COLOR);
    image(19, 32, TAIL_IMAGE[0], CAT_COLOR);
    image(23, 41, LEGS_IMAGE[0], CAT_COLOR);
    return screen;
  }

  void cls() {
    for (int i = 0; i < screen.length; i += 3) {
      screen[i] = 0x00;
      screen[i+1] = 0x33;
      screen[i+2] = 0x66;
    }
  }

  void r8(int x, int y) {
    for (int yy = 0; yy < 18; yy++) {
      for (int xx = 0; xx < 8; xx++) {
        pixel(x + xx, y + yy, RAINBOW_COLOR[yy / 3]);
      }
    }
  }

  void rainbow(int sequence) {
    for (int i = 0; i < 4; i++) {
      r8(19 - 8 * i, 26 + i % 2);
    }
  }

  void star(int x, int y, int sequence) {
    byte[][] image = STAR_IMAGE[sequence];
    for (int iy = 0, ny = y - image.length / 2; iy < image.length; iy++, ny++) {
      byte[] line = image[iy];
      for (int ix = 0, nx = x - line.length / 2; ix < line.length; ix++, nx++) {
        if (line[ix] != 0) pixel(nx, ny, -1);
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
      {1,2,3,3,3,3,3,3,3,3,3,3,5,5,3,3,4,3,3,2,1},
      {1,2,3,3,3,3,3,3,3,3,3,5,5,5,5,3,3,3,3,2,1},
      {1,2,3,3,3,3,3,3,4,3,3,5,5,5,5,5,3,3,3,2,5},
      {1,2,3,3,3,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,4,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,3,3,3,3,4,5,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,4,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5,5},
      {1,2,3,3,3,3,3,4,3,3,5,5,5,5,5,5,5,5,5,5,5},
      {1,2,2,3,4,3,3,3,3,3,5,5,5,5,5,5,5,5,5,5,5},
      {1,2,2,2,3,3,3,3,3,3,3,3,5,5,5,5,5,5,5,5,5},
      {0,1,2,2,2,2,2,2,2,2,2,2,5,5,5,5,5,5,5,5,5},
      {0,0,1,1,1,1,1,1,1,1,1,1,1,5,5,5,5,5,5,5,5},
  };
  public static final int[] CAT_COLOR = {0, 0x000000, 0x999999, 0xFFFFFF, 0xFF9999, 0x050505, 0x060606};
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
      {1,1,1,1,0,0},
      {1,2,2,1,1,0},
      {1,1,2,2,1,1},
      {0,1,1,2,2,1},
      {0,0,1,1,2,2},
      {0,0,0,1,1,1},
      {0,0,0,0,0,1},
  }};
  public static final byte[][][] LEGS_IMAGE = {{
      {0,1,1},
      {1,2,2,2},
      {1,2,2,1,1,0,1,2,2,1,0,0,0,0,0,1,2,2,1,0,1,2,2,1},
      {1,1,1,1,0,0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0,1,1,0},
  }};

}
