/*
 * Copyright (C) 2025 Aleksei Balan
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class ScreenTest {

  @Disabled
  @Test
  void mouseListener() throws InterruptedException {
    Screen screen = new Screen();
    int w = screen.image.getWidth();
    int h = screen.image.getHeight();
    int cx = w / 2;
    int cy = h / 2;
    int wx = cx;
    int wy = cy;
    Queue<String> keyListener = new LinkedBlockingQueue<>();
    screen.keyListener = keyListener::add;
    screen.enablePointer();
    screen.gameController = true;
    boolean open = true;
    while (open) {
      for (int x = 0; x < w; x++) screen.image.setRGB(x, cy, 0);
      for (int y = 0; y < h; y++) screen.image.setRGB(cx, y, 0);
      while (!keyListener.isEmpty()) {
        String key = keyListener.remove();
        if ("Esc".equals(key)) open = false;
        if (key.startsWith("Mouse")) {
          if (key.charAt(6) <= '9') {
            String[] xys = key.substring(5).split(",");
            cx += Integer.parseInt(xys[0]);
            cy += Integer.parseInt(xys[1]);
            wx = Integer.parseInt(xys[2]);
            wy = Integer.parseInt(xys[3]);
          }
        }
      }
      cx = cx % w; if (cx < 0) cx += w;
      cy = cy % h; if (cy < 0) cy += h;
      for (int x = 0; x < w; x++) screen.image.setRGB(x, cy, 0x55FF55);
      for (int y = 0; y < h; y++) screen.image.setRGB(cx, y, 0x55FF55);
      screen.update();
      Thread.sleep(100);
    }
    screen.close();
  }
}
