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

public class AmigaBall implements Runnable {

  Basic basic;
  int centerx;
  int centery;
  double radius;
  double rx;
  double ry;
  int red = 10;
  int white = 15;
  int gray = 7;
  int purple = 3;

  public AmigaBall(Basic basic) {
    this.basic = basic;
    centerx = basic.getWidth() / 2;
    centery = basic.getHeight() / 2;
    radius = basic.getHeight() * basic.getPixelHeight() / 2;
    rx = Math.min(radius, radius * basic.getPixelHeight());
    ry = Math.min(radius, radius / basic.getPixelHeight());
  }

  void drawBall(int ballx, int bally, double ballAngle) {
    double ipd = 4; // image plane distance
    double bd = 200; // ball distance
    double br = 20; // ball radius
    double rotate1 = Math.PI / 8 * 5;
    double rotate2 = Math.PI / 2;
    double r1c = Math.cos(rotate1);
    double r1s = Math.sin(rotate1);
    double r2c = Math.cos(rotate2);
    double r2s = Math.sin(rotate2);
    double r3c = Math.cos(ballAngle);
    double r3s = Math.sin(ballAngle);
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
        double uy1 = uy * r1c - uz * r1s;
        double uz1 = uy * r1s + uz * r1c;
        double ux2 = ux * r2c - uy1 * r2s;
        double uy2 = ux * r2s + uy1 * r2c;
        double uy3 = uy2 * r3c - uz1 * r3s;
        double uz3 = uy2 * r3s + uz1 * r3c;
        // unit vector back to angles
        double na = Math.atan2(uz3, uy3);
        double nb = Math.atan2(Math.sqrt((uz3 * uz3) + (uy3 * uy3)), ux2);
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
    int second100 = now.getSecond() * 100 + now.getNano() / 10_000_000;
    int delayx = 500;
    double oscx = Math.abs((second100 % delayx) / (double) delayx - 0.5) * 2;
    basic.paper(gray);
    basic.cls();
    basic.ink(purple);
    int gridy = (int) Math.floor(ry * 2 * 9 / 150);
    int gridy0 = basic.getHeight() - gridy * 15 - 1;
    int gridy1 = basic.getHeight() - 1;
    int gridx = (int) Math.floor(rx * 2 * 9 / 150);
    int gridx0 = (basic.getWidth() - gridx * 15) / 2;
    int gridx1 = basic.getWidth() - gridx0 - 1;
    int gridw0 = basic.getWidth() * 9 / 10;
    int gridw1 = (basic.getWidth() - gridw0) / 2;
    for (int i = 0; i < 16; i++) {
      int y = i * gridy + gridy0;
      int x = i * gridx + gridx0;
      basic.plot(gridx0, y);
      basic.draw(gridx1, y);
      basic.plot(x, gridy1);
      basic.draw(x, gridy0);
      basic.draw(i * gridw0 / 15 + gridw1, 0);
    }
    for (int y = gridy0; y > 0; y -= gridy / 2) {
      int x = y * (gridx0 - gridw1) / gridy0 + gridw1;
      basic.plot(x, y);
      basic.draw(basic.getWidth() - x, y);
    }
    drawBall(centerx / 2 + (int) Math.round(oscx * centerx),
        centery / 2 + (int) Math.round(Math.abs(Math.sin(Math.PI * second100 / 180)) * centery * 3 / 4),
        Math.PI * second100 / 150);
  }

}
