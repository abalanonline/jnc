/*
 * Copyright (C) 2022 Aleksei Balan
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

import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.*;

class SystemTermTest {

  @Test
  void testMenu() throws Exception {
    SystemTerm.Tty zt;
    TextFont textFont = TextFont.ZX.get();
    byte[] scr = new byte[6912];

    new DataInputStream(getClass().getResourceAsStream("/jnc2/tapeloader.scr")).readFully(scr);
    zt = new SystemTerm.Tty(textFont);
    zt.title = "Tape Loader";
    zt.footer = "To cancel - press BREAK twice";
    zt.write("                                ".getBytes());
    zt.repaint();
    assertArrayEquals(scr, zt.zxm.toScr());

    new DataInputStream(getClass().getResourceAsStream("/jnc2/calculator.scr")).readFully(scr);
    zt = new SystemTerm.Tty(textFont);
    zt.title = "Calculator";
    zt.footer = "0 OK, 0:0";
    zt.write("1-1\n0".getBytes());
    zt.repaint();
    assertArrayEquals(scr, zt.zxm.toScr());
  }

}
