/*
 * Copyright (C) 2023 Aleksei Balan
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

package ab.font;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.HashMap;
import java.util.Map;

public class DifferentCharsets extends Charset {

  public static final DifferentCharsets IBM437 = new DifferentCharsets("IBM437_",
      "\u0000\u263A\u263B\u2665\u2666\u2663\u2660\u2022\u25D8\u25CB\u25D9\u2642\u2640\u266A\u266B\u263C" +
      "\u25BA\u25C4\u2195\u203C\u00B6\u00A7\u25AC\u21A8\u2191\u2193\u2192\u2190\u221F\u2194\u25B2\u25BC" +
      "\u0020\u0021\"\u0023\u0024\u0025\u0026\u0027\u0028\u0029\u002A\u002B\u002C\u002D\u002E\u002F" +
      "\u0030\u0031\u0032\u0033\u0034\u0035\u0036\u0037\u0038\u0039\u003A\u003B\u003C\u003D\u003E\u003F" +
      "\u0040\u0041\u0042\u0043\u0044\u0045\u0046\u0047\u0048\u0049\u004A\u004B\u004C\u004D\u004E\u004F" +
      "\u0050\u0051\u0052\u0053\u0054\u0055\u0056\u0057\u0058\u0059\u005A\u005B\\\u005D\u005E\u005F" +
      "\u0060\u0061\u0062\u0063\u0064\u0065\u0066\u0067\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F" +
      "\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077\u0078\u0079\u007A\u007B\u007C\u007D\u007E\u2302" +
      "\u00C7\u00FC\u00E9\u00E2\u00E4\u00E0\u00E5\u00E7\u00EA\u00EB\u00E8\u00EF\u00EE\u00EC\u00C4\u00C5" +
      "\u00C9\u00E6\u00C6\u00F4\u00F6\u00F2\u00FB\u00F9\u00FF\u00D6\u00DC\u00A2\u00A3\u00A5\u20A7\u0192" +
      "\u00E1\u00ED\u00F3\u00FA\u00F1\u00D1\u00AA\u00BA\u00BF\u2310\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB" +
      "\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255D\u255C\u255B\u2510" +
      "\u2514\u2534\u252C\u251C\u2500\u253C\u255E\u255F\u255A\u2554\u2569\u2566\u2560\u2550\u256C\u2567" +
      "\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256B\u256A\u2518\u250C\u2588\u2584\u258C\u2590\u2580" +
      "\u03B1\u00DF\u0393\u03C0\u03A3\u03C3\u00B5\u03C4\u03A6\u0398\u03A9\u03B4\u221E\u03C6\u03B5\u2229" +
      "\u2261\u00B1\u2265\u2264\u2320\u2321\u00F7\u2248\u00B0\u2219\u00B7\u221A\u207F\u00B2\u25A0\u00A0");

  private String canonicalName;
  private Map<Byte, Character> decodingMap;
  private Map<Character, Byte> encodingMap;

  public DifferentCharsets(String canonicalName, String charset) {
    super(canonicalName, null);
    if (charset.length() != 0x100) throw new IllegalStateException();
    this.canonicalName = canonicalName;
    decodingMap = new HashMap<>();
    encodingMap = new HashMap<>();
    for (int i = 0; i < charset.length(); i++) {
      byte b = (byte) i;
      char c = charset.charAt(i);
      decodingMap.put(b, c);
      encodingMap.put(c, b);
    }
  }

  @Override
  public boolean contains(Charset cs) {
    return cs instanceof DifferentCharsets
        && this.canonicalName.equals(((DifferentCharsets) cs).canonicalName);
  }

  @Override
  public CharsetDecoder newDecoder() {
    return new DifferentDecoder(this, decodingMap);
  }

  @Override
  public CharsetEncoder newEncoder() {
    return new DifferentEncoder(this, encodingMap);
  }

  public static class DifferentDecoder extends CharsetDecoder {

    private Map<Byte, Character> map;

    public DifferentDecoder(DifferentCharsets cs, Map<Byte, Character> map) {
      super(cs, 1, 1);
      this.map = map;
    }

    @Override
    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
      while (in.hasRemaining()) {
        Character c = map.get(in.get());
        if (c == null) {
          in.position(in.position() - 1);
          return CoderResult.unmappableForLength(1);
        }
        if (!out.hasRemaining()) {
          in.position(in.position() - 1);
          return CoderResult.OVERFLOW;
        }
        out.put(c);
      }
      return CoderResult.UNDERFLOW;
    }
  }

  public static class DifferentEncoder extends CharsetEncoder {

    private Map<Character, Byte> map;

    public DifferentEncoder(DifferentCharsets cs, Map<Character, Byte> map) {
      super(cs, 1, 1);
      this.map = map;
    }

    @Override
    protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
      while (in.hasRemaining()) {
        Byte b = map.get(in.get());
        if (b == null) {
          in.position(in.position() - 1);
          return CoderResult.unmappableForLength(1);
        }
        if (!out.hasRemaining()) {
          in.position(in.position() - 1);
          return CoderResult.OVERFLOW;
        }
        out.put(b);
      }
      return CoderResult.UNDERFLOW;
    }
  }
}
