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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Launcher implements BasicApp {

  private static final BasicApp[] APPS = {new Triis(), new Jnc2Clock(), new BasicClock(), new CubicGrid3(), new Help()};
  public static final char LOWER_RIGHT_TRIANGLE = '\u25E2';
  public static final char UPPER_LEFT_TRIANGLE = '\u25E4';
  char slashLeft = LOWER_RIGHT_TRIANGLE;
  char slashRight = UPPER_LEFT_TRIANGLE;
  // TODO: 2024-08-11 do not modify these fields until unicode provides Left / Right and Lower One Quarter Block
  char frameLeft = '\u2551';
  char frameDownLeft = '\u255A';
  char frameDown = '\u2550';
  char frameDownRight = '\u255D';
  char frameRight = '\u2562';
  private final int[] CM = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
  Basic basic;
  boolean stop;
  private Screen screen; // FIXME: 2024-12-29 test, delete this

  @Override
  public TextMode preferredMode() {
    return TextMode.zx();
  }

  private void attr(int paper, int ink) {
    basic.paper(CM[paper]);
    basic.ink(CM[ink]);
  }

  private void slashes(int x, int y) {
    int[] colors = new int[]{0, 10, 14, 10, 14, 12, 13, 12, 13, 0, 0, 8};
    for (int i = 0; i < colors.length / 2; i++) {
      attr(colors[i * 2], colors[i * 2 + 1]);
      basic.printAt(x + i, y, i < 5 ? Character.toString(i % 2 == 0 ? slashLeft : slashRight) : " ");
    }
  }

  public void calculator() {
    attr(8, 8);
    for (int i = 0; i < 26; i++) basic.printAt(i, 21, " ");
    slashes(26, 21);

    attr(0, 15);
    basic.printAt(0, 21, "Calculator");

    attr(7, 0);
    basic.printAt(0, 0, "1-1");
    basic.printAt(0, 1, "0");
    basic.printAt(0, 23, "0 OK, 0:0");
  }

  public void menu(String title, String footer, List<String> items, int selected) {
    Dimension textSize = basic.getTextSize();
    int width = Math.max(12, items.stream().mapToInt(String::length).max().orElse(0) + 1);
    int left = ((textSize.width - width - 2) * 7 + 10) / 18 + 1;
    int height = Math.max(4, items.size());
    int top = ((textSize.height - height - 3) * 7 + 9) / 17 + 1;
    int xm = left + width;
    attr(0, 15);
    basic.printAt(left - 1, top - 1, title);
    for (int i = left - 1 + title.length(); i < xm - 5; i++) basic.printAt(i, top - 1, " ");
    slashes(xm - 5, top - 1);

    for (int i = 0; i < height; i++) {
      int y = top + i;
      attr(i == selected ? 13 : 15, 0);
      basic.printAt(left - 1, y, Character.toString(frameLeft));
      String item = i < items.size() ? items.get(i) : "";
      basic.printAt(left, y, item);
      for (int x = left + item.length(); x < xm; x++) basic.printAt(x, y, " ");
      basic.printAt(xm, y, Character.toString(frameRight));
    }
    attr(15, 0);
    int y = top + height;
    basic.printAt(left - 1, y, Character.toString(frameDownLeft));
    for (int x = left; x < xm; x++) basic.printAt(x, y, Character.toString(frameDown));
    basic.printAt(xm, y, Character.toString(frameDownRight));
    attr(7, 0);
    String[] f = footer.split("\r?\n");
    for (int i = 0; i < f.length; i++) {
      basic.printAt(0, textSize.height - f.length + i, f[i]);
    }
  }

  private char displayableChar(char... chars) {
    for (char c : chars) if (basic.canDisplay(c)) return c;
    return chars[chars.length - 1];
  }

  @Override
  public void open(Basic basic) {
    this.basic = basic;
    slashLeft = displayableChar(LOWER_RIGHT_TRIANGLE, '\u2592');
    slashRight = displayableChar(UPPER_LEFT_TRIANGLE, '\u2591');
    for (int i = 0; i < 16; i++) CM[i] =
        basic.getColorFromRgb((i & 1 | i << 15 & 0x10000 | i << 6 & 0x100) * (i < 8 ? 0xC0 : 0xFF));
    List<String> appList = Arrays.stream(APPS).map(a -> a.getClass().getSimpleName()).collect(Collectors.toList());
    Dimension padding = new Dimension();
    int cursor = 0;
    while (true) {
      attr(7, 0);
      basic.cls();
      ArrayList<String> list = new ArrayList<>(appList);
      String s0 = list.remove(0);
      for (int i = 0; i < padding.width; i++) s0 = " " + s0;
      list.add(0, s0);
      for (int i = 0; i < padding.height; i++) list.add(0, "");
      menu("Loader", "(C) 2024 GPLv3 JNC3", list, cursor + padding.height);
      basic.update();
      String s = basic.inkey();
      if ("Alt+Right".equals(s)) padding.width++;
      if ("Alt+Left".equals(s)) padding.width--;
      if ("Alt+Down".equals(s)) padding.height++;
      if ("Alt+Up".equals(s)) padding.height--;
      if ("Down".equals(s)) cursor = (cursor + 1) % appList.size();
      if ("Up".equals(s)) cursor = cursor > 0 ? cursor - 1 : appList.size() - 1;
      if ("p".equals(s) && screen != null) screen.enablePointer();
      if ("q".equals(s) || "Q".equals(s) || "Close".equals(s) || "Esc".equals(s)) {
        stop = true;
        break;
      }
      int basicLoad = 0;
      if ("Enter".equals(s)) basicLoad = cursor + 1;
      int i = s.length() == 1 ? s.charAt(0) - '0' : 0;
      if (i > 0 && i <= APPS.length) basicLoad = i;
      if (basicLoad > 0) {
        basic.load(APPS[basicLoad - 1]);
        stop = false;
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
    launcher.screen = screen;
    while (!launcher.stop) basic.load(launcher);
    screen.close();
  }
}
