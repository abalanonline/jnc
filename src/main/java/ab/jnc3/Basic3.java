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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Basic3 implements Basic { // FIXME: 2024-07-28 delete and redesign

  private final Screen screen;
  private final Tui tui;
  private TextMode mode;
  private TextMode switchMode;
  protected int paper;
  protected int color;
  private int x = 0;
  private int y = 0;
  private int ymax = 0;
  private final BlockingQueue<String> inkey = new LinkedBlockingQueue<>();
  private BasicApp runningApp;
  private final CancellationException stop = new CancellationException();
  private int[] toAnsi;

  public Basic3(Screen screen, Tui tui) {
    this.screen = screen;
    this.tui = tui;
    if (screen != null) screen.keyListener = this::keyListener;
        else if (tui != null) tui.setKeyListener(this::keyListener);
  }

  private void keyListener(String s) {
    boolean close = false;
    switch (s) {
      case "Close":
        Optional.ofNullable(runningApp).ifPresent(BasicApp::close);
        System.exit(0);
      case "Esc":
      case "Alt+Backspace": close = true; break;
      case "Alt+1": switchMode = TextMode.zx(); break;
      case "Alt+2": switchMode = TextMode.c64(); break;
      case "Alt+3": switchMode = TextMode.msx(); break;
      case "Alt+6": switchMode = TextMode.cga16(); break;
      case "Alt+7": switchMode = TextMode.cgaHigh(); break;
      case "Alt+8": switchMode = TextMode.cga4(); break;
      case "Alt+9": switchMode = TextMode.vgaHigh(); break;
      case "Alt+0": switchMode = TextMode.defaultMode(); break;
    }
    if (close || switchMode != null) {
      BasicApp app = runningApp;
      if (app != null) app.close();
      runningApp = null;
      s = "Close";
    }
    inkey.add(s);
  }

  @Override
  public int load(BasicApp app) {
    TextMode mode = app.preferredMode();
    switchMode = mode == null ? TextMode.zx() : mode;
    while (switchMode != null) {
      setMode(switchMode);
      switchMode = null;
      runningApp = app;
      inkey.clear();
      try {
        app.open(this);
      } catch (CancellationException e) {
        if (e != stop) throw e;
      } catch (NullPointerException e) {
        // TODO: 2024-08-04 throw a dedicated exception for null screen
      }
      runningApp = null;
    }
    return 0;
  }

  @Override
  public void loadAndClose(BasicApp app) {
    load(app);
    if (screen != null) screen.close();
    if (tui != null) tui.close();
  }

  private void setMode(TextMode mode) {
    this.mode = mode;
    Dimension r = mode.size;
    IndexColorModel colorModel = mode.colorMap == null ? null : new IndexColorModel(
        8, mode.colorMap.length, mode.colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);
    paper = mode.bgColor;
    color = mode.fgColor;
    ymax = r.height - 1;
    if (screen != null) screen.image = colorModel == null
        ? new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB)
        : new BufferedImage(r.width, r.height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    if (mode.colorMap != null) toAnsi = findAnsiColors(mode.colorMap);
    cls();
  }

  public static int colorDistanceSquared(int rgb0, int rgb1) {
    // https://en.wikipedia.org/wiki/Color_difference#sRGB
    int r0 = rgb0 >> 16 & 0xFF;
    int r1 = rgb1 >> 16 & 0xFF;
    int r = r0 - r1;
    int g = (rgb0 >> 8 & 0xFF) - (rgb1 >> 8 & 0xFF);
    int b = (rgb0 & 0xFF) - (rgb1 & 0xFF);
    int ri = r0 + r1; // interval [0, 510]
    return (2 * 510 + ri) * r * r + 4 * 510 * g * g + (3 * 510 - ri) * b * b;
  }

  public static int[] findAnsiColors(int[] colorMap) {
    int[] ansiMap = new int[16];
    for (int i = 0; i < 16; i++) {
      int c = 0;
      if ((i & 1) != 0) c += 0xAA0000;
      if ((i & 2) != 0) c += 0x00AA00;
      if ((i & 4) != 0) c += 0x0000AA;
      if ((i & 8) != 0) c += 0x555555;
      ansiMap[i] = c;
    }
    int n = colorMap.length;
    int[][] d = new int[n][16];
    int lost = (1 << n) - 1;
    int lostAnsi = 0xFFFF;
    for (int i0 = 0; i0 < n; i0++) {
      for (int i1 = 0; i1 < 16; i1++) d[i0][i1] = colorDistanceSquared(colorMap[i0], ansiMap[i1]);
    }
    int[] result = new int[n];
    while (lost > 0) {
      for (int c = 0; c < n; c++) {
        if ((lost & 1 << c) == 0) continue;
        int error = Integer.MAX_VALUE;
        int best = 0;
        for (int a = 0; a < 16; a++) {
          if ((lostAnsi & 1 << a) == 0) continue;
          if (error > d[c][a]) {
            error = d[c][a];
            best = a;
          }
        }
        result[c] = best;
      }
      // closest best
      for (int a = 0; a < 16; a++) {
        if ((lostAnsi & 1 << a) == 0) continue;
        int error = Integer.MAX_VALUE;
        int best = 0;
        for (int c = 0; c < n; c++) {
          if ((lost & 1 << c) == 0) continue;
          if (result[c] == a && error > d[c][a]) {
            error = d[c][a];
            best = c;
          }
        }
        if (error != Integer.MAX_VALUE) {
          lost ^= 1 << best;
          lostAnsi ^= 1 << a;
        }
      }
    }
    return result;
  }

  public int getAnsiAttr(int color) {
    if (mode.colorMap != null) return toAnsi[color];
    int error = Integer.MAX_VALUE;
    int best = 0;
    for (int i = 0; i < 16; i++) {
      int c = 0;
      if ((i & 1) != 0) c += 0xAA0000;
      if ((i & 2) != 0) c += 0x00AA00;
      if ((i & 4) != 0) c += 0x0000AA;
      if ((i & 8) != 0) c += 0x555555;
      int d = colorDistanceSquared(c, color);
      if (error > d) {
        error = d;
        best = i;
      }
    }
    return best;
  }

  @Override
  public void plot(int x, int y) {
    screen.image.setRGB(x, ymax - y, mode.getRgbColor(color));
    this.x = x;
    this.y = y;
  }

  @Override
  public void draw(int x, int y) {
    Graphics2D graphics = screen.image.createGraphics();
    graphics.setColor(new Color(mode.getRgbColor(color)));
    graphics.draw(new Line2D.Double(this.x, ymax - this.y, x, ymax - y));
    this.x = x;
    this.y = y;
  }

  @Override
  public void circle(int x, int y, int r) {
    double pixelAspectRatio =
        (double) (mode.size.height * mode.aspectRatio.width) / (mode.size.width * mode.aspectRatio.height);
    double rx = Math.min(r / pixelAspectRatio, r);
    double ry = Math.min(r * pixelAspectRatio, r);
    Graphics2D graphics = this.screen.image.createGraphics();
    graphics.setColor(new Color(mode.getRgbColor(color)));
    graphics.draw(new Ellipse2D.Double(x - rx, ymax - y - ry, rx + rx, ry + ry));
  }

  @Override
  public void cls() {
    if (screen == null) {
      Dimension size = tui.getSize();
      char[] chars = new char[size.width];
      Arrays.fill(chars, ' ');
      String empty = new String(chars);
      int attr = getAnsiAttr(paper) << 4 | getAnsiAttr(color);
      for (int y = 0; y < size.height; y++) tui.print(0, y, empty, attr);
    } else {
      Graphics2D graphics = screen.image.createGraphics();
      int rgb = mode.getRgbColor(paper);
      this.screen.setBackground(rgb);
      graphics.setBackground(new Color(rgb));
      graphics.clearRect(0, 0, mode.size.width, mode.size.height);
    }
  }

  @Override
  public void update() {
    if (runningApp == null) throw stop;
    if (screen == null) tui.update(); else screen.update();
  }

  @Override
  public void pause(int milliseconds) {
    if (runningApp == null) throw stop;
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException ignore) {}
  }

  @Override
  public void paper(int color) {
    paper = color;
  }

  @Override
  public void ink(int color) {
    this.color = color;
  }

  @Override
  public int getColorFromRgb(int rgb) {
    return mode.getIndexedColor(rgb);
  }

  @Override
  public boolean canDisplay(char c) {
    return tui == null && mode.font.canDisplay(c);
  }

  @Override
  public void printAt(int x, int y, String s) {
    if (tui == null) mode.font.drawString(s, x * mode.font.width, y * mode.font.height, screen.image,
        mode.getRgbColor(color), mode.getRgbColor(paper));
    else tui.print(x, y, s, getAnsiAttr(paper) << 4 | getAnsiAttr(color));
  }

  @Override
  public String inkey() {
    if (runningApp == null) throw stop;
    try {
      return inkey.take();
    } catch (InterruptedException e) {
      return null;
    }
  }

  @Override
  public String inkey(int milliseconds) {
    if (runningApp == null) throw stop;
    try {
      return inkey.poll(milliseconds, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return null;
    }
  }

  @Override
  public Dimension getSize() {
    return new Dimension(mode.size);
  }

  @Override
  public Dimension getTextSize() {
    return tui == null ? new Dimension(mode.size.width / mode.font.width, mode.size.height / mode.font.height)
        : tui.getSize();
  }

  @Override
  public Dimension getDisplayAspectRatio() {
    return new Dimension(mode.aspectRatio);
  }
}
