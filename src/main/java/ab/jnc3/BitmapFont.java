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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class BitmapFont {
  public byte[] glyph = new byte[0];
  public final short[] unicode; // saving 128K with shorts
  public int length = 0;
  public int byteSize = 0;
  public int height = 0;
  public int width = 8;

  public BitmapFont() {
    unicode = new short[0x10000];
    Arrays.fill(unicode, (short) -1);
  }

  public static BitmapFont fromPsf1(byte[] b) {
    assert b.length >= 4 && b[0] == 0x36 && b[1] == 0x04 : "PSF1 header";
    // font
    BitmapFont font = new BitmapFont();
    boolean isUnicode = (b[2] & 6) != 0;
    font.length = (b[2] & 1) == 0 ? 0x100 : 0x200;
    font.byteSize = b[3];
    font.height = b[3];
    // glyph
    int end = 4 + font.length * font.byteSize;
    assert b.length >= end + (isUnicode ? font.length * 2 : 0): "PSF1 file size";
    font.glyph = Arrays.copyOfRange(b, 4, end);
    // unicode
    CharBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(b, end, b.length))
        .order(ByteOrder.LITTLE_ENDIAN).asCharBuffer();
    if (isUnicode) for (short i = 0; i < font.length; i++) {
      for (char c = buffer.get(); c != '\uFFFF'; c = buffer.get()) {
        if (c == '\uFFFE') throw new UnsupportedCharsetException("series of characters");
        font.unicode[c] = i;
      }
    } else for (short i = 0; i < 0x100; i++) font.unicode[i] = i;
    assert !buffer.hasRemaining() : "PSF1 file size";
    return font;
  }

  public static BitmapFont fromPsf2(byte[] b) {
    int[] header = new int[8];
    ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(header);
    assert header[0] == 0x864AB572 && header[1] == 0 && header[2] == 0x20 : "PSF2 header";
    // font
    BitmapFont font = new BitmapFont();
    boolean isUnicode = (header[3] & 1) == 1;
    font.length = header[4];
    font.byteSize = header[5];
    font.height = header[6];
    font.width = header[7];
    assert font.byteSize == font.height * ((font.width + 7) >> 3) : "PSF2 height width";
    // glyph
    int end = 0x20 + font.length * font.byteSize;
    assert b.length >= end + (isUnicode ? font.length : 0) : "PSF2 file size";
    font.glyph = Arrays.copyOfRange(b, 0x20, end);
    // unicode
    if (isUnicode) for (short i = 0; i < font.length; i++) {
      int s0 = end;
      for (byte c = b[end++]; c != -1; c = b[end++]) {
        if (c == (byte) 0xFE) throw new UnsupportedCharsetException("series of characters");
      }
      for (char c : new String(Arrays.copyOfRange(b, s0, end - 1), StandardCharsets.UTF_8).toCharArray()) {
        font.unicode[c] = i;
      }
    } else for (short i = 0; i < 0x100; i++) font.unicode[i] = i;
    assert end == b.length : "PSF2 file size";
    return font;
  }

  public static BitmapFont fromPsf(byte[] b) {
    if (b.length > 2 && b[0] == 0x36 && b[1] == 0x04) return fromPsf1(b);
    if (b.length > 4 && b[0] == 0x72 && b[1] == (byte) 0xB5 && b[2] == 0x4A && b[3] == (byte) 0x86)
      return fromPsf2(b);
    b = Arrays.copyOf(b, 4);
    throw new IllegalStateException(String.format("PSF header %02X %02X %02X %02X", b[0], b[1], b[2], b[3]));
  }

  public static byte[] gunzip(byte[] bytes) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
         ByteArrayInputStream input = new ByteArrayInputStream(bytes);
         GZIPInputStream gzip = new GZIPInputStream(input)) {
      gzip.transferTo(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void main(String[] args) throws IOException {
    List<Path> paths = Files.find(Path.of("/usr/share/kbd/consolefonts/"), 3, (path, attributes) -> {
      String s = path.getFileName().toString();
      if (s.equals("README.psfu")) return false;
      if (s.endsWith(".gz")) s = s.substring(0, s.length() - 3);
      return s.endsWith(".psf") || s.endsWith(".psfu");
    }).collect(Collectors.toList());
    for (Path path : paths) {
      byte[] bytes = Files.readAllBytes(path);
      if (path.toString().toLowerCase().endsWith(".gz")) bytes = gunzip(bytes);
      BitmapFont.fromPsf(bytes);
    }
  }
}
