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
    for (int i = 0; i < screen.length; i += 3) {
      screen[i] = 0x00;
      screen[i+1] = 0x33;
      screen[i+2] = 0x66;
    }
    star(42, 1, 1);
    star(68, 10, 3);
    star(1, 22, 1);
    image(25, 25, POPTART_IMAGE, POPTART_COLOR);
    return screen;
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
  public static final int[] POPTART_COLOR = {0, 0x000000};
  public static final byte[][] POPTART_IMAGE = {
      {0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
  };

}
