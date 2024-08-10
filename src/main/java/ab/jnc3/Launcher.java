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

  private static final BasicApp[] APPS = {new Triis(), new Jnc2Clock(), new BasicClock()};
  private final int[] CM = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
  Basic basic;
  boolean stop;

  @Override
  public TextMode preferredMode() {
    TextMode mode = TextMode.zx();
    BitmapFont font = mode.font;
    System.arraycopy(
        new byte[]{(byte) 0xFE, (byte) 0xFC, (byte) 0xF8, (byte) 0xF0, (byte) 0xE0, (byte) 0xC0, (byte) 0x80, 0}, 0,
        font.bitmap, 0xDD * 8, 8);
    System.arraycopy(new byte[]{0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, (byte) 0xFF}, 0, font.bitmap, 0xDE * 8, 8);
    byte x80 = (byte) 0x80;
    System.arraycopy(new byte[]{x80, x80, x80, x80, x80, x80, x80, x80}, 0, font.bitmap, 0xD0 * 8, 8);
    System.arraycopy(new byte[]{1, 1, 1, 1, 1, 1, 1, 1}, 0, font.bitmap, 0xD1 * 8, 8);
    System.arraycopy(new byte[]{x80, x80, x80, x80, x80, x80, x80, -1}, 0, font.bitmap, 0xC0 * 8, 8);
    System.arraycopy(new byte[]{1, 1, 1, 1, 1, 1, 1, -1}, 0, font.bitmap, 0xD9 * 8, 8);
    System.arraycopy(new byte[]{0, 0, 0, 0, 0, 0, 0, -1}, 0, font.bitmap, 0xC4 * 8, 8);
    font.put('\u25E4', 0xDD);
    font.put('\u25E2', 0xDE);
    font.put('\u258F', 0xD0);
    font.put('\u2595', 0xD1);
    font.put('\u2514', 0xC0);
    font.put('\u2518', 0xD9);
    font.put('\u2500', 0xC4);
    font.put('\u00A9', 0x7F); // (c)
    font.cacheBitmap();
    return mode;
  }

  private void attr(int paper, int ink) {
    basic.paper(CM[paper]);
    basic.ink(CM[ink]);
  }

  private void slashes(int x, int y) {
    String s = "\u25E2\u25E4\u25E2\u25E4\u25E2 ";
    int[] colors = new int[]{0, 10, 14, 10, 14, 12, 13, 12, 13, 0, 8, 8};
    for (int i = 0; i < colors.length / 2; i++) {
      attr(colors[i * 2], colors[i * 2 + 1]);
      basic.printAt(x + i, y, s.substring(i, i + 1));
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
      basic.printAt(left - 1, y, "\u258F");
      String item = i < items.size() ? items.get(i) : "";
      basic.printAt(left, y, item);
      for (int x = left + item.length(); x < xm; x++) basic.printAt(x, y, " ");
      basic.printAt(xm, y, "\u2595");
    }
    attr(15, 0);
    int y = top + height;
    basic.printAt(left - 1, y, "\u2514");
    for (int x = left; x < xm; x++) basic.printAt(x, y, "\u2500");
    basic.printAt(xm, y, "\u2518");
    attr(7, 0);
    String[] f = footer.split("\r?\n");
    for (int i = 0; i < f.length; i++) {
      basic.printAt(0, textSize.height - f.length + i, f[i]);
    }
  }

  @Override
  public void open(Basic basic) {
    this.basic = basic;
    for (int i = 0; i < 16; i++) {
      int c = 0;
      if ((i & 1) != 0) c += 0x0000AA;
      if ((i & 2) != 0) c += 0xAA0000;
      if ((i & 4) != 0) c += 0x00AA00;
      if ((i & 8) != 0) c += 0x555555;
      CM[i] = basic.getColorFromRgb(c);
    }
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
      menu("Loader", "(C) 2024 GPLv3 JNC3", list, cursor);
      basic.update();
      String s = basic.inkey();
      if ("Alt+Right".equals(s)) padding.width++;
      if ("Alt+Left".equals(s)) padding.width--;
      if ("Alt+Down".equals(s)) padding.height++;
      if ("Alt+Up".equals(s)) padding.height--;
      if ("Down".equals(s)) cursor = (cursor + 1) % appList.size();
      if ("Up".equals(s)) cursor = cursor > 0 ? cursor - 1 : appList.size() - 1;
      if ("q".equals(s) || "Q".equals(s) || "Close".equals(s) || "Esc".equals(s)) {
        stop = true;
        break;
      }
      if ("Enter".equals(s)) {
        basic.load(APPS[cursor]);
        break; // needs to exit to restart
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
