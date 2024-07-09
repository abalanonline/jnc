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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PffTwoTest {

  @Test
  void fromFileToFile() throws IOException {
    byte[] file = getClass().getResourceAsStream("/jnc2/ascii.pf2").readAllBytes();
    assertArrayEquals(file, PffTwo.fromFile(file).toFile());

    TextFont textFont = TextFont.VGA16.get();
    assertArrayEquals(textFont.font, PffTwo.fromTextFont(textFont).toTextFont().font);
  }

  @Test
  @Disabled
  void vgaToFile() throws IOException {
    PffTwo font = PffTwo.fromTextFont(TextFont.VGA16.get());
    Map<Integer, PffTwoChar> map = font.characters;
    map.put(0x2501, map.get(0x2500)); // 0x2500 0x2550 horizontal
    map.put(0x2503, map.get(0x2502)); // 0x2502 0x2551 vertical
    map.put(0x250F, map.get(0x250C)); // 0x250C 0x2554 down right
    map.put(0x2513, map.get(0x2510)); // 0x2510 0x2557 down left
    map.put(0x2517, map.get(0x2514)); // 0x2514 0x255A up right
    map.put(0x251B, map.get(0x2518)); // 0x2518 0x255D up left
    font.name = "Vga Regular 16";
    font.family = "Vga";
    Files.write(Paths.get("vga.pf2"), font.toFile());
  }

}
