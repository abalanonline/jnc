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
  private final double radius;
  private double rx;
  private double ry;

  public BasicClock(Basic basic) {
    this.basic = basic;
    centerx = basic.getWidth() / 2;
    centery = basic.getHeight() / 2;
    radius = basic.getHeight() * basic.getPixelHeight() / 2;
    rx = Math.min(radius, radius * basic.getPixelHeight());
    ry = Math.min(radius, radius / basic.getPixelHeight());
  }

  void drawHand(int value, double length) {
    double angle = 2 * Math.PI * value / 60;
    basic.plot(centerx, centery);
    basic.draw((int) Math.round(Math.sin(angle) * rx * length + centerx),
        (int) Math.round(Math.cos(angle) * ry * length + centery));
  }

  @Override
  public void run() {
    basic.cls();
    basic.circle(centerx, centery, (int) Math.round(radius * 0.95));
    LocalTime now = LocalTime.now();
    drawHand(now.getHour(), 0.4);
    drawHand(now.getMinute(), 0.8);
    drawHand(now.getSecond(), 0.9);
  }

}
