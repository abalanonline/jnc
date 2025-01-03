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
import java.nio.charset.Charset;

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
    int[] bitmap = {0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, 0xFF, 0xFE, 0xFC, 0xF8, 0xF0, 0xE0, 0xC0, 0x80, 0x00,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0xFF,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0xFF,
        0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01};
    for (int i = 0; i < bitmap.length; i++) font.bitmap[i + 0x80] = (byte) bitmap[i];
    char[] charset = {'\u25E2', '\u25E4', '\u2551', '\u255A', '\u2550', '\u255D', '\u2562'};
    for (int i = 0; i < charset.length; i++) font.put(charset[i], i + 0x10);
    font.put('\u00A9', 0x7F); // (c)
    font.cacheBitmap();
    return font;
  }

  public static TextMode zx() {
    return new TextMode(zxFont(), 256, 192, new int[]{
        0x000000, 0x0000C0, 0xC00000, 0xC000C0, 0x00C000, 0x00C0C0, 0xC0C000, 0xC0C0C0,
        0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF}, 7, 0);
  }

  private static void arraycopy8(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
    System.arraycopy(src, srcPos * 8, dest, destPos * 8, length * 8);
  }

  public static TextMode c64() {
    BitmapFont font = BitmapFont.fromPsf(resource("/jnc3/c64.psf"));
    int[] charset = {'\u25E2', 0x10, '\u25E3', 0x11, '\u25E4', 0xA9, '\u25E5', 0xDF, // triangles
        '\u2551', 0xA5, '\u255A', 0xCC, '\u2550', 0xAF, '\u255D', 0xBA, '\u2562', 0xA7};
    for (int i = 0; i < charset.length; i += 2) font.put((char) charset[i], charset[i + 1]);
    return new TextMode(font, 320, 200, new int[]{
        0x000000, 0xFFFFFF, 0x9F4E44, 0x6ABFC6, 0xA057A3, 0x5CAB5E, 0x50459B, 0xC9D487,
        0xA1683C, 0x6D5412, 0xCB7E75, 0x626262, 0x898989, 0x9AE29B, 0x887ECB, 0xADADAD}, 6, 14);
  }

  public static final int[] COLOR_MAP_CGA = {
      0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
      0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

  private static BitmapFont cgaFont() {
    return vgaFont(8);
  }

  private static BitmapFont vgaFont(int height) {
    int srcPos;
    switch (height) {
      case 8: srcPos = 0; break;
      case 14: srcPos = 0x0800; break;
      case 16: srcPos = 0x172D; break;
      default: throw new IllegalArgumentException();
    }
    BitmapFont font = new BitmapFont(8, height);
    System.arraycopy(resource("/jnc2/vga.fnt"), srcPos, font.bitmap, 0, height * 0x100);
    Charset charset = Charset.forName("IBM437");
    for (int i = 0; i < 0x100; i++) font.put(new String(new byte[]{(byte) i}, charset).charAt(0), i);
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

  public static TextMode ega() {
    return new TextMode(vgaFont(14), 640, 350, COLOR_MAP_CGA, 0, 7);
  }

  public static TextMode vgaHigh() {
    return new TextMode(vgaFont(16), 640, 480, COLOR_MAP_CGA, 0, 7);
  }

  public static TextMode defaultMode() {
    return new TextMode(320, 240);
  }

  public static TextMode msx() {
    BitmapFont font = new BitmapFont(6, 8);
    System.arraycopy(resource("/jnc2/hb10bios.ic12"), 0x1BBF, font.bitmap, 0, 0x800);
    int[] charset = {'\u25E2', 0x84, '\u25E4', 0x85,
        '\u2551', 0x16, '\u255A', 0x1A, '\u2550', 0x17, '\u255D', 0x1B, '\u2562', 0x16};
    for (int i = 0; i < charset.length; i += 2) font.put((char) charset[i], charset[i + 1]);
    // https://en.wikipedia.org/wiki/List_of_8-bit_computer_hardware_graphics
    return new TextMode(font, 240, 192, new int[]{
        0x010101, 0x000000, 0x3EB849, 0x74D07D, 0x5955E0, 0x8076F1, 0xB95E51, 0x65DBEF,
        0xDB6559, 0xFF897D, 0xCCC35E, 0xDED087, 0x3AA241, 0xB766B5, 0xCCCCCC, 0xFFFFFF}, 4, 15);
  }

}
