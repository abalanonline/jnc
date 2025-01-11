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
import ab.tui.TuiUtil;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Tui3 implements Tui {

  private final BitmapFont font;
  private final Dimension size;
  private final Screen screen;
  private boolean closeScreen;
  private boolean update;
  private boolean close;
  private final int[] frontBuffer;
  private final int[] middleBuffer;
  private final int[] backBuffer;

  private Tui3(BitmapFont font, Dimension pixelSize, Screen screen) {
    this.font = font;
    size = new Dimension(pixelSize.width / font.width, pixelSize.height / font.height);
    this.screen = screen;
    frontBuffer = new int[size.height * size.width];
    middleBuffer = new int[size.height * size.width];
    backBuffer = new int[size.height * size.width];
    new Thread(this::thread, "tui3").start();
  }

  public Tui3(BitmapFont font, Screen screen) {
    this(font, new Dimension(screen.image.getWidth(), screen.image.getHeight()), screen);
  }

  public Tui3(BitmapFont font, Dimension pixelSize, int[] colorMap) {
    this(font, pixelSize, new Screen());
    closeScreen = true;
    screen.preferredSize = new Dimension(pixelSize.width, pixelSize.height);
    screen.image = new BufferedImage(pixelSize.width, pixelSize.height, BufferedImage.TYPE_BYTE_INDEXED,
        new IndexColorModel(8, colorMap.length, colorMap, 0, false, -1, DataBuffer.TYPE_BYTE));
  }

  @Override
  public Dimension getSize() {
    return new Dimension(size);
  }

  private void thread() {
    while (!close) {
      if (update) {
        update = false;
        int width = screen.image.getWidth();
        int ww = width - font.width;
        DataBuffer buffer = screen.image.getRaster().getDataBuffer();
        for (int y = 0, i = 0; y < size.height; y++) {
          int yy = y * font.height * width;
          for (int x = 0; x < size.width; x++, i++) {
            int c = middleBuffer[i];
            if (frontBuffer[i] == c) continue;
            frontBuffer[i] = c;
            font.drawCharSimple(c & 0xFFFFFF, x * font.width + yy, ww, buffer, c >> 24 & 0xF, c >>> 28);
          }
        }
        screen.update();
        continue;
      }
      synchronized (middleBuffer) {
        try {
          middleBuffer.wait(250);
          update = true; // low rate update
        } catch (InterruptedException e) {
          close = true;
        }
      }
    }
  }

  @Override
  public void print(int x, int y, String s, int attr) {
    attr <<= 24;
    x += y * size.width;
    for (int i = 0; i < s.length(); i++) backBuffer[x + i] = attr | font.getCode(s.charAt(i));
  }

  @Override
  public void update() {
    System.arraycopy(backBuffer, 0, middleBuffer, 0, middleBuffer.length);
    update = true;
    synchronized (middleBuffer) {
      middleBuffer.notify();
    }
  }

  @Override
  public void setKeyListener(Consumer<String> keyListener) {
    screen.keyListener = keyListener;
  }

  @Override
  public void close() {
    close = true;
    synchronized (middleBuffer) {
      middleBuffer.notify();
    }
    if (closeScreen) screen.close();
  }

  public Tui withInterpolation() {
    if (closeScreen) screen.interpolation = true;
    return this;
  }

  public static void main(String[] args) throws IOException {
    BitmapFont font = BitmapFont.fromPsf(Files.readAllBytes(Path.of("/usr/share/kbd/consolefonts/sun12x22.psfu.gz")));
    try (Tui tui = new Tui3(font, new Dimension(960, 720), new int[]{
        0x000000, 0xAA0000, 0x00AA00, 0xAA5500, 0x0000AA, 0xAA00AA, 0x00AAAA, 0xAAAAAA,
        0x555555, 0xFF5555, 0x55FF55, 0xFFFF55, 0x5555FF, 0xFF55FF, 0x55FFFF, 0xFFFFFF}).withInterpolation()) {
      TuiUtil.testTui(tui);
    }
  }

}
