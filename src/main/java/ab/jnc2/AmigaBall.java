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
import java.util.concurrent.atomic.AtomicReference;

public class AmigaBall implements Runnable {

  Basic basic;
  int centerx;
  int centery;
  double radius;
  double rx;
  double ry;
  int red = 10;
  int white = 15;

  public AmigaBall(Basic basic) {
    this.basic = basic;
    centerx = basic.getWidth() / 2;
    centery = basic.getHeight() / 2;
    radius = basic.getHeight() * basic.getPixelHeight() / 2;
    rx = Math.min(radius, radius * basic.getPixelHeight());
    ry = Math.min(radius, radius / basic.getPixelHeight());
  }

  void rotate(AtomicReference<Double> x, AtomicReference<Double> y, double angle) {
    double cos = Math.cos(angle);
    double sin = Math.sin(angle);
    double nx = x.get() * cos - y.get() * sin;
    double ny = x.get() * sin + y.get() * cos;
    x.set(nx);
    y.set(ny);
  }

  void drawBall(int ballx, int bally, double ballAngle) {
    double ipd = 4; // image plane distance
    double bd = 200; // ball distance
    double br = 20; // ball radius
    for (int y = 0; y < centery * 2; y++) {
      for (int x = 0; x < centerx * 2; x++) {
        double dx = (x - ballx) / rx;
        double dy = (y - bally) / ry;
        double aa = Math.atan2(dy, dx); // angle a of the ball
        dx = Math.sqrt((dx * dx) + (dy * dy)); // rotate and simplify to 2d task
        double ray = Math.atan2(dx, ipd); // angle 0-pi/2 between the center and the casting ray
        double h = Math.abs(bd * Math.sin(ray)); // height of the triangle: distance to ball, ball radius, casting ray
        if (h > br) continue;
        double ac = Math.acos(h / br); // c is the angle 0-pi/2 between ray and the ball surface
        double ab = Math.PI / 2 - ray - ac; // angle b of the ball, the second
        // transform two angles to the unit vector
        double ux = Math.cos(ab);
        double uy = Math.sin(ab) * Math.cos(aa);
        double uz = Math.sin(ab) * Math.sin(aa);
        // rotate vector
        AtomicReference<Double> ax = new AtomicReference<>(ux);
        AtomicReference<Double> ay = new AtomicReference<>(uy);
        AtomicReference<Double> az = new AtomicReference<>(uz);
        rotate(ay, az, Math.PI / 8 * 5);
        rotate(ax, ay, Math.PI / 2);
        rotate(ay, az, ballAngle);
        ux = ax.get();
        uy = ay.get();
        uz = az.get();
        // unit vector back to angles
        double na = Math.atan2(uz, uy);
        double nb = Math.atan2(Math.sqrt((uz * uz) + (uy * uy)), ux);
        boolean b1 = (int) (na / Math.PI * 8 + 32) % 2 == 0;
        boolean b2 = (int) (nb / Math.PI * 8 + 32) % 2 == 0;
        basic.ink(b1 ^ b2 ? red : white);
        basic.plot(x, y);
      }
    }
  }

  @Override
  public void run() {
    LocalTime now = LocalTime.now();
    basic.cls();
    drawBall(centerx, centery, Math.PI * (now.getSecond() * 100 + now.getNano() / 10_000_000) / 150);
  }

}
