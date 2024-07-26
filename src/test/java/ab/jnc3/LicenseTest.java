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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class LicenseTest {

  @Disabled
  @Test
  void testLicense() throws IOException {
    final String copyright = "/*\n * Copyright (C) %s Aleksei Balan\n *\n" +
        " * This program is free software: you can redistribute it and/or modify\n" +
        " * it under the terms of the GNU General Public License as published by\n" +
        " * the Free Software Foundation, either version 3 of the License, or\n" +
        " * (at your option) any later version.\n *\n" +
        " * This program is distributed in the hope that it will be useful,\n" +
        " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
        " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
        " * GNU General Public License for more details.\n *\n" +
        " * You should have received a copy of the GNU General Public License\n" +
        " * along with this program.  If not, see <https://www.gnu.org/licenses/>.\n */\n\n%s";
    Pattern pattern = Pattern.compile("/\\*\n\\s\\*\\sCopyright\\s(\\(C\\)\\s)?(?<year>20\\d{2})\\s.*?\\s\\*/\n\n" +
            "(?<body>.*)", Pattern.DOTALL);
    List<Path> paths = Files.find(Path.of("src"), 10, (path, attributes) ->
        path.getFileName().toString().endsWith(".java")).collect(Collectors.toList());
    for (Path path : paths) {
      String s = Files.readString(path);
      Matcher matcher = pattern.matcher(s);
      String year = matcher.matches() ? matcher.group("year") : Instant.now().toString().substring(0, 4);
      String body = matcher.matches() ? matcher.group("body") : s;
      Files.writeString(path, String.format(copyright, year, body));
    }
  }
}
