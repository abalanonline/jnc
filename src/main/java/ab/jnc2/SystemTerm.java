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

  public static final int FOOTER_HEIGHT = 2;

  private final Screen screen;
  private final Tty tty;
  Process process;

  public SystemTerm(Screen screen) {
    this.screen = screen;
    TextFont textFont = new TextFont("/48.rom", 0x3D00, 0x0300, 0x20, 8, 8);
    this.tty = new Tty(textFont);
    tty.title = "System Terminal";
    tty.footer = "0 OK, 0:0";

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (process != null) process.destroy();
    }));
    screen.keyListener = this;
    try {
      this.process = Runtime.getRuntime().exec(
          new String[]{"bash", "--norc", "-i"},
          new String[]{"TERM=xterm", "COLUMNS=32", "LINES=21", "PS1=$", });
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
    new Application(screen, false).run(shell);
  }

  public static class Tty extends OutputStream {
    protected GraphicsModeZx zxm = new GraphicsModeZx();
    private final TextFont textFont;
    public String title = "";
    public String footer = "";
    public int footerHeight = 2;

    private final int width;
    private final int height;
    private final List<byte[]> screen;
    public int x;
    public int y;

    public Tty(TextFont textFont) {
      this.textFont = textFont;
      this.width = GraphicsModeZx.WIDTH / textFont.width;
      this.height = GraphicsModeZx.HEIGHT / textFont.height - footerHeight - 1;
      this.screen = new LinkedList<>();
      for (int i = 0; i < height; i++) {
        screen.add(new byte[width]);
      }
      y = height - 1;
    }

    public String getLine(int index) {
      return new String(screen.get(index), StandardCharsets.ISO_8859_1);
    }

    public void repaint() {
      zxm.cls(0, 7); // dark black and white

      int bottomLine = GraphicsModeZx.AH - footerHeight - 2;
      for (int y = bottomLine, i = 0; y >= 0; y--, i++) {
        textFont.print(zxm.pixel, this.getLine(i), 0, y * 8, -1);
      }
      zxm.attrRect(this.x, bottomLine - this.y, 1, 1, GraphicsModeZx.WHITE & ~8, GraphicsModeZx.BLUE);

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
            x--;
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