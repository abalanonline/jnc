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

/**
 * BASIC language drawing commands. Requires a screen to draw.
 */
public class Basic {
  private final Screen screen;
  private int color = 0;
  private int x = 0;
  private int y = 0;

  public Basic(Screen screen) {
    this.screen = screen;
  }

  public void ink(int color) {

  }

  public void circle(int x, int y, int r) {

  }

  public void plot(int x, int y) {
    screen.image.setRGB(x, y, Color.GREEN.getRGB());
  }

  public void draw(int x, int y) {

  }

}
