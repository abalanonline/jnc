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
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PffTwoTest {

  @Test
  void fromFileToFile() throws IOException {
    byte[] file = getClass().getResourceAsStream("/ascii.pf2").readAllBytes();
    assertArrayEquals(file, PffTwo.fromFile(file).toFile());

    TextFont textFont = TextFont.VGA16.get();
    assertArrayEquals(textFont.font, PffTwo.fromTextFont(textFont).toTextFont().font);
  }

}
