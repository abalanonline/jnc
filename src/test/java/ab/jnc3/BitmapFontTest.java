/*
 * Copyright 2024 Aleksei Balan
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

package ab.jnc3;

import ab.jnc2.TextFont;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BitmapFontTest {

  public static final String BROWN_FOX = "The quick brown fox jumps over the lazy dog";

  @Disabled
  @Test
  void testValidCharacter() throws IOException {
    List<Path> paths = Files.find(Path.of("/usr/share/kbd/consolefonts/"), 3, (path, attributes) -> {
      String s = path.getFileName().toString();
      if (s.equals("README.psfu")) return false;
      if (s.endsWith(".gz")) s = s.substring(0, s.length() - 3);
      return s.endsWith(".psf") || s.endsWith(".psfu");
    }).collect(Collectors.toList());
    for (Path path : paths) {
      byte[] bytes = Files.readAllBytes(path);
      BitmapFont font = BitmapFont.fromPsf(bytes);
      if (bytes[0] == 0x1F) bytes = BitmapFont.gunzip(bytes);
      if (bytes[0] == 0x72 && bytes[12] == 0) continue;
      byte[] psf = bytes[0] == 0x72 ? font.toPsf2() : font.toPsf();
      assertArrayEquals(bytes, psf);
    }
  }

  public static void testFont(Screen screen, BitmapFont font) {
    int w = screen.image.getWidth();
    int h = screen.image.getHeight();
    DataBuffer buffer = screen.image.getRaster().getDataBuffer();
    for (int i = buffer.getSize() - 1; i >= 0; i--) buffer.setElem(i, 0);
    for (int i = 0; i < font.length; i++) {
      int cx = i % 32 * font.width;
      int cy = (i / 32 + 1) * font.height;
      if (cx + font.width > w || cy + font.height > h) continue;
      //font.drawChar(i, cx, cy, screen.image, 0xFFCCCCCC);
      font.drawCharSimple(i, cx + cy * w, w - font.width, buffer, 0xFFCCCCCC, 0xFF333333);
    }
  }

  public static void sleep() {
    try {
      while (true) Thread.sleep(1000);
    } catch (InterruptedException ignore) {}
  }

  @Disabled
  @Test
  void testPerformance() throws IOException {
    Screen screen = new Screen();
    List<Path> paths = Files.find(Path.of("/usr/share/kbd/consolefonts/"), 3, (path, attributes) -> {
      String s = path.getFileName().toString();
      if (s.equals("README.psfu")) return false;
      if (s.endsWith(".gz")) s = s.substring(0, s.length() - 3);
      return s.endsWith(".psf") || s.endsWith(".psfu");
    }).collect(Collectors.toList());
    final AtomicInteger iFont = new AtomicInteger();
    final AtomicInteger xText = new AtomicInteger();
    final AtomicInteger yText = new AtomicInteger();
    final AtomicLong ts0 = new AtomicLong();
    final AtomicLong tf0 = new AtomicLong();
    AtomicReference<BitmapFont> font = new AtomicReference<>();
    font.set(BitmapFont.fromPsf(Files.readAllBytes(paths.get(0))));
    Runnable update = () -> {
      testFont(screen, font.get());
      //font.get().drawStringSimple(BROWN_FOX, xText.get() / 3, yText.get() / 3, screen.image, 0xFF8000);
      BitmapFont font1 = font.get();
      BufferedImage image1 = screen.image;
      int iter1 = 100;
      int iter2 = 2000;
      for (int j = 0; j < iter1; j++) font1.drawStringSimple(BROWN_FOX, 0, 100, image1, 1);
      long timeSimple = System.nanoTime();
      for (int j = 0; j < iter2; j++) font1.drawStringSimple(BROWN_FOX, 0, 100, image1, 1);
      timeSimple -= System.nanoTime();
      for (int j = 0; j < iter1; j++) font1.drawString(BROWN_FOX, 0, 100, image1, 1);
      long timeFull = System.nanoTime();
      for (int j = 0; j < iter2; j++) font1.drawString(BROWN_FOX, 0, 100, image1, 1);
      timeFull -= System.nanoTime();
      ts0.addAndGet(timeSimple);
      tf0.addAndGet(timeFull);
      font1.drawStringSimple(
          String.format("%d / %d = %.3f", -timeSimple / 1_000_000, -timeFull / 1_000_000,
              (double) timeSimple / timeFull), 160, 0, image1, 0x00FF00);
      font1.drawStringSimple(
          String.format("%d / %d = %.3f", -ts0.get() / 1_000_000, -tf0.get() / 1_000_000,
              (double) ts0.get() / tf0.get()), 0, 0, image1, 0xFFFF00);
      screen.update();
    };
    screen.eventSupplier.addMouseMotionListener(new MouseMotionListener() {
      int x;
      int y;
      @Override
      public void mouseMoved(MouseEvent e) {
        x = e.getX();
        y = e.getY();
      }
      @Override
      public void mouseDragged(MouseEvent e) {
        xText.addAndGet(e.getX() - x);
        yText.addAndGet(e.getY() - y);
        x = e.getX();
        y = e.getY();
        update.run();
      }
    });
    screen.keyListener = k -> {
      switch (k) {
        case "PageUp": if (iFont.decrementAndGet() < 0) iFont.incrementAndGet(); break;
        case "PageDown": if (iFont.incrementAndGet() >= paths.size()) iFont.decrementAndGet(); break;
        case "Esc": System.exit(0); break;
      }
      try {
        font.set(BitmapFont.fromPsf(Files.readAllBytes(paths.get(iFont.get()))));
        update.run();
      } catch (IOException ignore) {}
    };
    for (int i = 0; i < paths.size(); i++) {
      font.set(BitmapFont.fromPsf(Files.readAllBytes(paths.get(i))));
      update.run();
    }
    //update.run();
    sleep();
  }

  @Disabled
  @Test
  void vga14x3() throws IOException {
    TextFont vga14 = ab.jnc2.TextFont.VGA14.get();
    BitmapFont font = new BitmapFont();
    font.bitmap = vga14.font;
    font.length = 0x100;
    font.byteSize = 14;
    font.height = 14;
    font.width = 8;
    Charset charset = Charset.forName("IBM437");
    for (int i = 0; i < 0x100; i++) font.put(new String(new byte[]{(byte) i}, charset).charAt(0), i);
    font.multiply(3, 3);
    font.cacheBitmap();
    Files.write(Paths.get("assets/vga14x3.psfu"), font.toPsf());
    //testFont(new Screen(), font);
    //sleep();
  }
}
