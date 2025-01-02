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
