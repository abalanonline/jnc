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
  private byte[] bytes;
  private int width;
  private int offset8;
  private int step = 8;
  private boolean little;

  @SneakyThrows
  public BmpPattern(Screen screen) {
    bytes = Files.readAllBytes(Paths.get("src/main/resources/48.rom"));
    this.screen = screen;
    graphics = this.screen.image.createGraphics();
    graphics.setBackground(Color.BLACK);
    textFont = TextFont.ZX.get();
    screen.keyListener = this;
    screenWidth = (screen.mode.resolution.width | 0b111) - 0b111;
    screenHeight = screen.mode.resolution.height;
    this.width = screenWidth;
  }

  @Override
  public void keyTyped(KeyEvent e) {
    switch (e.getKeyChar()) {
      case '8':
        if (step == 8) {
          step = 1;
        } else {
          step = 8;
          offset8 = (offset8 | 0b111) - 0b111;
          width = (width | 0b111) - 0b111;
          width = Math.max(8, width);
        }
        break;
      case '-': little = !little; break;
    }
    e.getKeyCode();
  }

  @Override
  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_UP: offset8 -= width * step; break;
      case KeyEvent.VK_DOWN: offset8 += width * step; break;
      case KeyEvent.VK_PAGE_UP: offset8 -= width * screenHeight / 2; break;
      case KeyEvent.VK_PAGE_DOWN: offset8 += width * screenHeight / 2; break;
      case KeyEvent.VK_LEFT: offset8 -= step; break;
      case KeyEvent.VK_RIGHT: offset8 += step; break;
      case KeyEvent.VK_HOME: width -= step; break;
      case KeyEvent.VK_END: width += step; break;
    }
    offset8 = Math.max(0, offset8);
    width = Math.max(step, Math.min(width, screenWidth));
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  @Override
  public void run() {
    graphics.clearRect(0, 0, screenWidth, screenHeight);
    int offset8 = this.offset8; // thread safe
    out:
    for (int y = 0; y < screenHeight; y++) {
      for (int x = 0; x < width; x++) {
        int oh = y * width + x + offset8;
        int ol = oh & 0b111;
        oh >>= 3;
        if (oh >= bytes.length) break out;
        ol = little ? 1 << ol : 0x80 >> ol;
        screen.image.setRGB(x, y, (bytes[oh] & ol) == 0 ? 0x0000AA : 0xFFFF55);
      }
    }
    textFont.print(screen.image,
        String.format("%8X%s", offset8 >> 3, step == 8 ? "  " : "." + (offset8 & 0b111)),
        screenWidth - 80, screenHeight / 2, 0xffffff, 0);
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.DEFAULT);
    screen.flicker(50, new BmpPattern(screen));
  }

}
