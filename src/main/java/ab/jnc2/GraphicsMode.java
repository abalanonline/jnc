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

import java.awt.Dimension;

/**
 * Graphics mode is a pojo. The windows and buffers management belongs to the Screen class
 */
public class GraphicsMode {

  public static final int[] COLOR_MAP_BW = {0, 0xFFFFFF};
  public static final int[] COLOR_MAP_VIC2 = {
      0x000000, 0xFFFFFF, 0x9F4E44, 0x6ABFC6, 0xA057A3, 0x5CAB5E, 0x50459B, 0xC9D487,
      0xA1683C, 0x6D5412, 0xCB7E75, 0x626262, 0x898989, 0x9AE29B, 0x887ECB, 0xADADAD};
  public static final int[] COLOR_MAP_ZX = {
      0x000000, 0x0000EE, 0xEE0000, 0xEE00EE, 0x00EE00, 0x00EEEE, 0xEEEE00, 0xEEEEEE,
      0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  public static final int[] COLOR_MAP_ZX_GAMMA_025 = {
      0x000000, 0x0000C0, 0xC00000, 0xC000C0, 0x00C000, 0x00C0C0, 0xC0C000, 0xC0C0C0,
      0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};
  public static final int[] COLOR_MAP_CGA = {
      0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
      0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

  public static final GraphicsMode CGA_16 =
      new GraphicsMode(160, 100).withColorMap(COLOR_MAP_CGA).withDefaultColors(0, 7);
  public static final GraphicsMode CGA_HIGH = new GraphicsMode(640, 200).withColorMap(COLOR_MAP_BW);
  public static final GraphicsMode ZX =
      new GraphicsMode(256, 192).withColorMap(COLOR_MAP_ZX_GAMMA_025).withDefaultColors(7, 0);
  public static final GraphicsMode C64 =
      new GraphicsMode(320, 200).withColorMap(COLOR_MAP_VIC2).withDefaultColors(6, 14);
  public static final GraphicsMode DEFAULT = new GraphicsMode(320, 240);

  public Dimension aspectRatio = new Dimension(4, 3);
  public Dimension resolution;
  public int[] colorMap;
  public int bgColor = 0;
  public int fgColor = 0xFFFFFF;

  public GraphicsMode(int width, int height) {
    resolution = new Dimension(width, height);
  }

  public GraphicsMode aspectRatio(Dimension aspectRatio) {
    this.aspectRatio = aspectRatio;
    return this;
  }

  public GraphicsMode withColorMap(int[] colorMap) {
    this.colorMap = colorMap;
    return this;
  }

  public GraphicsMode withDefaultColors(int bg, int fg) {
    bgColor = bg;
    fgColor = fg;
    return this;
  }

}
