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
   */
  public void printAt(int x, int y, String s) {
    textFont.print(screen.image, s, x * 8, cy - 7 - y * 8, color, paper);
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
