/*
 * Copyright (C) 2024 Aleksei Balan
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

package ab.jnc3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Basic3Test {

  @Test
  void findAnsiColors() {
    assertArrayEquals(new int[]{0, 15}, Basic3.findAnsiColors(TextMode.cgaHigh().colorMap));
    assertArrayEquals(new int[]{0, 4, 1, 5, 2, 6, 3, 7,
        8, 12, 9, 13, 10, 14, 11, 15}, Basic3.findAnsiColors(TextMode.zx().colorMap));
    assertArrayEquals(new int[]{0, 4, 2, 6, 1, 5, 3, 7,
        8, 12, 10, 14, 9, 13, 11, 15}, Basic3.findAnsiColors(TextMode.ega().colorMap));
  }
}
