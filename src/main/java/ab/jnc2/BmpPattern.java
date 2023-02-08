/*
 * Copyright 2023 Aleksei Balan
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

import lombok.SneakyThrows;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Quick preview for bitmap fonts.
 */
public class BmpPattern implements Runnable, KeyListener {

  private final Screen screen;
  private final int screenWidth;
  private final int screenHeight;
  private final Graphics2D graphics;
  private final TextFont textFont;
  private final byte[] bytes;
  private int width;
  private int offset;

  @SneakyThrows
  public BmpPattern(Screen screen) {
    bytes = Files.readAllBytes(Paths.get("src/main/resources/48.rom"));
    this.screen = screen;
    graphics = this.screen.image.createGraphics();
    graphics.setBackground(Color.BLACK);
    textFont = TextFont.ZX.get();
    screen.keyListener = this;
    screenWidth = screen.mode.resolution.width;
    screenHeight = screen.mode.resolution.height;
    this.width = screenWidth / 8;
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_UP: offset -= width; break;
      case KeyEvent.VK_DOWN: offset += width; break;
      case KeyEvent.VK_PAGE_UP: offset -= width * screenHeight / 2; break;
      case KeyEvent.VK_PAGE_DOWN: offset += width * screenHeight / 2; break;
      case KeyEvent.VK_LEFT: width--; break;
      case KeyEvent.VK_RIGHT: width++; break;
    }
    offset = Math.max(0, offset);
    width = Math.max(1, Math.min(width, screenWidth / 8));
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  @Override
  public void run() {
    graphics.clearRect(0, 0, screenWidth, screenHeight);
    out:
    for (int y = 0, o = offset; y < screenHeight; y++) {
      for (int x8 = 0; x8 < this.width; x8++) {
        if (o >= bytes.length) break out;
        byte b = bytes[o++];
        for (int x = 0; x < 8; x++) {
          screen.image.setRGB(x8 * 8 + x, y, (b & 0x80 >> x) == 0 ? 0x0000AA : 0xFFFF55);
        }
      }
    }
    textFont.print(screen.image, String.format("%8X", offset), screenWidth - 64, screenHeight / 2, 0xffffff, 0);
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.DEFAULT);
    screen.flicker(50, new BmpPattern(screen));
  }

}
