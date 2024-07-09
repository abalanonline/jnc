/*
 * Copyright 2021 Aleksei Balan
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
