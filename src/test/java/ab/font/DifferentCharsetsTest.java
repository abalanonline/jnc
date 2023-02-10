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
