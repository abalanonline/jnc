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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ApplicationTest {

  @Test
  void testMenu() throws Exception {
    Application application = new Application(null);
    byte[] scr = new byte[6912];

    new DataInputStream(getClass().getResourceAsStream("/jnc2/zx128.scr")).readFully(scr);
    application.menu("128", new String[]{"Tape Loader", "128 BASIC", "Calculator", "48 BASIC", "Tape Tester"},
        "\u007F 1986 Sinclair Research Ltd", 0);
    assertArrayEquals(scr, application.zxm.toScr());

    new DataInputStream(getClass().getResourceAsStream("/jnc2/zxplus3.scr")).readFully(scr);
    application.menu("128 +3", new String[]{"Loader", "+3 BASIC", "Calculator", "48 BASIC"},
        "\u007F1982, 1986, 1987 Amstrad Plc.\nDrives A:, B: and M: available.", 3);
    assertArrayEquals(scr, application.zxm.toScr());
  }
}
