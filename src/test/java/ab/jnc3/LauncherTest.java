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

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LauncherTest {

  @Test
  void menu() throws IOException {
    ZxBasic basic = new ZxBasic(new Launcher().preferredMode());
    Launcher.calculator(basic);
    byte[] calculator = getClass().getResourceAsStream("/jnc2/calculator.scr").readAllBytes();
    calculator[0x1821] = 0x38; // remove blinking cursor
    assertArrayEquals(calculator, basic.getScr());
    basic.cls();

    Launcher.menu(basic, "128", "\u00A9 1986 Sinclair Research Ltd",
        List.of("Tape Loader", "128 BASIC", "Calculator", "48 BASIC", "Tape Tester"), 0);
    byte[] zx128 = getClass().getResourceAsStream("/jnc2/zx128.scr").readAllBytes();
    assertArrayEquals(zx128, basic.getScr());
    basic.cls();

    Launcher.menu(basic, "128 +3", "\u00A91982, 1986, 1987 Amstrad Plc.\nDrives A:, B: and M: available.",
        List.of("Loader", "+3 BASIC", "Calculator", "48 BASIC"), 3);
    byte[] zxplus3 = getClass().getResourceAsStream("/jnc2/zxplus3.scr").readAllBytes();
    assertArrayEquals(zxplus3, basic.getScr());
    basic.cls();

  }

  public static class ZxBasic extends Basic3 {
    final TextMode mode;
    private final BufferedImage image;
    private final byte[] attr;

    public ZxBasic(TextMode mode) {
      super(null, null);
      this.mode = mode;
      image = new BufferedImage(mode.size.width, mode.size.height, BufferedImage.TYPE_BYTE_BINARY);
      attr = new byte[mode.size.width * mode.size.height / 64];
      Arrays.fill(attr, (byte) 0x38);
    }

    @Override
    public void printAt(int x, int y, String s) {
      mode.font.drawString(s, x * mode.font.width, y * mode.font.height, image, 0xFFFFFF, 0);
      byte a = (byte) (this.paper << 3 & 0x78 | this.color & 7 | this.color << 3 & 0x40);
      for (int i = s.length(), p = y * 32 + x; i > 0; i--) attr[p++] = a;
    }

    public byte[] getScr() {
      int s = mode.size.width * mode.size.height;
      byte[] bytes = new byte[s / 8 + s / 64];
      DataBuffer buffer = image.getRaster().getDataBuffer();
      for (int y = 0, i = 0; y < 192; y++) {
        for (int x = 0, xb = (y & 0xC0 | y << 3 & 0x38 | y >> 3 & 7) * 32; x < 32; x++) {
          int elem = buffer.getElem(i++);
          bytes[xb++] = (byte) elem;
        }
      }
      System.arraycopy(attr, 0, bytes, s / 8, attr.length);
      return bytes;
    }

    @Override
    public void cls() {
      DataBuffer buffer = image.getRaster().getDataBuffer();
      for (int i = buffer.getSize() - 1; i >= 0; i--) buffer.setElem(i, 0);
      Arrays.fill(attr, (byte) 0x38);
    }
  }
}
