/*
 * Copyright (C) 2022 Aleksei Balan
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

package ab.jnc2;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class SystemTerm implements Runnable, KeyListener {

  private final Screen screen;
  private final Tty tty;
  Process process;

  public SystemTerm(Screen screen) {
    this.screen = screen;
    this.tty = new Tty(TextFont.PICO8.get(), TextFont.ZX.get());
    tty.title = "System Terminal";
    tty.footer = "0 OK, 0:0";

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (process != null) process.destroy();
    }));
    screen.keyListener = this;
    try {
      this.process = Runtime.getRuntime().exec(
          new String[]{"bash", "--norc", "-i"},
          new String[]{"TERM=xterm", "COLUMNS=" + tty.columns, "LINES=" + tty.lines, "PS1=$", });
      new Thread(() -> this.transferFromTo(this.process.getInputStream(), tty)).start();
      new Thread(() -> this.transferFromTo(this.process.getErrorStream(), tty)).start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
    try {
      OutputStream outputStream = process.getOutputStream();
      outputStream.write(e.getKeyChar());
      outputStream.flush();
    } catch (IOException ex) {
      System.exit(0);
      throw new UncheckedIOException(ex);
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {

  }

  @Override
  public void keyReleased(KeyEvent e) {

  }

  @Override
  public void run() {
    if (!process.isAlive()) System.exit(0);
    tty.footer = "0 OK, " + screen.getWidth() + ":" + screen.getHeight();
    tty.draw(screen.image);
  }

  public void transferFromTo(InputStream inputStream, OutputStream outputStream) {
    try {
      inputStream.transferTo(outputStream);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.ZX);
    SystemTerm shell = new SystemTerm(screen);
    screen.flicker(50, shell);
  }

  public static class Tty extends OutputStream {
    protected GraphicsModeZx zxm = new GraphicsModeZx();
    private final TextFont monospaced;
    private final int mw;
    private final int mh;
    private final TextFont dialog;
    public String title = "";
    public String footer = "";
    public int footerHeight = 2;

    public final int columns;
    public final int lines;
    private final List<byte[]> plainText;
    public int x;
    public int y;

    public Tty() {
      this(TextFont.ZX.get());
    }

    public Tty(TextFont textFont) {
      this(textFont, textFont);
    }

    public Tty(TextFont monospaced, TextFont dialog) {
      this.monospaced = monospaced;
      this.mw = monospaced.width;
      this.mh = monospaced.height;
      this.dialog = dialog;
      this.columns = GraphicsModeZx.WIDTH / mw;
      this.lines = (GraphicsModeZx.HEIGHT - (footerHeight + 1) * 8) / mh;
      this.plainText = new LinkedList<>();
      for (int i = 0; i < lines; i++) {
        plainText.add(new byte[columns]);
      }
      y = lines - 1;
    }

    public String getLine(int index) {
      return new String(plainText.get(index), StandardCharsets.ISO_8859_1);
    }

    public void repaint() {
      zxm.cls(0, 7); // dark black and white

      int bottomLine = GraphicsModeZx.HEIGHT - (footerHeight + 1) * 8 - mh;
      for (int y = bottomLine, i = 0; y >= 0; y -= mh, i++) {
        monospaced.print(zxm.pixel, this.getLine(i), 0, y, -1);
      }
      if (mw == 8 && mh == 8) {
        zxm.attrRect(this.x, bottomLine / 8 - this.y, 1, 1, GraphicsModeZx.WHITE & ~8, GraphicsModeZx.BLUE);
      } else {
        monospaced.print(zxm.pixel, "_", this.x * mw, bottomLine - this.y * mh, -1);
      }

      int y = GraphicsModeZx.AH - footerHeight - 1;
      zxm.attrRect(0, y, GraphicsModeZx.AW, 1, GraphicsModeZx.BLACK, GraphicsModeZx.BLACK);
      zxm.attrRect(0, y, title.length(), 1, GraphicsModeZx.WHITE, GraphicsModeZx.BLACK);
      dialog.print(zxm.pixel, title, 0, y * 8, -1);

      zxm.uiRainbow(26, y);

      String[] footers = footer.split("\n");
      for (int i = 0; i < footers.length; i++) {
        dialog.print(zxm.pixel, footers[i], 0, (i - footers.length) * 8 + GraphicsModeZx.HEIGHT, -1);
      }
    }

    public void draw(BufferedImage image) {
      repaint();
      zxm.draw(image);
    }

    @Override
    public void write(byte[] b) {
      try {
        super.write(b);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public void write(int c) {
      if (c < 0x20) {
        switch (c) {
          case 0x0A:
            y--;
          case 0x0D:
            x = 0;
            break;
          case 0x08:
            if (x > 0) x--;
            break;
        }
        while (y < 0) {
          plainText.add(0, new byte[columns]);
          y++;
        }
        return;
      }
      if (x >= columns) {
        x = 0;
        y--;
      }
      while (y < 0) {
        plainText.add(0, new byte[columns]);
        y++;
      }
      plainText.get(y)[x] = (byte) c;
      x++;
    }
  }
}
