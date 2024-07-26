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

public class Basic3 implements Basic {

  public static final GraphicsMode DEFAULT_MODE = GraphicsMode.ZX;
  private final Screen screen;
  private GraphicsMode mode;
  private int paper;
  private int color;
  private int x = 0;
  private int y = 0;
  private double pixelHeight;
  private int ymax = 0;

  public Basic3(Screen screen) {
    this.screen = screen;
  }

  @Override
  public int load(BasicApp app) {
    GraphicsMode mode = app.preferredMode();
    if (mode != null) this.mode = mode;
    if (this.mode == null) this.mode = DEFAULT_MODE;
    setMode();
    app.open(this);
    app.run();
    return 0;
  }

  private void setMode() {
    Dimension r = mode.resolution;
    IndexColorModel colorModel = mode.colorMap == null ? null : new IndexColorModel(
        8, mode.colorMap.length, mode.colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);
    screen.image = colorModel == null
        ? new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB)
        : new BufferedImage(r.width, r.height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    paper = mode.getRgbColor(mode.bgColor);
    color = mode.getRgbColor(mode.fgColor);
    pixelHeight = (double) (r.width * mode.aspectRatio.height) / (r.height * mode.aspectRatio.width);
    ymax = r.height - 1;
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
    screen.image.setRGB(x, ymax - y, color);
    this.x = x;
    this.y = y;
  }

  @Override
  public void draw(int x, int y) {
    Graphics2D graphics = screen.image.createGraphics();
    graphics.setColor(new Color(color));
    graphics.draw(new Line2D.Double(this.x, ymax - this.y, x, ymax - y));
    this.x = x;
    this.y = y;
  }

  @Override
  public void circle(int x, int y, int r) {
    double rx = Math.min(r * pixelHeight, r);
    double ry = Math.min(r / pixelHeight, r);
    Graphics2D graphics = this.screen.image.createGraphics();
    graphics.setColor(new Color(color));
    graphics.draw(new Ellipse2D.Double(x - rx, ymax - y - ry, rx + rx, ry + ry));
  }

  @Override
  public void cls() {
    Graphics2D graphics = screen.image.createGraphics();
    this.screen.setBackground(paper);
    graphics.setBackground(new Color(paper));
    graphics.clearRect(0, 0, mode.resolution.width, mode.resolution.height);
  }

  @Override
  public void update() {
    screen.update();
  }
}
