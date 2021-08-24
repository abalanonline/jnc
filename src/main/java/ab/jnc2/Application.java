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

public class Application {

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.ZX);
    Basic basic = screen.basic();
    for (int y = 0; y < 64; y++) {
      for (int x = 0; x < 128; x++) {
        basic.ink(x / 16 + (y < 32 ? 0 : 8));
        basic.plot(x ,y);
      }
    }
    int r = basic.getWidth() / 3;
    double rx = r;
    double ry = r;
    if (basic.getPixelHeight() > 1) {
      ry = ry / basic.getPixelHeight();
    } else {
      rx = rx * basic.getPixelHeight();
    }
    int centerx = basic.getWidth() / 2;
    int centery = basic.getHeight() / 2;
    for (int i = 0; i < 16; i++) {
      basic.ink(i);
      basic.circle(centerx, centery, r - i * 2);
      basic.plot(centerx, centery);
      basic.draw(0, i * 3);
      double angle = Math.PI * i / 8;
      basic.plot(centerx, centery);
      basic.draw((int) Math.round(Math.cos(angle) * rx + centerx), (int) Math.round(Math.sin(angle) * ry + centery));
    }
    screen.repaint();
  }

}
