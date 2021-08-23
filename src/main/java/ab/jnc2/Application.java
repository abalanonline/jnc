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

public class Application {

  public static void main(String[] args) throws Exception {
    Screen screen = new Screen(GraphicsMode.ZX);
    Basic basic = screen.basic();
    for (int y = 0; y < 64; y++) {
      for (int x = 0; x < 128; x++) {
        basic.ink(x / 16 + (y < 32 ? 0 : 8));
        basic.plot(x ,y);
      }
    }
    Dimension resolution = screen.mode.resolution;
    int r = resolution.width / 3;
    for (int i = 0; i < 16; i++) {
      basic.ink(i);
      basic.circle(resolution.width / 2, resolution.height / 2, r - i * 2);
      basic.plot(resolution.width / 2, resolution.height / 2);
      basic.draw(0, i * 3);
    }
    screen.repaint();
  }

}
