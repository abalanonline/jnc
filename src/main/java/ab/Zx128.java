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

package ab;

import ab.jnc2.GraphicsMode;
import ab.jnc2.GraphicsModeZx;
import ab.jnc2.Screen;
import ab.jnc2.TextFont;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;

public class Zx128 implements Runnable, KeyListener { // FIXME: 2022-02-27 poor naming

  public static final String[] CLASSES = new String[]{ // TODO: 2022-02-27 class scan
      "ab.jnc2.BasicClock",
      "ab.jnc2.AmigaBall",
      "ab.jnc.g3.Game3",
  };

  public static final int BLACK = 8;
  public static final int RED = 10;
  public static final int GREEN = 12;
  public static final int CYAN = 13;
  public static final int YELLOW = 14;
  public static final int WHITE = 15;

  Screen screen;
  GraphicsModeZx zxm;
  private TextFont textFont;
  private int selected;

  public Zx128(Screen screen) {
    this.screen = screen;
    this.zxm = new GraphicsModeZx();
    textFont = new TextFont("/48.rom", 0x3D00, 0x0300, 0x20, 8, 8);
    if (screen == null) {
      return; // return from constructor!
    }
    screen.keyListener = this;
  }

  public void menu(String title, String[] items, String footer, int selected) {
    int maxWidth = Arrays.stream(items).mapToInt(String::length).max().orElse(0);
    int maxHeight = items.length;
    int w = Math.max(14, (maxWidth + 3) & 0xFFFE);
    int h = Math.max(5, maxHeight + 1);
    int x = Math.max(0, 14 - w / 2);
    int y = Math.max(1, 11 - (h + 1) / 2);
    zxm.cls(0, 7); // dark black and white

    zxm.attrRect(x, y - 1, w, 1, WHITE, BLACK);
    textFont.print(zxm.pixel, title, x * 8, y * 8 - 8, -1);

    zxm.attrRect(x, y, w, h, BLACK, WHITE);
    zxm.clearRect(x * 8, y * 8, w * 8, h * 8, zxm.pixel, -1);
    zxm.clearRect(x * 8 + 1, y * 8, w * 8 - 2, h * 8 - 1, zxm.pixel, 0);
    for (int i = 0; i < items.length; i++) {
      textFont.print(zxm.pixel, items[i], x * 8 + 8, (y + i) * 8, -1);
    }
    zxm.attrRect(x, y + selected, w, 1, BLACK, CYAN);

    int[] rainbow = new int[]{BLACK, RED, YELLOW, GREEN, CYAN, BLACK, BLACK};
    for (int i = 0; i < rainbow.length - 1; i++) {
      zxm.attrRect(x + w - 6 + i, y - 1, 1, 1, rainbow[i | 1], rainbow[(i + 1) & 0xFFFE]);
    }
    for (int i = 0; i < 8; i++) {
      int xx = (x + w) * 8 - 41;
      int yy = y * 8 - 8;
      zxm.clearRect(xx - i, yy + i, 8, 1, zxm.pixel, -1);
      zxm.clearRect(xx - i + 16, yy + i, 8, 1, zxm.pixel, -1);
      zxm.clearRect(xx - i + 32, yy + i, i + 1, 1, zxm.pixel, -1);
    }

    String[] footers = footer.split("\n");
    for (int i = 0; i < footers.length; i++) {
      textFont.print(zxm.pixel, footers[i], 0, (i - footers.length) * 8 + GraphicsModeZx.HEIGHT, -1);
    }
  }

  @Override
  public void run() {

    menu("JNC2",
        Arrays.stream(CLASSES).map(s -> s.replaceAll("ab\\.jnc2?\\.", "")).toArray(String[]::new),
        "\u007F 2022 Apache License v2.0", this.selected);
    zxm.draw(screen.image);
  }

  @Override
  public void keyTyped(KeyEvent e) {
    switch (e.getKeyChar()) {
      case KeyEvent.VK_ENTER:
        System.exit(1);
        break;
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_UP:
        this.selected = (this.selected + CLASSES.length - 1) % CLASSES.length;
        break;
      case KeyEvent.VK_DOWN:
        this.selected = (this.selected + 1) % CLASSES.length;
        break;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.ZX);
    Runnable basicProgram = new Zx128(screen);
    while (true) {
      basicProgram.run();
      screen.repaint();
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

}
