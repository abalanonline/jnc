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

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

public class Triis implements BasicApp {

  public static final int[][] PIECES_OLD = new int[][]{{0x72, 0x232, 0x270, 0x262}, {0x71, 0x322, 0x470, 0x226},
      {0x63, 0x132}, {0x66}, {0x36, 0x231}, {0x74, 0x223, 0x170, 0x622}, {0xF0, 0x2222}};

  public static final int[][] PIECES = new int[][]{{0x62, 0x64, 0x46, 0x26}, {0x70, 0x222}};

  Basic basic;
  Point fieldXy;
  String block;
  String empty;
  String clear;
  int[][] field;
  int x;
  int y;
  int n;
  int r;
  boolean wide;

  @Override
  public GraphicsMode preferredMode() {
    return GraphicsMode.ZX;
  }

  void drawScreen() {
    int x2 = wide ? 2 : 1;
    for (int i = 0; i < 20; i++) {
      basic.printAt(fieldXy.x - 1 * x2, fieldXy.y + i, block);
      basic.printAt(fieldXy.x + 10 * x2, fieldXy.y + i, block);
    }
    for (int i = -1; i < 11; i++) {
      basic.printAt(fieldXy.x + i * x2, fieldXy.y + 20, block);
    }
  }

  boolean isPiece(int x, int y) {
    x -= this.x;
    y -= this.y;
    if (x < 0 || x >= 4 || y < 0 || y >= 4) return false;
    return (PIECES[n][(r & 3) % PIECES[n].length] << y * 4 + x & 0x8000) != 0;
  }

  void drawField() {
    int x2 = wide ? 2 : 1;
    for (int y = 0; y < 20; y++) {
      for (int x = 0; x < 10; x++) {
        if (field[y][10] > 0) {
          basic.printAt(fieldXy.x + x * x2, fieldXy.y + y, clear);
          continue;
        }
        basic.printAt(fieldXy.x + x * x2, fieldXy.y + y, field[y][x] == 0 ? empty : block);
        if (isPiece(x, y)) {
          basic.printAt(fieldXy.x + x * x2, fieldXy.y + y, block);
        }
      }
    }
  }

  private boolean valid() {
    for (int y = Math.max(0, this.y); y < Math.min(this.y + 4, 20); y++) {
      for (int x = Math.max(0, this.x); x < Math.min(this.x + 4, 10); x++) {
        if (isPiece(x, y) && field[y][x] != 0) return false;
      }
    }
    for (int y = 0; y < 20; y++) if (isPiece(-1, y) || isPiece(10, y)) return false;
    for (int x = -1; x < 11; x++) if (isPiece(x, 20)) return false;
    return true;
  }

  private boolean move(int x, int y, int r) {
    this.x += x;
    this.y += y;
    this.r += r;
    if (valid()) return true;
    this.x -= x;
    this.y -= y;
    this.r -= r;
    return false;
  }

  void newPiece() {
    x = 3;
    y = -2;
    n = ThreadLocalRandom.current().nextInt(PIECES.length);
    r = 0;
  }

  void reset() {
    field = new int[20][11];
    newPiece();
  }

  @Override
  public void open(Basic basic) {
    this.basic = basic;
    Dimension size = basic.getTextSize();
    wide = size.width > size.height * 2;
    block = wide ? "[]" : "#";
    empty = wide ? "  " : " ";
    clear = wide ? "==" : "=";
    fieldXy = new Point((size.width - (wide ? 20 : 10)) / 2, (size.height - 20) / 2);
    drawScreen();
    reset();
    long nanoTime = System.nanoTime();
    int pauseNs = 200_000_000;
    boolean remove = false;
    boolean stop = false;
    while (true) {
      String key = basic.inkey(1);
      boolean update = false;
      if (key == null) {
        if (System.nanoTime() > nanoTime && !stop) {
          update = true;
          nanoTime += pauseNs;
          if (remove) {
            for (int y = 19, yc = 19; y >= 0; y--, yc--) {
              while (yc >= 0 && field[yc][10] > 0) yc--;
              for (int x = 0; x < 10; x++) field[y][x] = yc < 0 ? 0 : field[yc][x];
              field[y][10] = 0;
            }
            remove = false;
          }
          boolean move = move(0, 1, 0);
          if (!move) {
            for (int y = 0; y < 20; y++) {
              boolean full = true;
              for (int x = 0; x < 10; x++) {
                if (isPiece(x, y)) field[y][x] = n + 1;
                if (field[y][x] == 0) full = false;
              }
              if (full) {
                field[y][10] = 1;
                remove = true;
              } else if (this.y == -2) {
                stop = true;
              }
            }
            newPiece();
            if (remove) nanoTime += pauseNs * 4;
            update = true;
          }
        }
      } else {
        switch (key) {
          case "Left": if (move(-1, 0, 0)) update = true; break;
          case "Right": if (move(1, 0, 0)) update = true; break;
          case "Up": if (move(0, 0, 1)) update = true; break;
          case "Down":
            for (boolean b = true; b; ) {
              b = move(0, 1, 0);
              if (b) {
                update = true;
                nanoTime = System.nanoTime() + pauseNs;
              }
            }
            break;
        }
        if (stop) {
          reset();
          nanoTime = System.nanoTime();
          stop = false;
        }
      }
      if (update) {
        drawField();
        basic.update();
      }
    }
  }

  @Override
  public void run() {

  }

  @Override
  public void close() {

  }

  public static void main(String[] args) {
    Screen screen = new Screen();
    Basic basic = new Basic3(screen);
    basic.load(new Triis());
    screen.close();
  }
}
