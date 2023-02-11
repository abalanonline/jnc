/*
 * Copyright 2023 Aleksei Balan
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

package ab.font;

import ab.jnc2.TextFont;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * PFF2 bitmap font format.
 * http://grub.gibibit.com/New_font_format
 */
public class PffTwo {

  public String name;
  public String family;
  public String weight;
  public String slant;
  private short pointSize;
  private short maxWidth;
  private short maxHeight;
  private short ascent;
  private short descent;
  public final Map<Integer, PffTwoChar> characters;

  public PffTwo() {
    this.characters = new TreeMap<>(
        Comparator.comparingInt(a -> a + Integer.MIN_VALUE)); // unsigned
  }

  public static int readSectionLength(ByteBuffer buffer, String section) {
    //assert section.length() == 4;
    byte[] bytes = new byte[section.length()];
    buffer.get(bytes);
    assert section.equals(new String(bytes));
    return buffer.getInt();
  }

  public static byte[] readSection(ByteBuffer buffer, String section) {
    byte[] bytes = new byte[readSectionLength(buffer, section)];
    buffer.get(bytes);
    return bytes;
  }

  public static String readString(ByteBuffer buffer, String section) {
    byte[] bytes = readSection(buffer, section);
    assert bytes[bytes.length - 1] == 0;
    return new String(Arrays.copyOf(bytes, bytes.length - 1));
  }

  public static short readShort(ByteBuffer buffer, String section) {
    int length = readSectionLength(buffer, section);
    assert length == 2;
    return buffer.getShort();
  }

  public static PffTwo fromFile(byte[] bytes) {
    PffTwo that = new PffTwo();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);

    // FILE, NAME, FAMI, WEIG, SLAN, PTSZ, MAXW, MAXH, ASCE, DESC
    String sFile = new String(readSection(buffer, "FILE"));
    assert "PFF2".equals(sFile);
    that.name = readString(buffer, "NAME");
    that.family = readString(buffer, "FAMI");
    that.weight = readString(buffer, "WEIG"); // bold normal
    that.slant = readString(buffer, "SLAN"); // italic normal
    that.pointSize = readShort(buffer, "PTSZ");
    that.maxWidth = readShort(buffer, "MAXW");
    that.maxHeight = readShort(buffer, "MAXH");
    that.ascent = readShort(buffer, "ASCE");
    that.descent = readShort(buffer, "DESC");
    assert "bold".equals(that.weight) || "normal".equals(that.weight);
    assert "italic".equals(that.slant) || "normal".equals(that.slant);
    assert that.pointSize > 0;
    assert that.maxWidth > 0;
    assert that.maxHeight > 0;
    assert that.ascent + that.descent == that.maxHeight;

    int chixLength = readSectionLength(buffer, "CHIX");
    assert chixLength % 9 == 0;
    chixLength /= 9;
    int[] unicode = new int[chixLength];
    int[] offset = new int[chixLength];
    for (int i = 0; i < chixLength; i++) {
      unicode[i] = buffer.getInt();
      byte flags = buffer.get();
      assert flags == 0;
      offset[i] = buffer.getInt();
    }

    int dataLength = readSectionLength(buffer, "DATA");
    assert dataLength == -1;
    for (int i = 0; i < chixLength; i++) {
      assert buffer.position() == offset[i];
      short width = buffer.getShort();
      short height = buffer.getShort();
      short xOffset = buffer.getShort();
      short yOffset = buffer.getShort();
      short deviceWidth = buffer.getShort();
      assert width >= 0; // can be 0
      assert width <= that.maxWidth;
      assert width <= deviceWidth;
      assert height >= 0; // can be 0
      assert height <= that.maxHeight;
      assert xOffset >= 0;
      assert xOffset + width <= deviceWidth;

      PffTwoChar character = new PffTwoChar(width, height);
      character.xOffset = xOffset;
      character.yOffset = yOffset;
      character.deviceWidth = deviceWidth;
      byte b = 0;
      byte ib = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if (ib <= 0) {
            b = buffer.get();
            ib = 8;
          }
          character.bitmap[y][x] = (b & 0x80) != 0;
          b <<= 1;
          ib--;
        }
      }
      assert b == 0;
      that.characters.put(unicode[i], character);
    }
    assert !buffer.hasRemaining();
    return that;
  }

  public static void writeSectionLength(ByteBuffer buffer, String section, int length) {
    buffer.put(section.getBytes());
    buffer.putInt(length);
  }

  public static void writeSection(ByteBuffer buffer, String section, byte[] bytes) {
    writeSectionLength(buffer, section, bytes.length);
    buffer.put(bytes);
  }

  public static void writeString(ByteBuffer buffer, String section, String s) {
    writeSectionLength(buffer, section, s.length() + 1);
    buffer.put(s.getBytes());
    buffer.put((byte) 0);
  }

  public static void writeShort(ByteBuffer buffer, String section, short s) {
    writeSectionLength(buffer, section, 2);
    buffer.putShort(s);
  }

  public byte[] toFile() {
    int[] unicode = this.characters.keySet().stream().mapToInt(c -> c).toArray();
    int n = unicode.length;
    List<byte[]> characters = Arrays.stream(unicode).mapToObj(uc -> {
      PffTwoChar c = this.characters.get(uc);
      ByteBuffer buf = ByteBuffer.wrap(new byte[(c.width * c.height + 7) / 8 + 10]);
      buf.putShort(c.width);
      buf.putShort(c.height);
      buf.putShort(c.xOffset);
      buf.putShort(c.yOffset);
      buf.putShort(c.deviceWidth);
      byte b = 0;
      int ib = 0;
      for (int y = 0; y < c.height; y++) {
        for (int x = 0; x < c.width; x++) {
          if (ib == 0) {
            ib = 0x80;
          }
          if (c.bitmap[y][x]) b |= ib;
          ib >>= 1;
          if (ib == 0) {
            buf.put(b);
            b = 0;
          }
        }
      }
      if (ib > 0) buf.put(b);
      return buf.array();
    }).collect(Collectors.toList());
    int offsetBase = 20 + name.length() + 9 + family.length() + 9
        + weight.length() + 9 + slant.length() + 59 + n * 9 + 8;
    ByteBuffer buffer = ByteBuffer.allocate(offsetBase + characters.stream().mapToInt(b -> b.length).sum());

    writeSection(buffer, "FILE", "PFF2".getBytes());
    writeString(buffer, "NAME", name);
    writeString(buffer, "FAMI", family);
    writeString(buffer, "WEIG", weight);
    writeString(buffer, "SLAN", slant);
    writeShort(buffer, "PTSZ", pointSize);
    writeShort(buffer, "MAXW", maxWidth);
    writeShort(buffer, "MAXH", maxHeight);
    writeShort(buffer, "ASCE", ascent);
    writeShort(buffer, "DESC", descent);

    writeSectionLength(buffer, "CHIX", n * 9);
    for (int i = 0; i < n; i++) {
      buffer.putInt(unicode[i]);
      buffer.put((byte) 0);
      buffer.putInt(offsetBase);
      offsetBase += characters.get(i).length;
    }
    writeSectionLength(buffer, "DATA", -1);
    for (int i = 0; i < n; i++) {
      buffer.put(characters.get(i));
    }
    return buffer.array();
  }

  public static PffTwo fromTextFont(TextFont textFont) {
    PffTwo that = new PffTwo();
    short shortWidth = (short) textFont.width;
    short shortHeight = (short) textFont.height;
    byte[] charsetBytes = new byte[0x100];
    for (int i = 0; i < 0x100; i++) {
      charsetBytes[i] = (byte) i;
    }
    String charset = new String(charsetBytes, textFont.charset);

    that.name = "Unifont Regular " + shortHeight;
    that.family = "Unifont";
    that.weight = "normal";
    that.slant = "normal";
    that.pointSize = shortHeight;
    that.maxWidth = shortWidth;
    that.maxHeight = shortHeight;
    that.descent = 2; // TODO: 2023-02-10 calculate baseline
    that.ascent = (short) (that.maxHeight - that.descent);

    for (int i = 0; i < 0x100; i++) {
      int c = (textFont.font.length >> 8) * i;
      PffTwoChar character = new PffTwoChar(shortWidth, shortHeight);
      character.xOffset = (short) 0;
      character.yOffset = (short) -that.descent;
      character.deviceWidth = shortWidth;
      for (int y = 0; y < shortHeight; y++) {
        for (int x = 0; x < shortWidth; x++) {
          character.bitmap[y][x] = (textFont.font[c + y] >> (7 - x) & 1) == 1;
        }
      }
      that.characters.put((int) charset.charAt(i), character);
    }
    return that;
  }

  public TextFont toTextFont() {
    TextFont textFont = new TextFont(new byte[0], 0, 8, maxHeight);
    byte[] charsetBytes = new byte[0x100];
    for (int i = 0; i < 0x100; i++) {
      charsetBytes[i] = (byte) i;
    }
    String charset = new String(charsetBytes, textFont.charset);
    for (int i = 0; i < 0x100; i++) {
      char c = charset.charAt(i);
      PffTwoChar pffTwoChar = characters.get((int) c);
      if (pffTwoChar == null) continue;
      int yo = ascent - pffTwoChar.yOffset - pffTwoChar.height;
      int xo = pffTwoChar.xOffset;
      for (int y = 0; y < pffTwoChar.bitmap.length; y++) {
        boolean[] row = pffTwoChar.bitmap[y];
        for (int x = 0; x < row.length; x++) {
          if (row[x]) textFont.font[i * maxHeight + y + yo] |= 1 << (7 - x - xo);
        }
      }
    }
    return textFont;
  }

}
