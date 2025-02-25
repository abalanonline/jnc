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
import java.util.Arrays;
import java.util.function.Consumer;

public class Tui3 implements Tui {

  private final BitmapFont font;
  private final Dimension size;
  private Screen screen;

  private boolean controlScreen;
  private boolean interpolation;
  private int[] colorMap;
  private boolean update;
  private boolean open;
  private final int[] frontBuffer;
  private final int[] middleBuffer;
  private final int[] backBuffer;

  private Tui3(BitmapFont font, Dimension pixelSize) {
    this.font = font;
    size = new Dimension(pixelSize.width / font.width, pixelSize.height / font.height);
    frontBuffer = new int[size.height * size.width];
    middleBuffer = new int[size.height * size.width];
    backBuffer = new int[size.height * size.width];
  }

  /**
   * We have a screen with the size, ratio and colors and want to make a text device from it.
   */
  public Tui3(BitmapFont font, Screen screen) {
    this(font, new Dimension(screen.image.getWidth(), screen.image.getHeight()));
    this.screen = screen;
  }

  /**
   * We want Tui3 to do the screen management.
   */
  public Tui3(BitmapFont font, Dimension pixelSize, int[] colorMap) {
    this(font, pixelSize);
    controlScreen = true;
    this.colorMap = Arrays.copyOf(colorMap, colorMap.length);
  }

  @Override
  public Tui open() {
    if (open) throw new IllegalStateException("open");
    open = true;
    if (controlScreen) {
      int width = size.width * font.width;
      int height = size.height * font.height;
      screen = new Screen();
      screen.preferredSize = new Dimension(width, height);
      screen.image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED,
          new IndexColorModel(8, colorMap.length, colorMap, 0, false, -1, DataBuffer.TYPE_BYTE));
      screen.interpolation = interpolation;
    }
    new Thread(this::thread, "tui3").start();
    return this;
  }

  @Override
  public void close() {
    if (!open) return;
    open = false;
    synchronized (middleBuffer) {
      middleBuffer.notify();
    }
    if (controlScreen) screen.close();
    Arrays.fill(frontBuffer, 0);
    Arrays.fill(middleBuffer, 0);
    Arrays.fill(backBuffer, 0);
  }

  @Override
  public Dimension getSize() {
    return new Dimension(size);
  }

  private void thread() {
    while (open) {
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
          open = false;
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

  public Tui withInterpolation() {
    if (controlScreen) {
      interpolation = true;
      Screen screen = this.screen;
      if (screen != null) screen.interpolation = true;
    }
    return this;
  }

  public static void main(String[] args) throws IOException {
    BitmapFont font = BitmapFont.fromPsf(Files.readAllBytes(Path.of("/usr/share/kbd/consolefonts/sun12x22.psfu.gz")));
    try (Tui tui = new Tui3(font, new Dimension(960, 720), new int[]{
        0x000000, 0xAA0000, 0x00AA00, 0xAA5500, 0x0000AA, 0xAA00AA, 0x00AAAA, 0xAAAAAA,
        0x555555, 0xFF5555, 0x55FF55, 0xFFFF55, 0x5555FF, 0xFF55FF, 0x55FFFF, 0xFFFFFF}).withInterpolation().open()) {
      TuiUtil.testTui(tui);
    }
  }

}
