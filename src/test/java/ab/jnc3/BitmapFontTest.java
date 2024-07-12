/*
 * Copyright 2024 Aleksei Balan
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

package ab.jnc3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BitmapFontTest {

  @Disabled
  @Test
  void testValidCharacter() throws IOException {
    List<Path> paths = Files.find(Path.of("/usr/share/kbd/consolefonts/"), 3, (path, attributes) -> {
      String s = path.getFileName().toString();
      if (s.equals("README.psfu")) return false;
      if (s.endsWith(".gz")) s = s.substring(0, s.length() - 3);
      return s.endsWith(".psf") || s.endsWith(".psfu");
    }).collect(Collectors.toList());
    for (Path path : paths) {
      byte[] bytes = Files.readAllBytes(path);
      BitmapFont.fromPsf(bytes);
    }
  }
}
