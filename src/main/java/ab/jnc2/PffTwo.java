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

package ab.jnc2;

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

  private String name;
  private String family;
  private String weight;
  private String slant;
  private short pointSize;
  private short maxWidth;
  private short maxHeight;
  private short ascent;
  private short descent;
  private Map<Integer, PffTwoChar> characters;

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

  public void fromFile(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);

    // FILE, NAME, FAMI, WEIG, SLAN, PTSZ, MAXW, MAXH, ASCE, DESC
    String sFile = new String(readSection(buffer, "FILE"));
    assert "PFF2".equals(sFile);
    this.name = readString(buffer, "NAME");
    this.family = readString(buffer, "FAMI");
    this.weight = readString(buffer, "WEIG"); // bold normal
    this.slant = readString(buffer, "SLAN"); // italic normal
    this.pointSize = readShort(buffer, "PTSZ");
    this.maxWidth = readShort(buffer, "MAXW");
    this.maxHeight = readShort(buffer, "MAXH");
    this.ascent = readShort(buffer, "ASCE");
    this.descent = readShort(buffer, "DESC");
    assert "bold".equals(weight) || "normal".equals(weight);
    assert "italic".equals(slant) || "normal".equals(slant);
    assert pointSize > 0;
    assert maxWidth > 0;
    assert maxHeight > 0;
    assert ascent + descent == maxHeight;

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
    Map<Integer, PffTwoChar> characters = new TreeMap<>(
        Comparator.comparingInt(a -> a + Integer.MIN_VALUE)); // unsigned
    for (int i = 0; i < chixLength; i++) {
      assert buffer.position() == offset[i];
      short width = buffer.getShort();
      short height = buffer.getShort();
      short xOffset = buffer.getShort();
      short yOffset = buffer.getShort();
      short deviceWidth = buffer.getShort();
      assert width >= 0; // can be 0
      assert width <= maxWidth;
      assert width <= deviceWidth;
      assert height >= 0; // can be 0
      assert height <= maxHeight;
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
      characters.put(unicode[i], character);
    }
    this.characters = characters;
    assert !buffer.hasRemaining();
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

}
