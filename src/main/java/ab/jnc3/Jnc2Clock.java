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
import java.time.LocalTime;

public class Jnc2Clock implements BasicApp {

  private Basic basic;
  int centerx;
  int centery;
  double radius;
  double rx;
  double ry;

  void drawThickLine(int x1, int y1, int x2, int y2, int stroke) {
    int smin = - ((stroke - 1) / 2);
    int smax = stroke + smin;
    for (int y = smin; y < smax; y++) {
      for (int x = smin; x < smax; x++) {
        basic.plot(x + x1, y + y1);
        basic.draw(x + x2, y + y2);
      }
    }
  }

  void drawHand(double value, double length, int stroke) {
    double angle = 2 * Math.PI * value / 60;
    drawThickLine(centerx, centery,
        (int) Math.round(Math.sin(angle) * rx * length + centerx),
        (int) Math.round(Math.cos(angle) * ry * length + centery), stroke);
  }

  @Override
  public GraphicsMode preferredMode() {
    return null;
  }

  @Override
  public void open(Basic basic) {
    this.basic = basic;
    Dimension size = basic.getSize();
    Dimension displayAspectRatio = basic.getDisplayAspectRatio();
    centerx = size.width / 2;
    centery = size.height / 2;
    double pixelAspectRatio = (double) (size.height * displayAspectRatio.width) / (size.width * displayAspectRatio.height);

    radius = size.height / pixelAspectRatio / 2;
    rx = Math.min(radius, radius / pixelAspectRatio);
    ry = Math.min(radius, radius * pixelAspectRatio);
    while (true) {
      basic.cls();
      basic.circle(centerx, centery, (int) Math.round(radius * 0.95));
      LocalTime now = LocalTime.now();
      drawHand((now.getHour() * 60 + now.getMinute()) / 60.0, 0.4, 2);
      drawHand((now.getMinute() * 60 + now.getSecond()) / 60.0, 0.8, 2);
      drawHand(now.getSecond(), 0.9, 1);
      basic.update();
      basic.pause(200);
    }
  }

  @Override
  public void run() {}

  @Override
  public void close() {} // dumb app that ignores close() method and expects that basic will force-close it
}
