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
import ab.tui.Tui;
import ab.tui.TuiConsole;

import java.awt.*;

public class Launcher implements BasicApp {

  private static final BasicApp[] APPS = {new Triis(), new Jnc2Clock(), new BasicClock()};
  boolean stop;

  @Override
  public GraphicsMode preferredMode() {
    return GraphicsMode.C64;
  }

  @Override
  public void open(Basic basic) {
    Dimension textSize = basic.getTextSize();
    int w = textSize.width;
    int h = textSize.height;
    for (int y = 0; y < h; y++) for (int x = y % 2 * 2; x < w; x += 4) basic.printAt(x, y, ":");
    int mw = Math.min(w, 20);
    int mh = Math.min(h, 10);
    int mx = (w - mw) / 2;
    int my = (h - mh) / 2;
    for (int y = my; y < my + mh; y++) {
      boolean fl = y == my || y == my + mh - 1;
      basic.printAt(mx, y, fl ? "+" : "|");
      for (int x = mx + 1; x < mx + mw - 1; x++) basic.printAt(x, y, fl ? "-" : " ");
      basic.printAt(mx + mw - 1, y, fl ? "+" : "|");
    }
    basic.printAt(mx + 2, my, " JNC3 ");
    int ai = 0;
    for (BasicApp app : APPS) basic.printAt(mx + 2, my + 2 + ai,
        String.format("%d. %s", ++ai, app.getClass().getSimpleName()));
    basic.printAt(mx + 2, my + 2 + ai, "Q. Quit");
    basic.update();
    while (true) {
      String s = basic.inkey();
      if ("q".equals(s) || "Q".equals(s) || "Close".equals(s)) {
        stop = true;
        break;
      }
      int i = s.length() == 1 ? s.charAt(0) - '0' : 0;
      if (i > 0 && i <= APPS.length) {
        basic.load(APPS[i - 1]);
        break; // needs to exit to restart
      }
    }
  }

  @Override
  public void close() {}

  public static void main(String[] args) {
    Screen screen = new Screen();
    Basic basic = new Basic3(screen, null);
    Launcher launcher = new Launcher();
    while (!launcher.stop) basic.load(launcher);
    screen.close();
  }
}
