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

package ab;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ApplicationTest {

  @Test
  void testMenu() throws Exception {
    Application application = new Application(null, false);
    byte[] scr = new byte[6912];

    new DataInputStream(getClass().getResourceAsStream("/zx128.scr")).readFully(scr);
    application.menu("128", new String[]{"Tape Loader", "128 BASIC", "Calculator", "48 BASIC", "Tape Tester"},
        "\u007F 1986 Sinclair Research Ltd", 0);
    assertArrayEquals(scr, application.zxm.toScr());

    new DataInputStream(getClass().getResourceAsStream("/zxplus3.scr")).readFully(scr);
    application.menu("128 +3", new String[]{"Loader", "+3 BASIC", "Calculator", "48 BASIC"},
        "\u007F1982, 1986, 1987 Amstrad Plc.\nDrives A:, B: and M: available.", 3);
    assertArrayEquals(scr, application.zxm.toScr());
  }
}
