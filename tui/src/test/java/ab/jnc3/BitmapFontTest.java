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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

  @Disabled
  @Test
  void testIbm437() throws IOException {
    Charset charset = Charset.forName("IBM437");
    Map<Character, Set<Character>> map = new LinkedHashMap<>();
    String x0020 =  "\u0000\u263A\u263B\u2665\u2666\u2663\u2660\u2022\u25D8\u25CB\u25D9\u2642\u2640\u266A\u266B\u263C" +
        "\u25BA\u25C4\u2195\u203C\u00B6\u00A7\u25AC\u21A8\u2191\u2193\u2192\u2190\u221F\u2194\u25B2\u25BC";
    for (int i = 0; i < 0x20; i++) map.put(x0020.charAt(i), new LinkedHashSet<>());
    for (int i = 0x20; i < 0x100; i++) map.put(new String(new byte[]{(byte) i}, charset).charAt(0), new LinkedHashSet<>());
    HashSet<Character> exclude = new HashSet<>();
    for (char c = '\uE000'; c < '\uF800'; c++) exclude.add(c);
    List<Path> paths = Files.find(Path.of("/usr/share/kbd/consolefonts/"), 3, (path, attributes) -> {
      String s = path.getFileName().toString();
      if (s.equals("README.psfu")) return false;
      if (s.endsWith(".gz")) s = s.substring(0, s.length() - 3);
      return s.endsWith(".psf") || s.endsWith(".psfu");
    }).collect(Collectors.toList());
    for (Path path : paths) {
      byte[] bytes = Files.readAllBytes(path);
      BitmapFont font = BitmapFont.fromPsf(bytes);
      ArrayList<Character> list = new ArrayList<>();
      char keyChar = 0;
      int keyChars = 0;
      if (font.unicodeCache == null) continue;
      if (IntStream.range(0, font.unicodeCache.length).anyMatch(i -> exclude.contains(font.unicodeCache[i]))) {
        System.out.println(path);
        continue;
      }
      for (char c : font.unicodeCache) {
        if (c == '\uFFFF') {
          if (keyChars == 1) map.get(keyChar).addAll(list);
          list.clear();
          keyChars = 0;
          continue;
        }
        if (map.containsKey(c)) {
          keyChar = c;
          keyChars++;
        }
        list.add(c);
      }
    }
    for (Map.Entry<Character, Set<Character>> entry : map.entrySet()) {
      Set<Character> set = entry.getValue();
      if (set.size() > 1) System.out.println("" + entry.getKey()
          + set.stream().map(Object::toString).collect(Collectors.joining()));
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
}
