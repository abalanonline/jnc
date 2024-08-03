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

import ab.jnc2.GraphicsMode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalTime;

public class BasicClock implements BasicApp {

  private static final int[][] DIGIT2043 = new int[][]{{ // poor replica, the original font is better
      0x03FC0, 0x0FFF0, 0x1FFF8, 0x3FFFC, 0x7FFFE, 0x7FFFE, 0xFF0FF, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, // 0
      0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F,
      0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F,
      0xFE07F, 0xFE07F, 0xFE07F, 0xFF0FF, 0x7FFFE, 0x7FFFE, 0x3FFFC, 0x1FFF8, 0x0FFF0, 0x03FC0}, {
      0x1F0, 0x1F0, 0x3F0, 0x3F0, 0x7F0, 0x1FF0, 0xFFF0, 0xFFF0, 0xFFF0, 0xFFF0, 0xFFF0, 0x7F0, 0x7F0, 0x7F0, // 1
      0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0,
      0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0, 0x7F0}, {
      0x01FC0, 0x0FFF0, 0x1FFF8, 0x3FFFC, 0x7FFFE, 0x7FFFE, 0x7F9FE, 0xFF0FF, 0xFE07F, 0xFE07F, 0xFE07F, // 2
      0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0x000FF, 0x000FF, 0x001FF, 0x001FE, 0x003FE, 0x003FC, 0x007FC,
      0x007F8, 0x00FF0, 0x01FE0, 0x03FC0, 0x07F80, 0x0FF00, 0x1FE00, 0x1FE00, 0x3FC00, 0x7F800, 0x7F000,
      0x7F000, 0xFE000, 0xFE000, 0xFE000, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF}, {
      0x03F80, 0x0FFF0, 0x1FFF8, 0x3FFFC, 0x7FFFE, 0x7FFFE, 0xFF1FE, 0xFE0FF, 0xFE07F, 0xFE07F, 0xFE07F, // 3
      0xFE07F, 0xFE07F, 0x0007F, 0x0007F, 0x0007F, 0x000FE, 0x001FE, 0x01FFC, 0x01FF8, 0x01FF0, 0x01FF0,
      0x01FFC, 0x01FFE, 0x001FE, 0x000FE, 0x0007F, 0x0007F, 0x0007F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F,
      0xFE07F, 0xFE07F, 0xFE0FF, 0xFF0FE, 0x7FFFE, 0x7FFFE, 0x3FFFC, 0x1FFF8, 0x0FFF0, 0x03FC0}, {
      0x001FE, 0x001FE, 0x003FE, 0x003FE, 0x007FE, 0x007FE, 0x007FE, 0x00FFE, 0x00FFE, 0x01FFE, 0x01FFE, // 4
      0x03FFE, 0x03FFE, 0x03FFE, 0x07EFE, 0x07EFE, 0x0FCFE, 0x0FCFE, 0x1FCFE, 0x1F8FE, 0x1F8FE, 0x3F0FE,
      0x3F0FE, 0x7E0FE, 0x7E0FE, 0x7E0FE, 0xFC0FE, 0xFC0FE, 0xF80FE, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF,
      0xFFFFF, 0xFFFFF, 0x000FE, 0x000FE, 0x000FE, 0x000FE, 0x000FE, 0x000FE, 0x000FE, 0x000FE}, {
      0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFE000, 0xFE000, 0xFE000, 0xFE000, 0xFE000, // 5
      0xFE000, 0xFE000, 0xFE000, 0xFE3E0, 0xFEFF8, 0xFFFFC, 0xFFFFE, 0xFFFFE, 0xFFFFE, 0xFF0FF, 0xFE0FF,
      0xFE07F, 0xFE07F, 0xFE07F, 0x0007F, 0x0007F, 0x0007F, 0x0007F, 0x0007F, 0xFE07F, 0xFE07F, 0xFE07F,
      0xFE07F, 0xFE07F, 0xFE07F, 0xFF0FE, 0x7FFFE, 0x7FFFE, 0x3FFFC, 0x1FFF8, 0x0FFF0, 0x01F80}, {
      0x01F80, 0x0FFF0, 0x1FFF8, 0x3FFFC, 0x7FFFE, 0x7FFFE, 0x7F9FE, 0xFF0FF, 0xFE07F, 0xFE07F, 0xFE07F, // 6
      0xFE07F, 0xFE000, 0xFE000, 0xFE000, 0xFE3E0, 0xFE7F8, 0xFEFFC, 0xFFFFE, 0xFFFFE, 0xFFFFE, 0xFF9FF,
      0xFF0FF, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F,
      0xFE07F, 0xFE07F, 0xFE0FF, 0xFF1FE, 0x7FFFE, 0x7FFFE, 0x3FFFC, 0x1FFF8, 0x0FFF0, 0x03FC0}, {
      0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0xFFFFF, 0x0007F, 0x0007F, 0x0007E, 0x000FE, 0x000FE, // 7
      0x000FE, 0x000FC, 0x001FC, 0x001FC, 0x001F8, 0x001F8, 0x003F8, 0x003F8, 0x003F0, 0x007F0, 0x007F0,
      0x007F0, 0x007E0, 0x00FE0, 0x00FE0, 0x00FE0, 0x00FC0, 0x01FC0, 0x01FC0, 0x01F80, 0x01F80, 0x03F80,
      0x03F80, 0x03F00, 0x07F00, 0x07F00, 0x07F00, 0x07E00, 0x0FE00, 0x0FE00, 0x0FE00, 0x0FC00}, {
      0x01F80, 0x0FFF0, 0x1FFF8, 0x3FFFC, 0x7FFFC, 0x7FFFE, 0x7F9FE, 0xFF0FF, 0xFE07F, 0xFE07F, 0xFE07F, // 8
      0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0x7F0FE, 0x7F1FE, 0x3FFFC, 0x3FFF8, 0x0FFF0, 0x1FFF8,
      0x3FFFC, 0x7FFFE, 0x7F9FE, 0xFF0FE, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F,
      0xFE07F, 0xFE07F, 0xFE0FF, 0xFF1FE, 0x7FFFE, 0x7FFFE, 0x3FFFC, 0x1FFF8, 0x0FFF0, 0x03FC0}, {
      0x03F00, 0x0FFF0, 0x1FFF8, 0x3FFFC, 0x7FFFE, 0x7FFFE, 0xFFFFE, 0xFF0FF, 0xFE0FF, 0xFE07F, 0xFE07F, // 9
      0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFE07F, 0xFF0FF, 0xFF0FF,
      0xFFFFF, 0xFFFFF, 0x7FFFF, 0x3FFFF, 0x1FF7F, 0x0FE7F, 0x0007F, 0x0007F, 0x0007F, 0xFE07F, 0xFE07F,
      0xFF07F, 0xFF07F, 0xFF0FF, 0xFF8FF, 0x7FFFF, 0x7FFFE, 0x3FFFE, 0x1FFFC, 0x0FFF8, 0x03FE0}, {
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0, 0, 0, 0, // :
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F, 0x7F}};
  private static final int[][] DIGIT0613 = new int[][]{{
      30, 63, 51, 51, 51, 51, 51, 51, 51, 51, 51, 63, 30, }, {
      6, 14, 30, 30, 6, 6, 6, 6, 6, 6, 6, 6, 6, }, {
      30, 63, 51, 51, 51, 6, 6, 12, 24, 48, 48, 63, 63, }, {
      30, 63, 51, 51, 3, 14, 14, 3, 3, 51, 51, 63, 30, }, {
      3, 7, 7, 15, 15, 27, 27, 51, 51, 63, 63, 3, 3, }, {
      63, 63, 48, 48, 54, 63, 51, 3, 3, 51, 51, 63, 30, }, {
      30, 63, 51, 51, 48, 54, 63, 51, 51, 51, 51, 63, 30, }, {
      62, 62, 6, 6, 6, 4, 12, 12, 12, 24, 24, 24, 24, }, {
      30, 63, 51, 51, 51, 30, 30, 51, 51, 51, 51, 63, 30, }, {
      30, 63, 51, 51, 51, 51, 63, 27, 3, 51, 51, 63, 30, }, {}};
  private static final int LCD_BRIGHT = 0xAAAAAA;

  private BufferedImage png;
  private int pngw;
  private int pngh;
  private int[][] indexed;
  private Basic basic;
  private int w;
  private int h;
  private int dx;
  private int dy;
  boolean stop;

  @Override
  public GraphicsMode preferredMode() {
    return new GraphicsMode(320, 240);
  }

  @Override
  public void open(Basic basic) {
    if (png == null) try {
      png = ImageIO.read(getClass().getResourceAsStream("/jnc3/garmin.png"));
      pngw = png.getWidth();
      pngh = png.getHeight();
      indexed = new int[pngh][pngw];
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    this.basic = basic;
    Dimension size = basic.getSize();
    w = size.width;
    h = size.height;
    dx = (w - pngw) / 2;
    dy = h - 1 - (h - pngh) / 2;
    for (int y = 0; y < pngh; y++) {
      int by = dy - y;
      if (by < 0 || by >= h) continue;
      for (int x = 0; x < pngw; x++) {
        int bx = dx + x;
        if (bx < 0 || bx >= w) continue;
        int c = basic.getColorFromRgb(png.getRGB(x, y));
        indexed[y][x] = c;
        basic.ink(c);
        basic.plot(bx, by);
      }
    }
    stop = false;
  }

  private void drawDigits(int[][] font, int width, int height, int y, int... x) {
    final int bright = basic.getColorFromRgb(LCD_BRIGHT);
    for (int i = 0; i < x.length; i++) {
      int cx = x[i++];
      int[] c = font[x[i]];
      for (int iy = 0, by = dy - y; iy < height; iy++, by--) {
        if (by < 0 || by >= h) continue;
        for (int ix = cx, cy = c[iy] << Integer.SIZE - width; ix < cx + width; ix++, cy <<= 1) {
          int bx = dx + ix;
          if (bx < 0 || bx >= w) continue;
          basic.ink(cy < 0 ? bright : indexed[iy + y][ix]);
          basic.plot(bx, by);
        }
      }
    }
  }

  @Override
  public void run() {
    int color = basic.getColorFromRgb(0xAA5500);
    Dimension textSize = basic.getTextSize();
    int x = textSize.width;
    int y = textSize.height - 1;
    while (!stop) {
      LocalTime now = LocalTime.now();
      drawDigits(DIGIT2043, 20, 43, 112, 105, 10,
          70, now.getHour() / 10, 95, now.getHour() % 10,
          129, now.getMinute() / 10, 154, now.getMinute() % 10);
      drawDigits(DIGIT0613, 6, 13, 112, 178, now.getSecond() / 10, 185, now.getSecond() % 10);
      basic.ink(color);
      basic.printAt(x - 4, y - 1, "JNC3");
      basic.printAt(x - 5, y, now.toString().substring(0, 5));
      basic.update();
      if ("q".equals(basic.inkey(100))) stop = true;
    }
  }

  @Override
  public void close() {
    stop = true;
  }
}
