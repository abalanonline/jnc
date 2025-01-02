/*
 * Copyright (C) 2021 Aleksei Balan
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

package ab.jnc2;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NibbleTest {

  @Test
  void get() throws Exception {
    byte[] b = MessageDigest.getInstance("MD5").digest(new byte[0]);
    UUID md5Uuid = UUID.nameUUIDFromBytes(new byte[0]);
    Nibble uuid0 = new Nibble();
    Nibble uuid0a = new Nibble(32);
    Nibble uuid1 = new Nibble(
        4, 4, 4, 4, 4, 4, 4, 4,
        4, 4, 4, 4,
        4, 4, 4, 4,
        4, 4, 4, 4,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4);
    Nibble uuid1a = new Nibble(32, new Nibble());
    Nibble uuid2 = new Nibble(
        new Nibble(4, 4, 4, 4, 4, 4, 4, 4),
        new Nibble(4, 4, 4, 4),
        new Nibble(4, 4, 4, 4),
        new Nibble(4, 4, 4, 4),
        new Nibble(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4));
    Nibble uuid2a = new Nibble(
        new Nibble(8, new Nibble()),
        new Nibble(4, new Nibble()),
        new Nibble(4, new Nibble()),
        new Nibble(4, new Nibble()),
        new Nibble(12, new Nibble()));

    assertEquals(4, uuid0.getSize());
    assertEquals(32, uuid0a.getSize());
    assertEquals(128, uuid1.getSize());
    assertEquals(128, uuid1a.getSize());
    assertEquals(128, uuid2.getSize());
    assertEquals(128, uuid2a.getSize());

    assertEquals(4, uuid0.get(b, 4));
    assertEquals(7, uuid0.get(b, 120));
    assertEquals(md5Uuid.getMostSignificantBits() >> 32, uuid0a.get(b, 0));
    assertEquals((int) md5Uuid.getLeastSignificantBits(), uuid0a.get(b, 96));

    assertEquals(1, uuid1.get(b, 0, 2));
    assertEquals(2, uuid1.get(b, 0, 29));
    assertEquals(8, uuid1a.get(b, 0, 4));
    assertEquals(8, uuid1a.get(b, 0, 27));

    assertEquals(8, uuid2.get(b, 0, 1, 0));
    assertEquals(0, uuid2.get(b, 0, 3, 3));
    assertEquals(9, uuid2a.get(b, 0, 0, 7));
    assertEquals(0, uuid2a.get(b, 0, 4, 0));

  }
}
