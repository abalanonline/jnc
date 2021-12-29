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
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NohzDyve implements Runnable, KeyListener {
  public static final int W = 256;
  public static final int H = 192;
  public static final int T1A = 180, T1D = 180, T1S = 0, T1R = 0;
  public static final int T2A = 180 + 90, T2D = 32, T2S = H, T2R = 200;
  public static final int[][] PNG_MAP = new int[][]{
      { 16, 180,   0,   0, 1},
      { 16, 180, 240,   0, 1},
      {  8,   9,  16,   1, 1},
      {  4,  59,  16,  24, 1},
      {208,  72,  24,  16, 1},
      { 23,  22,  24,  88, 4}, // 5
      { 10,  21, 120,  88, 1}, // 6
      { 19,  26, 213,  88, 1},
      { 17,  28, 144, 112, 2},
      { 25,  20,  24, 112, 2},
      { 16,  31,  80, 112, 4},
      { 29,  18,  32, 144, 4},
      { 21,  22, 130,  88, 2},
      { 31,  15, 176,  88, 1},
      {208,   9,  24,   1, 1}, // 14
      { 32,  31, 200, 161, 1},
      { 16,  22, 136, 168, 4},
  };
  public static int throttling;

  BufferedImage image;
  Screen screen;
  Graphics2D graphics;
  TextFont textFont;
  TextFont symbols;
  BufferedImage png;
  BufferedImage[][][] sprite;
  int[] color;
  int[] indexedColor;
  Color color0;

  int state;
  int gotoState;
  Osc mm = new Osc(-1);
  Osc cx = new Osc();
  Osc tx = new Osc();

  Osc nx = new Osc();
  Osc ny = new Osc();
  Rectangle nb = new Rectangle(17, 28);
  boolean ctrl;
  Rectangle eb = new Rectangle(16, 31);
  Rectangle tb = new Rectangle(25, 20);
  Osc[] oscs = {mm, cx, tx, nx, ny};
  Nibble tm = new Nibble(0x1000, new Nibble(3, 3)).random(6);
  Nibble tme = new Nibble(0x1000, new Nibble(2, 7, 6, 7)).random(6);
  Nibble tmt = new Nibble(0x1000, new Nibble(2, 8, 6)).random(0);
  Map<Integer, Rectangle> nn = new HashMap<>();
  Set<Integer> vt = new HashSet<>();
  GraphicsModeZx zxm;
  int[] sin32 = IntStream.range(0, 128).map(i -> (int) (Math.sin(Math.PI * i / 64) * 16)).toArray();
  int hp, score, hiscore;
  private final Clip music;
  private final Clip sound;

  public NohzDyve(Screen screen) throws Exception {
    screen.setPreferredSize(new Dimension(screen.mode.resolution.width * 2, screen.mode.resolution.height * 2));
    screen.setTitle(this.getClass().getSimpleName() + ".java");
    this.screen = screen;
    image = screen.createImage();
    graphics = image.createGraphics();
    String resName = "/" + this.getClass().getSimpleName() + ".";
    png = ImageIO.read(getClass().getResourceAsStream(resName + "png"));
    color = new int[10];
    byte[] buffer = new byte[1];
    indexedColor = new int[color.length];
    for (int i = 0; i < color.length; i++) {
      color[i] = png.getRGB(png.getWidth() - i - 1, png.getHeight() - 1);
      image.getColorModel().getDataElements(color[i], buffer);
      indexedColor[i] = buffer[0];
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
    textFont = new TextFont(resName + "fnt", 0, 0x0300, 0x20, 6, 8);
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

    music = AudioSystem.getClip();
    music.open(AudioSystem.getAudioInputStream(
        new BufferedInputStream(getClass().getResourceAsStream(resName + "wav"))));
    music.setLoopPoints(18000, 18000 + 56357);
    sound = AudioSystem.getClip();
    sound.open(AudioSystem.getAudioInputStream(
        new BufferedInputStream(getClass().getResourceAsStream(resName + "wav"))));
    sound.setLoopPoints(87800, 88400);
  }

  void sound(int sample) {
    switch (sample) {
      case 0:
        music.stop();
        music.flush();
        music.setFramePosition(0);
        music.loop(Clip.LOOP_CONTINUOUSLY);
        break;
      case 1:
        sound.stop();
        sound.flush();
        sound.setFramePosition(85200);
        sound.loop(Clip.LOOP_CONTINUOUSLY);
        break;
      case 2:
        music.stop();
        music.flush();
        music.setFramePosition(95000);
        music.start();
        break;
    }
  }

  void draw(Rectangle r, int sprite, int p) {
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

  void drawAttr(Rectangle r, int color) {
    drawAttr(r.x, r.y, r.width, r.height, color);
  }

  void drawAttr(int x, int y, int width, int height, int color) {
    int x1 = x >> 3, y1 = y >> 3, x2 = (x + width + 7) >> 3, y2 = (y + height + 7) >> 3;
    zxm.clearRect(x1, y1, x2 - x1, y2 - y1, zxm.ink, color);
  }

  public void print(String s, int x, int y, int color) {
    textFont.printCentered(image, s, x, y, this.color[color]);
    textFont.printCentered(zxm.pixel, s, x, y, -1);
    int w = textFont.width * s.length();
    drawAttr(x - w/2, y, w, textFont.height, indexedColor[color]);
  }

  void drawScore() {
    print(String.format("score %04d0", score), 97, 3, 2);
    print(String.format("hi score %04d0", hiscore), 184, 3, 3);
    symbols.print(image, String.join("", Collections.nCopies(hp, "1")), 32, 3, this.color[5]);
    symbols.print(zxm.pixel, String.join("", Collections.nCopies(hp, "1")), 32, 3, -1);
    drawAttr(32, 3, symbols.width * 3, symbols.height, indexedColor[5]);
    //print(String.join("", Collections.nCopies(throttling | 1, "-")), 128, 9, 3);
  }

  void tm(int a, int s, int f, int t, BiConsumer<Integer, Integer> consumer) {
    int adj = a + f < 0 ? 1 - (a + f) / s : 0;
    for (int x = f - ((a + f + s * adj) % s), n = (a + f + s * adj) / s - adj; x < t; x += s, n++) {
      consumer.accept(x, n);
    }
  }

  void tm(int a, int s, BiConsumer<Integer, Integer> consumer) {
    tm(a, s, 0, H, consumer);
  }

  void drawField() {
    drawAttr(0, 0, 16, 192, 10);
    drawAttr(16, 0, 8, 192, 6);
    drawAttr(232, 0, 8, 192, 6);
    drawAttr(240, 0, 16, 192, 11);
    tm(cx.v - T1A, T1D + T1S, (y, n) -> {
      draw(0, 0, y, 0, 0);
      draw(1, 240, y, 0, 0);
      draw(2, 16, y + 1, 0, 0);
      draw(2, 16, y + 1, 1, 0);
      drawAttr(0, y + 1, 16, 9, 6);
      drawAttr(240, y + 1, 16, 9, 6);
      draw(3, 16, y + 24, 0, 0);
      draw(3, 16, y + 24, 1, 0);
      draw(3, 16, y + 108, 0, 0);
      draw(3, 16, y + 108, 1, 0);
      int tmv = n < 0 ? 7 : tm.get(n & 0xFFF, 0);
      if ((tmv & 4) == 0) {
        int yy = (tmv & 2) / 2 * 84 + 65 + y;
        draw(6, 21, yy, tmv & 1, 0);
        drawAttr((tmv & 1) == 0 ? 24 : 224, yy, 8, 21, 6);
      } else {
        int yy = (tmv & 2) / 2 * 84 + 55 + y;
        draw(5, 18, yy, tmv & 1, -mm.v >> 2 & 3);
        drawAttr((tmv & 1) == 0 ? 24 : 208, yy, 24, 22, 5);
      }
      tmv = n < 1 ? 4 : tm.get(n & 0xFFF, 1);
      if (tmv < 4) {
        draw(14, 24, y + 1, 0, 0);
        drawAttr(24, y + 1, 208, 9, 1);
      }
      switch (tmv) {
        case 0:
          draw(16, 40, y + 8, 0, 0);
          draw(16, 144, y + 12, 0, 2);
          draw(16, 168, y + 11, 0, 3);
          draw(15, 184, y + 8, 0, 0);
          drawAttr(40, y + 8, 16, 22, 4);
          drawAttr(144, y + 12, 16, 21, 4);
          drawAttr(168, y + 11, 8, 11, 5);
          drawAttr(184, y + 8, 32, 31, 5);
          break;
        case 1:
          draw(16, 64, y + 10, 0, 1);
          draw(16, 168, y + 11, 0, 3);
          draw(16, 184, y + 9, 0, 2);
          drawAttr(64, y + 10, 16, 8, 7);
          drawAttr(168, y + 11, 8, 11, 2);
          drawAttr(184, y + 9, 16, 21, 2);
          break;
        case 2: // mirrored 0
          draw(16, 40, y + 8, 1, 0);
          draw(16, 144, y + 12, 1, 2);
          draw(16, 168, y + 11, 1, 3);
          draw(15, 184, y + 8, 1, 0);
          drawAttr(200, y + 8, 16, 22, 4);
          drawAttr(96, y + 12, 16, 21, 3);
          drawAttr(80, y + 11, 8, 11, 3);
          drawAttr(40, y + 8, 32, 31, 3);
          break;
        case 3:
          draw(16, 40, y + 8, 0, 0);
          draw(16, 64, y + 10, 0, 1);
          draw(16, 184, y + 9, 0, 2);
          drawAttr(40, y + 8, 16, 22, 2);
          drawAttr(64, y + 10, 16, 8, 7);
          drawAttr(184, y + 9, 16, 21, 6);
          break;
      }
    });
  }

  void drawLife() {
    if (cx.v < T1A) {
      draw(12, 18, 84 + 55 - cx.v, 0, 1);
      drawAttr(24, 139 - cx.v, 16, 22, 6);
    }
    if (ctrl && state == 1) vt.clear();
    //tm(tx.v - T2A, T2D + T2S, (y, n) -> {
    //tm((tx.v - T2A + H/2) * 16, (T2D + T2S) * 16, -H / 2 * 16, H / 2 * 16, (y, n) -> { y /= 16; y += H/2;
    tm(tx.v + cx.v * 16 - 68*16 - T2A * 16 + H/2 * 16, (T2D + T2S) * 16, -H / 2 * tx.s, H / 2 * tx.s, (y, n) -> { y /= tx.s; y += H/2;
      if (n < 0) return;
      if (ctrl && state == 1) {
        vt.add(n);
      } else {
        if (!vt.contains(n)) return;
      }
      y += tmt.get(n & 0xFFF, 2);
      int p = tmt.get(n & 0xFFF, 1);
      int pm = ((p * T2R >> 8) - mm.v) % T2R;
      int x = Math.abs((T2R >> 1) - pm) << 1;
      tb.setLocation(24 + x - 8, y);
      if (ctrl && tb.intersects(nb)) {
        ctrl = false;
        sound(2);
        nn.put(-1, new Rectangle(nb.x - 6, nb.y + 10, mm.v, 0)); // 17,28 -> 29,18
      }
      draw(tb, 9, (p - mm.v) >> 2 & 1);
      drawAttr(tb, 10 + tmt.get(n & 0xFFF, 0));
    });
    //tm(cx.v - 270, H + 32, (y, n) -> {
    tm((cx.v - 270 + H/2) * 16, (H + 32) * 16, -H / 2 * 16, H / 2 * 16, (y, n) -> { y /= 16; y += H/2;
      if (n < 0) return;
      y += tme.get(n & 0xFFF, 2);
      if (nn.containsKey(n)) {
        Rectangle r = nn.get(n);
        r.height = mm.v; // access
        nn.put(n, r);
        return;
      }
      int p = (tme.get(n & 0xFFF, 1) - mm.v) & 0x7F;
      int x = 64 + tme.get(n & 0xFFF, 3) + sin32[p];
      eb.setLocation(x - 8, y);
      if (ctrl && eb.intersects(nb)) {
        if (nn.get(n) == null) {
          sound(1);
          score++;
          tx.s++;
          nn.put(n, new Rectangle(eb.x - 6, eb.y + cx.v + 6, mm.v, mm.v)); // 16,31 -> 29,18
        }
      } else {
        draw(eb, 10, p >> 2 & 3);
        drawAttr(eb, 11 + tme.get(n & 0xFFF, 0));
      }
    });
    nn.values().forEach(n -> {
      int p = (n.width - mm.v) >> 2;
      if (p < 4) draw(11, n.x, n.y - (n.height == 0 ? 0 : cx.v), 0, p);
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
        tx.v = 0;
        tx.s = 16;
        ctrl = true;
        break;
      case 2:
        cx.s = 0;
        if (hp > 0) { hp--; }
        break;
      case 3:
        sound(0);
        cx.v = 0;
        cx.s = 1;
        mm.v = 68;
        if (hiscore + score > 0) {
          tm.random();
          tme.random();
          tmt.random();
        }
        break;
      case 4:
        hp = 3;
        if (hiscore < score) { hiscore = score; }
        score = 0;
        mm.v = 16;
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
    if (gotoState >= 0) { init(gotoState); gotoState = -1; }
    Arrays.stream(oscs).forEach(Osc::inc);
    cls();
    switch (state) {
      case 0:
        drawField();
        draw(4, 24, 16, 0, 0);
        drawAttr(32, 16, 200, 72, 8);
        print("controls:", W/2, 112, 1);
        print("{ left: o  -  right: p }", W/2, 120, 1);
        print("space to start", W/2, 144, 1);
        zxm.guessInkColorFrom(image, true);
        break;
      case 1:
        nb.setLocation(nx.v, 72 - sin32[(-mm.v) & 0x7F]);
        drawAttr(nb, 15);
        if (ctrl && !(nx.v < 219 && nx.v > 20)) {
          ctrl = false;
          sound(2);
          nn.put(-1, new Rectangle(nb.x - 6, nb.y + 10, mm.v, 0));
        }
        drawScore();
        drawField();
        drawLife();
        if (ctrl) {
          draw(nb, 8, -mm.v >> 2 & 1);
        } else {
          int i = nn.get(-1).width - mm.v;
          if (i > 32) gotoState = 2;
        }
        break;
      case 2:
        drawScore();
        drawField();
        if (hp == 0) {
          print("g a m e    o v e r", W / 2, 88, 4);
        }
        drawLife();
        break;
      case 3:
        drawScore();
        drawField();
        if (mm.v < 59) {
          draw(12, 18, 139 - cx.v, 0, mm.v < 49 ? 1 : 0);
        }
        if (mm.v < 42) {
          draw(7, 143 - 3 * mm.v, 76 + mm.v - (mm.v / 10), 0, 0);
        }
        if (mm.v < 28) {
          draw(13, 100 - 3 * mm.v, 76 + mm.v - (mm.v / 10), 0, 0);
        }
        drawAttr(40, 139 - cx.v, 128, 31, 15);
        drawAttr(24, 139 - cx.v, 16, 22, 6);
        if (mm.v <= 0) {
          gotoState = 1;
        }
        break;
      case 4:
        drawField();
        draw(4, 24, 16, 0, 0);
        drawAttr(32, 16, 200, 72, 8);
        for (int i = 0; i < (17 - mm.v) >> 1; i++) {
          for (int y = 16; y < 16 + 72; y++) {
            for (int x = 32 + i - (y & 7), j = 0; j < 200; x += 8, j += 8) {
              image.setRGB(x, y, 0);
              zxm.pixel.setRGB(x, y, 0);
            }
          }
        }
        zxm.guessInkColorFrom(image, true);
        if (mm.v <= 0) {
          gotoState = 3;
        }
        break;
    }
    //screen.image.createGraphics().drawImage(image, 0, 0, null);
    zxm.draw(screen.image);
  }

  @Override
  public void keyTyped(KeyEvent e) {
    GraphicsMode newMode;
    switch (e.getKeyChar()) {
      case 0x20:
        if (state == 0) { gotoState = 4; }
        if (state == 2) { gotoState = hp > 0 ? 3 : 0; }
        break;
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_O:
        if (ctrl && nx.s != 4) nx.s = -4; break;
      case KeyEvent.VK_P:
        if (ctrl && nx.s != -4) nx.s = 4; break;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_O:
        if (ctrl && nx.s == -4) nx.s = -2; break;
      case KeyEvent.VK_P:
        if (ctrl && nx.s == 4) nx.s = 2; break;
    }
  }

  public static void main(String[] args) throws Exception {
    Screen screen = new Screen(GraphicsMode.ZX);
    Runnable basicProgram = new NohzDyve(screen);
    int nano = Instant.now().getNano();
    while (true) {
      basicProgram.run();
      screen.repaint();
      int now = Instant.now().getNano();
      if (now - nano < 20_000_000) { // don't sync if late
        try {
          throttling = 20 - now / 1_000_000 % 20;
          Thread.sleep(throttling); // nano sync 1 - 20
          now = ((now / 20_000_000) + 1) * 20_000_000; // Instant.now().getNano();
        } catch (InterruptedException e) {
          break;
        }
      } else {
        throttling = 0;
      }
      nano = now;
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
