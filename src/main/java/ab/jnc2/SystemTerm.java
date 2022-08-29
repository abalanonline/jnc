/*
 * Copyright 2022 Aleksei Balan
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

import ab.Application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class SystemTerm implements Runnable {

  public static final int FOOTER_HEIGHT = 2;

  private final Screen screen;
  private final Basic basic;
  protected final GraphicsModeZx zxm;
  private final Tty tty;
  private final TextFont textFont;
  Process process;

  public SystemTerm(Screen screen) {
    this.zxm = new GraphicsModeZx();
    this.screen = screen;
    this.textFont = new TextFont("/48.rom", 0x3D00, 0x0300, 0x20, 8, 8);
    this.basic = screen == null ? null : new Basic(screen);
    this.tty = new Tty(GraphicsModeZx.AW, GraphicsModeZx.AH - FOOTER_HEIGHT - 1);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (process != null) process.destroy();
    }));
  }

  @Override
  public void run() {
    menu(tty, "System Terminal", "0 OK, 0:0", FOOTER_HEIGHT);
    zxm.draw(screen.image);
  }

  public void transferToTty(InputStream inputStream) {
    try {
      inputStream.transferTo(tty);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void menu(String text, String title, String footer, int footerHeight) {
    Tty tty = new Tty(GraphicsModeZx.AW, GraphicsModeZx.AH - footerHeight - 1);
    tty.write(text.getBytes(StandardCharsets.ISO_8859_1));
    menu(tty, title, footer, footerHeight);
  }

  public void menu(Tty tty, String title, String footer, int footerHeight) {
    zxm.cls(0, 7); // dark black and white

    int bottomLine = GraphicsModeZx.AH - footerHeight - 2;
    for (int y = bottomLine, i = 0; y >= 0; y--, i++) {
      textFont.print(zxm.pixel, tty.getLine(i), 0, y * 8, -1);
    }
    zxm.attrRect(tty.x, bottomLine - tty.y, 1, 1, GraphicsModeZx.WHITE & ~8, GraphicsModeZx.BLUE);

    int y = GraphicsModeZx.AH - footerHeight - 1;
    zxm.attrRect(0, y, GraphicsModeZx.AW, 1, GraphicsModeZx.BLACK, GraphicsModeZx.BLACK);
    zxm.attrRect(0, y, title.length(), 1, GraphicsModeZx.WHITE, GraphicsModeZx.BLACK);
    textFont.print(zxm.pixel, title, 0, y * 8, -1);

    zxm.uiRainbow(26, y);

    String[] footers = footer.split("\n");
    for (int i = 0; i < footers.length; i++) {
      textFont.print(zxm.pixel, footers[i], 0, (i - footers.length) * 8 + GraphicsModeZx.HEIGHT, -1);
    }
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.ZX);
    SystemTerm shell = new SystemTerm(screen);
    try {
      shell.process = Runtime.getRuntime().exec(
          new String[]{"top", "-b"},
          new String[]{"TERM=dumb", "COLUMNS=33", "LINES=21", });
      new Thread(() -> shell.transferToTty(shell.process.getInputStream())).start();
      new Thread(() -> shell.transferToTty(shell.process.getErrorStream())).start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    new Application(screen, false).run(shell);
  }

  public static class Tty extends OutputStream {

    private final int width;
    private final int height;
    private final List<byte[]> screen;
    public int x;
    public int y;

    public Tty(int width, int height) {
      this.width = width;
      this.height = height;
      this.screen = new LinkedList<>();
      for (int i = 0; i < height; i++) {
        screen.add(new byte[width]);
      }
      y = height - 1;
    }

    public String getLine(int index) {
      return new String(screen.get(index), StandardCharsets.ISO_8859_1);
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
        }
        while (y < 0) {
          screen.add(0, new byte[width]);
          y++;
        }
        return;
      }
      if (x >= width) {
        x = 0;
        y--;
      }
      while (y < 0) {
        screen.add(0, new byte[width]);
        y++;
      }
      screen.get(y)[x] = (byte) c;
      x++;
    }
  }
}
