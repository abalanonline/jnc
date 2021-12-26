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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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
      { 29,  18, 32, 144, 4},
      { 21,  22,130,  88, 2},
  };
  BufferedImage image;
  Screen screen;
  Graphics2D graphics;
  TextFont textFont;
  TextFont symbols;
  BufferedImage png;
  BufferedImage[][][] sprite;
  int[] color;
  Color color0;

  int state;
  Osc mm = new Osc(-1);
  Osc cx = new Osc();
  Osc ex = new Osc();

  Osc nx = new Osc();
  Osc ny = new Osc();
  Rectangle nb = new Rectangle(17, 28);
  boolean ctrl;
  Osc er = new Osc(0x100);
  Osc ep = new Osc(4);
  Rectangle eb = new Rectangle(16, 31);
  Osc tx = new Osc(0x100);
  Osc tp = new Osc(2);
  Rectangle tb = new Rectangle(25, 20);
  Osc[] oscs = {mm, cx, ex, nx, ny, er, ep, tx, tp};
  Nibble tm = new Nibble(0x100, new Nibble(3)).random(1);
  Nibble t2 = new Nibble(0x100, new Nibble(8, 8, 8)).random(0);
  Map<Integer, Rectangle> nn = new HashMap<>();
  boolean test0, test1;
  GraphicsModeZx zxm;

  public MyNewOne(Screen screen) throws IOException {
    this.screen = screen;
    image = screen.createImage();
    graphics = image.createGraphics();
    png = ImageIO.read(Object.class.getResourceAsStream("/MyNewOne.png"));
    color = new int[10];
    for (int i = 0; i < color.length; i++) {
      color[i] = png.getRGB(png.getWidth() - i - 1, png.getHeight() - 1);
    }
    color0 = new Color(color[0]);
    screen.setBackground(color0);
    sprite = new BufferedImage[4][][];
    sprite[0] = new BufferedImage[PNG_MAP.length][];
    for (int i = 0; i < PNG_MAP.length; i++) {
      sprite[0][i] = new BufferedImage[PNG_MAP[i][4]];
      for (int j = 0; j < sprite[0][i].length; j++) {
        sprite[0][i][j] = new BufferedImage(PNG_MAP[i][0], PNG_MAP[i][1], BufferedImage.TYPE_INT_RGB);
        sprite[0][i][j].createGraphics().drawImage(png, -PNG_MAP[i][2] - j * PNG_MAP[i][0], -PNG_MAP[i][3], null);
      }
    }
    textFont = new TextFont("/48.rom", 0x3D00, 0x0300, 0x20, 8, 8);
    textFont = new TextFont("/MyNewOne.fnt", 0, 0x0300, 0x20, 6, 8);
    symbols = new TextFont(new byte[]{108, -2, 124, 56, 16}, '1', 8, 5);
    screen.keyListener = this;
    graphics.setXORMode(color0);

    zxm = new GraphicsModeZx();
    zxm.pg.setXORMode(color0);
    sprite[1] = new BufferedImage[PNG_MAP.length][];
    for (int i = 0; i < PNG_MAP.length; i++) {
      sprite[1][i] = new BufferedImage[PNG_MAP[i][4]];
      for (int j = 0; j < sprite[1][i].length; j++) {
        BufferedImage i0 = new BufferedImage(PNG_MAP[i][0], PNG_MAP[i][1], BufferedImage.TYPE_BYTE_BINARY);
        sprite[1][i][j] = i0;
        BufferedImage i1 = sprite[0][i][j];
        for (int y = 0, mx = PNG_MAP[i][0], my = PNG_MAP[i][1]; y < my; y++) {
          for (int x = 0; x < mx; x++) {
            i0.setRGB(x, y, (i1.getRGB(x, y) & 0xFFFFFF) == 0 ? 0 : -1);
          }
        }
      }
    }
  }

  void draw(int sprite, Rectangle r, int p) {
    //graphics.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    draw(sprite, r.x, r.y, 0, p);
  }

  void draw(int sprite, int xCoord, int yCoord, int mode, int p) {
    int x = xCoord, y = yCoord, w = this.sprite[0][sprite][p].getWidth(), h = this.sprite[0][sprite][p].getHeight();
    switch (mode) {
      case 0: break;
      case 1: x = W - xCoord; w = -w; break;
      default: throw new IllegalStateException();
    }

    Graphics2D[] graphics = new Graphics2D[]{this.graphics, zxm.pg};
    for (int i = 0; i < 2; i++) {
      graphics[i].drawImage(this.sprite[i][sprite][p], x, y, w, h, null);
    }
  }

  void drawAttr(int x, int y, int width, int height, int color) {
    zxm.clearRect(x, y, width, height, zxm.ink, color);
  }

  public void print(String s, int x, int y, int color) {
    textFont.printCentered(image, s, x, y, this.color[color]);
    textFont.printCentered(zxm.pixel, s, x, y, -1);
  }

  void drawScore() {
    print("score 000", 97, 3, 2);
    print("hi score 000", 184, 3, 3);
    int hp = 3;
    symbols.print(image, String.join("", Collections.nCopies(hp, "1")), 32, 3, this.color[5]);
    symbols.print(zxm.pixel, String.join("", Collections.nCopies(hp, "1")), 32, 3, -1);
  }

  void tm(int va, int sd, BiConsumer<Integer, Integer> biConsumer) {
    int adj = va < 0 ? 1 - va / sd : 0;
    for (int i = -((va + sd * adj) % sd), n = (va + sd * adj) / sd - adj; i < H; i += sd, n++) {
      biConsumer.accept(i, n);
    }
  }

  void drawField() {
    drawAttr(0, 0, 2, 24, 10);
    drawAttr(2, 0, 1, 24, 6);
    drawAttr(29, 0, 1, 24, 6);
    drawAttr(30, 0, 2, 24, 11);
    tm(cx.v - T1A, T1D + T1S, (y, n) -> {
      draw(0, 0, y, 0, 0);
      draw(1, 240, y, 0, 0);
      draw(2, 16, y + 1, 0, 0);
      draw(2, 16, y + 1, 1, 0);
      drawAttr(0, (y + 1) >> 3, 2, 2, 6);
      drawAttr(30, (y + 1) >> 3, 2, 2, 6);
      draw(3, 16, y + 24, 0, 0);
      draw(3, 16, y + 24, 1, 0);
      draw(3, 16, y + 108, 0, 0);
      draw(3, 16, y + 108, 1, 0);
      int tmv = n < 0 ? 7 : tm.get(n & 0xFF);
      if ((tmv & 4) == 0) {
        draw(6, 21, (tmv & 2) / 2 * 84 + 65 + y, tmv & 1, 0);
      } else {
        draw(5, 18, (tmv & 2) / 2 * 84 + 55 + y, tmv & 1, -mm.v >> 2 & 3);
      }
    });
  }

  void drawLife() {
    draw(12, 18, 84 + 55 - cx.v, 0, 1);
    tm(ex.v - T2A, T2D + T2S + 32, (y, n) -> {
      if (n < 0) return;
      tb.setLocation(160, y);
      ctrl &= !tb.intersects(nb);
      draw(9, tb, 0);
    });
    tm(ex.v - 50, 31, (y, n) -> {
      if (n < 0) return;
      if (nn.containsKey(n)) {
        Rectangle r = nn.get(n);
        r.height = mm.v;
        nn.put(n, r);
        return;
      }
      eb.setLocation(30, y);
      if (eb.intersects(nb)) {
        nn.putIfAbsent(n, new Rectangle(eb.x, eb.y + cx.v, mm.v, mm.v));
      } else {
        draw(10, eb, 0);
      }
    });
    nn.values().forEach(n -> {
      int p = (n.width - mm.v) >> 4;
      if (p < 4) draw(11, n.x, n.y - cx.v, 0, p);
    });
    nn = nn.entrySet().stream().filter(e -> {
      Rectangle n = e.getValue();
      return Math.min(n.width, n.height) - mm.v < 256;
    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  void init(int state) {
    this.state = state;
    switch (state) {
      case 0:
        cx.v = 0;
        cx.s = 0;
        break;
      case 1:
        nx.s = 0;
        nx.v = 140;
        ny.s = 0;
        ny.v = 73;
        mm.v = 0;
        nn.clear();
        cx.v = 68;
        cx.s = 2;
        ctrl = true;
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

  private void cls() {
    zxm.cls();
    graphics.setBackground(color0);
    graphics.clearRect(0, 0, screen.mode.resolution.width, screen.mode.resolution.height);
  }

  @Override
  public void run() {
    Arrays.stream(oscs).forEach(Osc::inc);
    cls();
    switch (state) {
      case 0:
        drawField();
        draw(4, 24, 16, 0, 0);
        drawAttr(4, 2, 25, 9, 8);
        print("controls:", W/2, 112, 1);
        print("< left: o  -  right: p >", W/2, 120, 1);
        print("space to start", W/2, 144, 1);
        zxm.guessInkColorFrom(image, true);
        break;
      case 1:
        nb.setLocation(nx.v, ny.v);
        ctrl &= nx.v < 219 && nx.v > 20;
        drawField();
        drawLife();
        drawScore();
        draw(8, nb, -mm.v >> 2 & 1);
        break;
      case 4:
        drawField();
        drawScore();
        break;
      case 3:
        print("g a m e    o v e r", W/2, 88, 4);
        break;
    }
    if (test1) {
      screen.image.createGraphics().drawImage(image, 0, 0, null);
      return;
    }
    zxm.draw(screen.image);
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
      case 'q': test0 = false; cx.s = 1; break;
      case 'w': test0 = true; cx.s = 0; break;
      case 'e': test1 = !test1; break;
      case 0x20: System.exit(0);
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_LEFT:
        if (ctrl) nx.s = -1; break;
      case KeyEvent.VK_RIGHT:
        if (ctrl) nx.s = 1; break;
      case KeyEvent.VK_UP:
        if (ctrl) ny.s = -1; break;
      case KeyEvent.VK_DOWN:
        if (ctrl) ny.s = 1; break;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
        if (ctrl) nx.s = 0; break;
      case KeyEvent.VK_UP:
      case KeyEvent.VK_DOWN:
        if (ctrl) ny.s = 0; break;
    }
  }

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
