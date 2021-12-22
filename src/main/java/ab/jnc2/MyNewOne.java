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

import ab.Nibble;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class MyNewOne implements Runnable, KeyListener {
  public static final int W = 256;
  public static final int H = 192;
  public static final int T1A = 180, T1D = 180, T1S = 0, T1R = 0;
  public static final int T2A = 0, T2D = 20, T2S = 0, T2R = 0;
  public static final int[][] PNG_MAP = new int[][]{
      { 16, 180,   0,  0, 1},
      { 16, 180, 240,  0, 1},
      {  8,   9,  16,  1, 1},
      {  4,  59,  16, 24, 1},
      {208,  72,  24, 16, 1},
      { 23,  22,  24, 88, 4}, // 5
      { 10,  21, 120, 88, 1}, // 6
      { 19,  26, 213, 88, 1},
      { 17,  28, 144, 112, 2},
      { 25,  20, 24, 112, 2},
      { 16,  31, 80, 112, 4},
  };
  Screen screen;
  Graphics2D graphics;
  TextFont textFont;
  TextFont symbols;
  BufferedImage png;
  BufferedImage[][] sprite;
  int[] color;
  Color color0;

  int state;
  Osc mm = new Osc(-1);
  Osc cx = new Osc();
  Osc ex = new Osc();

  Osc nx = new Osc();
  Osc ny = new Osc();
  Rectangle nb;
  Osc er = new Osc(0x100);
  Osc ep = new Osc(4);
  Osc tx = new Osc(0x100);
  Osc tp = new Osc(2);
  Osc[] oscs = {mm, cx, ex, nx, ny, er, ep, tx, tp};
  Nibble.Random tm = new Nibble.Random(new Nibble(0x100, new Nibble(3))).init(1);
  Nibble.Random t2 = new Nibble.Random(new Nibble(0x100, new Nibble(8, 8, 8))).init(0);

  public MyNewOne(Screen screen) throws IOException {
    this.screen = screen;
    graphics = screen.image.createGraphics();
    png = ImageIO.read(Object.class.getResourceAsStream("/MyNewOne.png"));
    color = new int[10];
    for (int i = 0; i < color.length; i++) {
      color[i] = png.getRGB(png.getWidth() - i - 1, png.getHeight() - 1);
    }
    color0 = new Color(color[0]);
    screen.setBackground(color0);
    sprite = new BufferedImage[PNG_MAP.length][];
    for (int i = 0; i < PNG_MAP.length; i++) {
      sprite[i] = new BufferedImage[PNG_MAP[i][4]];
      for (int j = 0; j < sprite[i].length; j++) {
        sprite[i][j] = new BufferedImage(PNG_MAP[i][0], PNG_MAP[i][1], BufferedImage.TYPE_INT_RGB);
        sprite[i][j].createGraphics().drawImage(png, -PNG_MAP[i][2] - j * PNG_MAP[i][0], -PNG_MAP[i][3], null);
      }
    }
    textFont = new TextFont("/48.rom", 0x3D00, 0x0300, 0x20, 8, 8);
    symbols = new TextFont(new byte[]{108, -2, 124, 56, 16}, '1', 8, 5);
    screen.keyListener = this;
    graphics.setXORMode(color0);
  }

  void draw(int sprite, int x, int y, int p) {
    draw(sprite, x, y, 0, p);
  }

  void draw(int sprite, int x, int y, int mode, int p) {
    BufferedImage image = this.sprite[sprite][p];
    switch (mode) {
      case 0:
        graphics.drawImage(image, x, y, null);
        break;
      case 1:
        graphics.drawImage(image, W - x, y, -image.getWidth(), image.getHeight(), null);
        break;
    }
  }

  public void print(String s, int x, int y, int color) {
    textFont.printCentered(screen.image, s, x, y, this.color[color]);
  }

  void drawScore() {
    print("score 000", 97, 3, 2);
    print("hi score 000", 184, 3, 3);
  }

  void tm(int va, int sd, BiConsumer<Integer, Integer> biConsumer) {
    int adj = va < 0 ? 1 - va / sd : 0;
    for (int i = -((va + sd * adj) % sd), n = (va + sd * adj) / sd - adj; i < H; i += sd, n++) {
      biConsumer.accept(i, n);
    }
  }

  void drawField() {
    graphics.setBackground(color0);
    graphics.clearRect(0, 0, screen.mode.resolution.width, screen.mode.resolution.height);
    tm(cx.v - T1A, T1D + T1S, (y, n) -> {
      draw(0, 0, y, 0, 0);
      draw(1, 240, y, 0, 0);
      draw(2, 16, y + 1, 0, 0);
      draw(2, 16, y + 1, 1, 0);
      draw(3, 16, y + 24, 0, 0);
      draw(3, 16, y + 24, 1, 0);
      draw(3, 16, y + 108, 0, 0);
      draw(3, 16, y + 108, 1, 0);
      int tmv = n < 0 ? 7 : tm.get(n);
      if ((tmv & 4) == 0) {
        draw(6, 21, (tmv & 2) / 2 * 84 + 65 + y, tmv & 1, 0);
      } else {
        draw(5, 18, (tmv & 2) / 2 * 84 + 55 + y, tmv & 1, ((16 - mm.v % 16) % 16) / 4);
      }
    });
    tm(ex.v - T2A, T2D + T2S, (y, n) -> {
      if (n < 0) return;
      draw(9, 128, y, 0);
    });
  }

  void init(int state) {
    this.state = state;
    switch (state) {
      case 0:
        cx.v = 0;
        cx.s = 0;
        break;
      case 1:
        cx.s = 1;
        break;
      case 4:
        cx.v = 0;
        cx.s = 1;
        break;
      case 9:
        cx.v = -200;
        cx.s = 0;
        this.state = 0;
        break;
    }
  }

  @Override
  public void run() {
    Arrays.stream(oscs).forEach(Osc::inc);
    switch (state) {
      case 0:
        drawField();
        draw(4, 24, 16, 0);
        print("controls:", W/2, 112, 1);
        print("< left: o  -  right: p >", W/2, 120, 1);
        print("space to start", W/2, 144, 1);
        break;
      case 1:
        drawField();
        drawScore();
        break;
      case 4:
        drawField();
        drawScore();
        draw(8, W/2, 16, ((8 - mm.v % 8) % 8) / 4);
        break;
      case 3:
        print("g a m e    o v e r", W/2, 88, 4);
        break;
    }

  }

  @Override
  public void keyTyped(KeyEvent e) {
    GraphicsMode newMode;
    switch (e.getKeyChar()) {
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        init(e.getKeyChar() - '0'); break;
      case 'q': cx.s = 1; break;
      case 'w': cx.s = 0; break;
      case 0x20: System.exit(0);
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {}

  @Override
  public void keyReleased(KeyEvent e) {}

  public static void main(String[] args) throws IOException {
    Screen screen = new Screen(GraphicsMode.ZX);
    Runnable basicProgram = new MyNewOne(screen);
    while (true) {
      basicProgram.run();
      screen.repaint();
      try {
        Thread.sleep(20 - Instant.now().getNano() / 1_000_000 % 20 + 2); // nano sync
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public static class Osc { // oscillator
    int v; // value
    int s; // speed
    int limit; // bound or limit, exclusive

    public Osc() {
    }

    /**
     * Osc(0) forward 0, 1, 2, 3, 4
     * Osc(3) forward with limit 0, 1, 2, 0, 1
     * Osc(-3) backward 3, 2, 1, 0, -1
     * @param l
     */
    public Osc(int l) {
      if (l >= 0) {
        s = 1;
        limit = l;
      } else {
        s = -1;
        v = -l;
      }
    }

    void inc() {
      v = limit == 0 ? v + s : (v + s) % limit;
    }
  }

}
