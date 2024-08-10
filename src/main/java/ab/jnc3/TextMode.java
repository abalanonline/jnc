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

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.UncheckedIOException;

public class TextMode {

  public final BitmapFont font;
  public final Dimension size;
  public final Dimension aspectRatio = new Dimension(4, 3);
  public final int[] colorMap;
  public final int bgColor;
  public final int fgColor;
  private final IndexColorModel colorModel;

  public TextMode(BitmapFont font, int width, int height, int[] colorMap, int bgColor, int fgColor) {
    this.font = font;
    this.size = new Dimension(width, height);
    this.colorMap = colorMap;
    this.bgColor = bgColor;
    this.fgColor = fgColor;
    this.colorModel = new IndexColorModel(8, colorMap.length, colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);
  }

  public TextMode(int width, int height) {
    this.font = zxFont();
    this.size = new Dimension(width, height);
    this.colorMap = null;
    this.bgColor = 0;
    this.fgColor = 0xAAAAAA;
    this.colorModel = null;
  }

  public int getRgbColor(int indexed) {
    return colorMap == null ? indexed : colorMap[indexed];
  }

  public int getIndexedColor(int rgb) {
    return colorModel == null ? rgb : ((byte[]) colorModel.getDataElements(rgb, null))[0];
  }

  private static byte[] resource(String resource) {
    try {
      return TextMode.class.getResourceAsStream(resource).readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static BitmapFont zxFont() {
    BitmapFont font = new BitmapFont(8, 8);
    System.arraycopy(resource("/jnc2/48.rom"), 0x3D00, font.bitmap, 0x100, 0x0300);
    font.cacheBitmap();
    return font;
  }

  public static TextMode zx() {
    return new TextMode(zxFont(), 256, 192, new int[]{
        0x000000, 0x0000C0, 0xC00000, 0xC000C0, 0x00C000, 0x00C0C0, 0xC0C000, 0xC0C0C0,
        0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF}, 7, 0);
  }

  private static BitmapFont c64Font() {
    BitmapFont font = new BitmapFont(8, 8);
    byte[] b = resource("/jnc2/901225-01.u5");
    System.arraycopy(b, 0, font.bitmap, 0x200, 0x0100);
    System.arraycopy(b, 0x100, font.bitmap, 0x100, 0x0100);
    System.arraycopy(b, 0x800, font.bitmap, 0x300, 0x0100);
    font.cacheBitmap();
    return font;
  }

  public static TextMode c64() {
    return new TextMode(c64Font(), 320, 200, new int[]{
        0x000000, 0xFFFFFF, 0x9F4E44, 0x6ABFC6, 0xA057A3, 0x5CAB5E, 0x50459B, 0xC9D487,
        0xA1683C, 0x6D5412, 0xCB7E75, 0x626262, 0x898989, 0x9AE29B, 0x887ECB, 0xADADAD}, 6, 14);
  }

  public static final int[] COLOR_MAP_CGA = {
      0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
      0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

  private static BitmapFont cgaFont() {
    BitmapFont font = new BitmapFont(8, 8);
    System.arraycopy(resource("/jnc2/vga.fnt"), 0, font.bitmap, 0, 0x0800);
    font.cacheBitmap();
    return font;
  }

  public static TextMode cga16() {
    return new TextMode(cgaFont(), 160, 100, COLOR_MAP_CGA, 0, 7);
  }

  public static TextMode cgaHigh() {
    return new TextMode(cgaFont(), 640, 200, new int[]{0, 0xFFFFFF}, 1, 0);
  }

  public static TextMode cga4() {
    return new TextMode(cgaFont(), 320, 200,
        new int[]{COLOR_MAP_CGA[0], COLOR_MAP_CGA[3], COLOR_MAP_CGA[5], COLOR_MAP_CGA[7]}, 0, 1);
  }

  public static TextMode defaultMode() {
    return new TextMode(320, 240);
  }

}
