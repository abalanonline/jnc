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

import ab.jnc2.GraphicsMode;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Basic3 implements Basic { // FIXME: 2024-07-28 delete and redesign

  public static final GraphicsMode DEFAULT_MODE = GraphicsMode.ZX;
  private final BitmapFont font;
  private final Screen screen;
  private GraphicsMode mode;
  private GraphicsMode switchMode;
  private int paper;
  private int color;
  private int x = 0;
  private int y = 0;
  private double pixelHeight;
  private int ymax = 0;
  private final BlockingQueue<String> inkey = new LinkedBlockingQueue<>();
  private BasicApp runningApp;
  private final CancellationException stop = new CancellationException();

  public Basic3(Screen screen) {
    this.screen = screen;
    screen.keyListener = this::keyListener;
    font = new BitmapFont(8, 8);
    font.bitmap = ab.jnc2.TextFont.ZX.get().font;
    font.cacheBitmap();
  }

  private void keyListener(String s) {
    boolean close = false;
    switch (s) {
      case "Close":
        Optional.ofNullable(runningApp).ifPresent(BasicApp::close);
        System.exit(0);
      case "Esc":
      case "Alt+Backspace": close = true; break;
      case "Alt+1": switchMode = GraphicsMode.ZX; break;
      case "Alt+2": switchMode = GraphicsMode.C64; break;
      case "Alt+3": switchMode = GraphicsMode.CGA_16; break;
      case "Alt+4": switchMode = GraphicsMode.CGA_HIGH; break;
      case "Alt+5": switchMode = GraphicsMode.CGA_4; break;
      case "Alt+0": switchMode = GraphicsMode.DEFAULT; break;
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
    GraphicsMode mode = app.preferredMode();
    switchMode = mode == null ? DEFAULT_MODE : mode;
    while (switchMode != null) {
      setMode(switchMode);
      switchMode = null;
      runningApp = app;
      inkey.clear();
      try {
        app.open(this);
        app.run();
      } catch (CancellationException e) {
        if (e != stop) throw e;
      }
      runningApp = null;
    }
    return 0;
  }

  private void setMode(GraphicsMode mode) {
    this.mode = mode;
    Dimension r = mode.resolution;
    IndexColorModel colorModel = mode.colorMap == null ? null : new IndexColorModel(
        8, mode.colorMap.length, mode.colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);
    screen.image = colorModel == null
        ? new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB)
        : new BufferedImage(r.width, r.height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    paper = mode.bgColor;
    color = mode.fgColor;
    pixelHeight = (double) (r.width * mode.aspectRatio.height) / (r.height * mode.aspectRatio.width);
    ymax = r.height - 1;
    cls();
  }

  @Override
  public int getWidth() {
    return screen.image.getWidth();
  }

  @Override
  public int getHeight() {
    return screen.image.getHeight();
  }

  @Override
  public double getPixelHeight() {
    return pixelHeight;
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
    double rx = Math.min(r * pixelHeight, r);
    double ry = Math.min(r / pixelHeight, r);
    Graphics2D graphics = this.screen.image.createGraphics();
    graphics.setColor(new Color(mode.getRgbColor(color)));
    graphics.draw(new Ellipse2D.Double(x - rx, ymax - y - ry, rx + rx, ry + ry));
  }

  @Override
  public void cls() {
    Graphics2D graphics = screen.image.createGraphics();
    int rgb = mode.getRgbColor(paper);
    this.screen.setBackground(rgb);
    graphics.setBackground(new Color(rgb));
    graphics.clearRect(0, 0, mode.resolution.width, mode.resolution.height);
  }

  @Override
  public void update() {
    if (runningApp == null) throw stop;
    screen.update();
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
  public void printAt(int x, int y, String s) {
    font.drawString(s, x * 8, y * 8, screen.image, mode.getRgbColor(color), mode.getRgbColor(paper));
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
}
