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

import java.awt.*;

public interface Basic {
  /**
   * Loads a program, deleting any existing program.
   */
  int load(BasicApp app);
  Dimension getSize();
  Dimension getTextSize();
  Dimension getDisplayAspectRatio();
  // FIXME: 2024-07-25 temp copy of jnc2.Basic, delete and redesign
  double getPixelHeight();
  void plot(int x, int y);
  void draw(int x, int y);
  void circle(int x, int y, int radius);
  void cls();
  void update();
  void pause(int milliseconds);
  void paper(int color);
  void ink(int color);
  int getColorFromRgb(int rgb);
  void printAt(int x, int y, String s);
  String inkey();
  String inkey(int milliseconds);
}
