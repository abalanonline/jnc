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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PetsciiTest {

  private byte[] bytes;

  @BeforeEach
  void setUp() throws IOException {
    bytes = Files.readAllBytes(Paths.get("../assets/901225-01.u5"));
  }

  private void assertShifted(int sf, int st, int us) { // shifted from, shifted to, unshifted shift
    int n = (st - sf) * 8;
    us = (us + sf) * 8;
    sf = sf * 8 + 0x800;
    assertArrayEquals(Arrays.copyOfRange(bytes, sf, sf + n), Arrays.copyOfRange(bytes, us, us + n));
  }

  @Disabled
  @Test
  void testEquals() {
    // inverted
    byte[] inverted = Arrays.copyOf(bytes, bytes.length);
    for (int i = 0; i < inverted.length; i++) inverted[i] ^= 0xFF;
    inverted[0x005] ^= 4;
    inverted[0x805] ^= 4; // at sign 1 pixel difference
    assertArrayEquals(Arrays.copyOfRange(bytes, 0x400, 0x800), Arrays.copyOfRange(inverted, 0x000, 0x400));
    assertArrayEquals(Arrays.copyOfRange(bytes, 0xC00, 0x1000), Arrays.copyOfRange(inverted, 0x800, 0xC00));

    // shifted
    assertShifted(0x00, 0x01, 0); // at sign
    // 01 - 1A = a-z
    assertShifted(0x1B, 0x41, 0); // digits
    assertShifted(0x41, 0x5B, -0x40); // A-Z
    assertShifted(0x5B, 0x5E, 0);
    // 5E, 5F = inverse medium shade, upper left to lower right
    assertShifted(0x60, 0x69, 0);
    // 69 = upper right to lower left
    assertShifted(0x6A, 0x7A, 0);
    // 7A = check mark
    assertShifted(0x7B, 0x80, 0);
  }

  private static final String CODE_PAGE =
      "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[£]↑←" +
      " !\"#$%&'()*+,-./0123456789:;<=>?" +
      "─♠~~~~~~~╮╰╯~╲╱~~•~♥~╭╳○♣~♦┼~│π◥" +
      " ▌▄▔▁▏▒▕~◤~├▗└┐▂┌┴┬┤▎▍~~~▃~▖▝┘▘▚" +
      "~abcdefghijklmnopqrstuvwxyz";

  @Disabled
  @Test
  void toPsf() throws IOException {
    BitmapFont font = new BitmapFont(8, 8);
    font.bitmap = new byte[0x800];
    System.arraycopy(bytes, 0, font.bitmap, 0, 0x400);
    System.arraycopy(bytes, 0x800, font.bitmap, 0x400, 0x400);
    for (int i = 0; i < 0x100; i++) font.unicode[i] = null;

    for (int i = 0; i < CODE_PAGE.length(); i++) {
      char c = CODE_PAGE.charAt(i);
      if (c != '~') font.put(c, i); // ~ doesn't exist
    }
    font.put('\u2713', 0xFA); // check mark
    font.multiply(3, 3);
    Files.write(Paths.get("../assets/c64x3.psfu"), font.toPsf());
  }
}
