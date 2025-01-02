/*
 * Copyright (C) 2021 Aleksei Balan
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

package ab.jnc2;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * BASIC language drawing commands. Requires a screen to draw.
 */
public class Basic {
  private final Screen screen;
  private double pixelHeight;
  private int cy; //constant for cartesian system calculation
  private int paper;
  private int color;
  private int x = 0;
  private int y = 0;
  private TextFont textFont;

  public Basic(Screen screen) {
    this.screen = screen;
    Dimension screenRatio = screen.mode.aspectRatio;
    Dimension resolution = screen.mode.resolution;
    pixelHeight = ((double) resolution.width / screenRatio.width) / ((double) resolution.height / screenRatio.height);
    cy = screen.mode.resolution.height - 1;
    paper(screen.mode.bgColor);
    ink(screen.mode.fgColor);
    textFont = new TextFont(8, 8);
  }

  public void paper(int color) {
    this.paper = screen.mode.getRgbColor(color);
  }

  public void ink(int color) { // bright, flash, inverse, over
    this.color = screen.mode.getRgbColor(color);
  }

  public int getColorFromRgb(int rgb) {
    return screen.mode.getIndexedColor(rgb);
  }

  public void circle(int x, int y, int r) {
    double rx = Math.min(r * pixelHeight, r);
    double ry = Math.min(r / pixelHeight, r);
    Graphics2D graphics = this.screen.image.createGraphics();
    graphics.setColor(new Color(color));
    graphics.draw(new Ellipse2D.Double(x - rx, cy - y - ry, rx + rx, ry + ry));
  }

  public void plot(int x, int y) {
    screen.image.setRGB(x, cy - y, color);
    this.x = x;
    this.y = y;
  }

  public void draw(int x, int y) {
    Graphics2D graphics = this.screen.image.createGraphics();
    graphics.setColor(new Color(color));
    graphics.draw(new Line2D.Double(this.x, cy - this.y, x, cy - y));
    this.x = x;
    this.y = y;
  }

  /**
   * PRINT AT as implemented in Sinclair BASIC.
   * FIXME: 2024-07-28 PRINT and ATTR lines are numbered from the top, opposite to PLOT and DRAW
   */
  public void printAt(int x, int y, String s) {
    textFont.print(screen.image, s, x * textFont.width, cy + 1 - (y + 1) * textFont.height, color, paper);
  }

  public void border(int color) {
    this.screen.setBackground(new Color(screen.mode.getRgbColor(color)));
  }

  public void cls() {
    Graphics2D graphics = this.screen.image.createGraphics();
    Color paperColor = new Color(paper);
    this.screen.setBackground(paperColor);
    graphics.setBackground(paperColor);
    graphics.clearRect(0, 0, screen.mode.resolution.width, screen.mode.resolution.height);
  }

  public int getWidth() {
    return screen.mode.resolution.width;
  }

  public int getHeight() {
    return screen.mode.resolution.height;
  }

  public double getPixelHeight() {
    return pixelHeight;
  }

}
