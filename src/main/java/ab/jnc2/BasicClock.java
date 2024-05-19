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

import java.time.LocalTime;

public class BasicClock implements Runnable {

  Basic basic;
  int centerx;
  int centery;
  double radius;
  double rx;
  double ry;

  public BasicClock(Basic basic) {
    this.basic = basic;
    centerx = basic.getWidth() / 2;
    centery = basic.getHeight() / 2;
    radius = basic.getHeight() * basic.getPixelHeight() / 2;
    rx = Math.min(radius, radius * basic.getPixelHeight());
    ry = Math.min(radius, radius / basic.getPixelHeight());
    //basic.paper(basic.getColorFromRgb(0x999900));
  }

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
  public void run() {
    basic.cls();
    basic.circle(centerx, centery, (int) Math.round(radius * 0.95));
    LocalTime now = LocalTime.now();
    drawHand((now.getHour() * 60 + now.getMinute()) / 60.0, 0.4, 2);
    drawHand((now.getMinute() * 60 + now.getSecond()) / 60.0, 0.8, 2);
    drawHand(now.getSecond(), 0.9, 1);
  }

}
