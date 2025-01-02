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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DifferentCharsetsTest {

  @Test
  void decodeEncode() {
    byte[] bytes = new byte[0x100];
    for (int i = 0; i < 0x100; i++) {
      bytes[i] = (byte) i;
    }
    assertArrayEquals(bytes, new String(bytes, StandardCharsets.ISO_8859_1).getBytes(StandardCharsets.ISO_8859_1));
    assertArrayEquals(bytes, new String(bytes, DifferentCharsets.IBM437).getBytes(DifferentCharsets.IBM437));
  }
}
