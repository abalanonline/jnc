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

import ab.tui.Tui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.function.Consumer;

public class Tui3 implements Tui {

  private final boolean closeScreen;
  private final BitmapFont font;
  private final Dimension size;
  private final Screen screen;

  public Tui3(BitmapFont font, Screen screen) {
    closeScreen = false;
    this.font = font;
    size = new Dimension(screen.image.getWidth() / font.width, screen.image.getHeight() / font.height);
    this.screen = screen;
  }

  public Tui3(BitmapFont font, Dimension pixelSize, int[] colorMap) {
    closeScreen = true;
    this.font = font;
    size = new Dimension(pixelSize.width / font.width, pixelSize.height / font.height);

    screen = new Screen();
    screen.preferredSize = new Dimension(pixelSize.width, pixelSize.height);
    screen.image = new BufferedImage(pixelSize.width, pixelSize.height, BufferedImage.TYPE_BYTE_INDEXED,
        new IndexColorModel(8, colorMap.length, colorMap, 0, false, -1, DataBuffer.TYPE_BYTE));
  }

  @Override
  public Dimension getSize() {
    return new Dimension(size);
  }

  @Override
  public void print(int x, int y, String s, int attr) {
    font.drawStringSimple(s, x * font.width, y * font.height, screen.image, attr & 15, attr >> 4 & 15);
  }

  @Override
  public void update() {
    screen.update();
  }

  @Override
  public void setKeyListener(Consumer<String> keyListener) {
    screen.keyListener = keyListener;
  }

  @Override
  public void close() {
    if (closeScreen) screen.close();
  }

  public Tui withInterpolation() {
    if (closeScreen) screen.interpolation = true;
    return this;
  }

}
