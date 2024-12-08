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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class VgaFontTest {

  public static final String BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

  private enum Font {
    VGA8(8, 0x5266), VGA14(14, 0x5A66), VGA16(16, 0x6993);
    public final int height;
    public final int address;

    Font(int height, int address) {
      this.height = height;
      this.address = address;
    }
  }

  private static BitmapFont makeFont(byte[] bytes) {
    BitmapFont font = new BitmapFont(8, bytes.length / 0x100);
    font.bitmap = bytes;
    Charset charset = Charset.forName("IBM437");
    for (int i = 0; i < 0x100; i++) font.put(new String(new byte[]{(byte) i}, charset).charAt(0), i);
    font.cacheBitmap();
    return font;
  }

  public int comparePsf(BitmapFont font1, BitmapFont font2, String chars) {
    if (font1.height != font2.height) return 0;
    int h = font1.height;
    int result = 0;
    mismatch:
    for (char c : chars.toCharArray()) {
      int i1 = font1.get(c) * h;
      int i2 = font2.get(c) * h;
      if (i1 < 0 || i2 < 0) continue;
      for (int i = 0; i < h; i++) if (font1.bitmap[i1++] != font2.bitmap[i2++]) continue mismatch;
      result++;
    }
    return result;
  }

  private String test0zBox() {
    byte[] testBytes = new byte[0x8F];
    int testBytesIndex = 0;
    for (int i = 0x20; i < 0x7F; i++) testBytes[testBytesIndex++] = (byte) i;
    for (int i = 0xB0; i < 0xE0; i++) testBytes[testBytesIndex++] = (byte) i;
    return new String(testBytes, Charset.forName("IBM437"));
  }

  private String testAllVisible() {
    byte[] testBytes = new byte[0xDF];
    int testBytesIndex = 0;
    for (int i = 0x80; i < 0x100; i++) testBytes[testBytesIndex++] = (byte) i;
    for (int i = 0x20; i < 0x7F; i++) testBytes[testBytesIndex++] = (byte) i;
    return new String(testBytes, Charset.forName("IBM437"));
  }

  private List<Path> findAllPsf() {
    try {
      return Files.list(Path.of("/usr/share/kbd/consolefonts/")).filter(path -> {
        String s = path.getFileName().toString();
        if (s.equals("README.psfu")) return false;
        if (s.endsWith(".gz")) s = s.substring(0, s.length() - 3);
        return s.endsWith(".psf") || s.endsWith(".psfu");
      }).collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Disabled
  @Test
  void findVgaGlyph() throws IOException {
    byte[] b0 = Files.readAllBytes(Paths.get("assets/90x7423.zm14"));
    byte[] b1 = Files.readAllBytes(Paths.get("assets/90x7426.zm16"));
    byte[] vga = new byte[0x10000];
    for (int i = 0, p = 0; i < 0x8000; i++) {
      vga[p++] = b0[i];
      vga[p++] = b1[i];
    }
    assertEquals(65, comparePsf(makeFont(Arrays.copyOfRange(vga, Font.VGA16.address, 0x1000 + Font.VGA16.address)),
        TextMode.vgaHigh().font, BASE64_ALPHABET));
    assertEquals(65, comparePsf(makeFont(Arrays.copyOfRange(vga, Font.VGA8.address, 0x800 + Font.VGA8.address)),
        TextMode.cga4().font, BASE64_ALPHABET));

    String testChars = test0zBox();
    List<Path> paths = findAllPsf();

    for (Font font : Font.values()) {
      byte[] vgaBytes = Arrays.copyOfRange(vga, font.address, font.height * 0x100 + font.address);
      BitmapFont vgaFont = makeFont(vgaBytes);
      Map<Integer, List<String>> top = new TreeMap<>(Comparator.comparingInt(a -> -a));
      for (Path path : paths) {
        BitmapFont psfFont = BitmapFont.fromPsf(Files.readAllBytes(path));
        int similarity = comparePsf(vgaFont, psfFont, testChars);
        top.computeIfAbsent(similarity, a -> new ArrayList<>()).add(path.toString());
      }
      Map.Entry<Integer, List<String>> entry = top.entrySet().iterator().next();
      System.out.printf("%d/%d %s%n", entry.getKey(), testChars.length(), String.join(", ", entry.getValue()));
      //142/143 /usr/share/kbd/consolefonts/gr737c-8x8.psfu.gz
      //138/143 /usr/share/kbd/consolefonts/gr737c-8x14.psfu.gz
      //142/143 /usr/share/kbd/consolefonts/gr737c-8x16.psfu.gz, /usr/share/kbd/consolefonts/gr737d-8x16.psfu.gz
      //157/223 /usr/share/kbd/consolefonts/gr737c-8x8.psfu.gz
      //205/223 /usr/share/kbd/consolefonts/lat7a-14.psfu.gz
      //202/223 /usr/share/kbd/consolefonts/default8x16.psfu.gz
    }

  }

  @Disabled
  @Test
  void findVgaCodePage() throws IOException {
    String testString = testAllVisible();
    byte[] testBytes = testString.getBytes(Charset.forName("IBM437"));
    char[] testChars = testString.toCharArray();
    Map<Integer, List<String>> top = new TreeMap<>(Comparator.comparingInt(a -> -a));
    List<Path> allPsf = findAllPsf();
    for (Path path : allPsf) {
      BitmapFont font = BitmapFont.fromPsf(Files.readAllBytes(path));
      int similarity = 0;
      for (int i = 0; i < testChars.length; i++) {
        int v = font.get(testChars[i]);
        if (v < 0) similarity--;
        if (v == (testBytes[i] & 0xFF)) similarity++;
      }
      top.computeIfAbsent(similarity, a -> new ArrayList<>()).add(path.toString());
    }
    Map.Entry<Integer, List<String>> entry = top.entrySet().iterator().next();
    System.out.printf("%d/%d %s%n", entry.getKey(), testChars.length, String.join(", ", entry.getValue()));
    // /usr/share/kbd/consolefonts/ter-i32b.psf.gz
    // /usr/share/kbd/consolefonts/default8x9.psfu.gz,
    // /usr/share/kbd/consolefonts/default8x16.psfu.gz,
  }

}
