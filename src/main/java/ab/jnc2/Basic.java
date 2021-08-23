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
  private int color = Color.WHITE.getRGB();
  private int x = 0;
  private int y = 0;

  public Basic(Screen screen) {
    this.screen = screen;
  }

  public void paper(int color) {

  }

  public void ink(int color) { // bright, flash, inverse, over
    int[] colorMap = screen.mode.colorMap;
    if (colorMap != null) {
      color = color % colorMap.length;
      color = colorMap[color];
    }
    this.color = color | 0xFF000000;
  }

  public void circle(int x, int y, int r) {
    Dimension screen = this.screen.mode.aspectRatio;
    Dimension resolution = this.screen.mode.resolution;
    double pixelHeight = ((double) resolution.width / screen.width) / ((double) resolution.height / screen.height);
    double rx = Math.min(r * pixelHeight, r);
    double ry = Math.min(r / pixelHeight, r);
    Graphics2D graphics = this.screen.image.createGraphics();
    graphics.setColor(new Color(color));
    graphics.draw(new Ellipse2D.Double(x - rx, y - ry, rx + rx, ry + ry));
  }

  public void plot(int x, int y) {
    screen.image.setRGB(x, y, color);
    this.x = x;
    this.y = y;
  }

  public void draw(int x, int y) {
    Graphics2D graphics = this.screen.image.createGraphics();
    graphics.setColor(new Color(color));
    graphics.draw(new Line2D.Double(this.x, this.y, x, y));
    this.x = x;
    this.y = y;
  }

  public void printAt(int x, int y, String s) {
  }

  public void border(int color) {
  }

  public void cls() {
  }

}
