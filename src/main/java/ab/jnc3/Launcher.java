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
import java.util.List;

public class Launcher implements BasicApp {

  private static final BasicApp[] APPS = {new Triis(), new Jnc2Clock(), new BasicClock()};
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

  private static void slashes(Basic basic, int x, int y) {
    String s = "\u25E2\u25E4\u25E2\u25E4\u25E2 ";
    int[] colors = new int[]{0, 10, 14, 10, 14, 12, 13, 12, 13, 0, 8, 8};
    for (int i = 0; i < colors.length / 2; i++) {
      basic.paper(colors[i * 2]);
      basic.ink(colors[i * 2 + 1]);
      basic.printAt(x + i, y, s.substring(i, i + 1));
    }
  }

  public static void calculator(Basic basic) {
    basic.paper(8);
    basic.ink(8);
    for (int i = 0; i < 26; i++) basic.printAt(i, 21, " ");
    slashes(basic, 26, 21);

    basic.paper(0);
    basic.ink(15);
    basic.printAt(0, 21, "Calculator");

    basic.paper(7);
    basic.ink(0);
    basic.printAt(0, 0, "1-1");
    basic.printAt(0, 1, "0");
    basic.printAt(0, 23, "0 OK, 0:0");
  }

  public static void menu(Basic basic, String title, String footer, List<String> items, int selected) {
    Rectangle r = new Rectangle(8, 8, 12, items.size());
    int xm = r.x + r.width;
    basic.paper(0);
    basic.ink(15);
    basic.printAt(r.x - 1, r.y - 1, title);
    for (int i = r.x - 1 + title.length(); i < xm - 5; i++) basic.printAt(i, r.y - 1, " ");
    slashes(basic, xm - 5, r.y - 1);

    basic.ink(0);
    for (int i = 0; i < r.height; i++) {
      int y = r.y + i;
      basic.paper(i == selected ? 13 : 15);
      basic.printAt(r.x - 1, y, "\u258F");
      String item = items.get(i);
      basic.printAt(r.x, y, item);
      for (int x = r.x + item.length(); x < xm; x++) basic.printAt(x, y, " ");
      basic.printAt(xm, y, "\u2595");
    }
    basic.paper(15);
    int y = r.y + r.height;
    basic.printAt(r.x - 1, y, "\u2514");
    for (int x = r.x; x < xm; x++) basic.printAt(x, y, "\u2500");
    basic.printAt(xm, y, "\u2518");
    basic.paper(7);
    basic.ink(0);
    String[] f = footer.split("\r?\n");
    for (int i = 0; i < f.length; i++) {
      basic.printAt(0, 24 - f.length + i, f[i]);
    }
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
      basic.cls();
      menu(basic, "128", "\u00A9 1986 Sinclair Research Ltd",
          List.of("Tape Loader", "128 BASIC", "Calculator", "48 BASIC", "Tape Tester"), 0);
      basic.update();
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
