/*
 * Copyright 2022 Aleksei Balan
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

import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.*;

class SystemTermTest {

  @Test
  void testMenu() throws Exception {
    SystemTerm.Tty zt;
    TextFont textFont = new TextFont("/48.rom", 0x3D00, 0x0300, 0x20, 8, 8);
    byte[] scr = new byte[6912];

    new DataInputStream(getClass().getResourceAsStream("/tapeloader.scr")).readFully(scr);
    zt = new SystemTerm.Tty(textFont);
    zt.title = "Tape Loader";
    zt.footer = "To cancel - press BREAK twice";
    zt.write("                                ".getBytes());
    zt.repaint();
    assertArrayEquals(scr, zt.zxm.toScr());

    new DataInputStream(getClass().getResourceAsStream("/calculator.scr")).readFully(scr);
    zt = new SystemTerm.Tty(textFont);
    zt.title = "Calculator";
    zt.footer = "0 OK, 0:0";
    zt.write("1-1\n0".getBytes());
    zt.repaint();
    assertArrayEquals(scr, zt.zxm.toScr());
  }

}
