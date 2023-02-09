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
import java.util.Map;
import java.util.TreeMap;

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
    Map<Integer, PffTwoChar> characters = new TreeMap<>();
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
      byte bs = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if (bs <= 0) {
            b = buffer.get();
            bs = 8;
          }
          character.bitmap[y][x] = (b & 0x80) != 0;
          b <<= 1;
          bs--;
        }
      }
      assert b == 0;
      characters.put(unicode[i], character);
    }
    assert !buffer.hasRemaining();
  }

}
