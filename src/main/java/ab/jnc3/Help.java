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

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class Help implements BasicApp {

  private final String[] help;
  private final Dimension size;
  private int page;
  private Basic basic;
  private Point start;

  public Help() {
    InputStream stream = getClass().getResourceAsStream("/jnc3/help.txt");
    byte[] bytes;
    try {
      bytes = stream.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    help = new String(bytes).split("\r?\n\r?\n");
    int w = 0;
    int h = 0;
    for (String h1 : help) {
      String[] h2 = h1.split("\r?\n");
      h = Math.max(h, h2.length);
      for (String h3 : h2) {
        w = Math.max(w, h3.length());
      }
    }
    size = new Dimension(w, h);
  }

  @Override
  public TextMode preferredMode() {
    TextMode mode = TextMode.ega();
    return new TextMode(mode.font, 320, 175, mode.colorMap, 0, 7);
  }

  public void update() {
    basic.cls();
    String[] h = help[page].split("\r?\n");
    for (int i = 0; i < h.length; i++) basic.printAt(start.x, start.y + i, h[i]);
    basic.update();
  }

  @Override
  public void open(Basic basic) {
    this.basic = basic;
    Dimension size = basic.getTextSize();
    start = new Point((size.width - this.size.width + 1) / 2, (size.height - this.size.height) / 2);
    update();
    while (true) {
      switch (basic.inkey()) {
        case "Left": case "Up": case "PageUp": page = Math.max(0, page - 1); update(); break;
        case "Right": case "Down": case "PageDown": page = Math.min(page + 1, help.length - 1); update(); break;
      }
    }
  }

  @Override
  public void close() {}

  public static void main(String[] args) {
    new Basic3(new Screen(), null).loadAndClose(new Help());
  }
}
