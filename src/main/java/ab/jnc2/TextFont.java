/*
 * Copyright 2021 Aleksei Balan
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Text can load font and print text.
 * For printing it use BufferedImage and fourth quadrant coordinates native to java.
 */
public class TextFont {
  byte[] font;
  int height;
  int width;

  public TextFont(String resource, int byteStart, int byteSize, int charStart, int width, int height) {
    InputStream input = TextFont.class.getResourceAsStream(resource);
    byte[] buffer = new byte[byteSize];
    try {
      if ((input.skip(byteStart) != byteStart) || (input.read(buffer) != byteSize))
        throw new IllegalStateException("buffer error");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    font = new byte[height * 0x100];
    System.arraycopy(buffer, 0, font, charStart * height, byteSize);
    this.width = width;
    this.height = height;
  }

  private void print(BufferedImage image, String s, int x, int y, int color, int bgColor, boolean withBackground) {
    int width = image.getWidth();
    int height = image.getHeight();
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      c *= this.height;
      for (int iy = 0; iy < this.height; iy++) {
        for (int ix = 0; ix < this.width; ix++) {
          int jx = x + ix;
          int jy = y + iy;
          if (jx < 0 || jy < 0 || jx >= width || jy >= height) continue;
          if ((font[c + iy] >> (7 - ix) & 1) == 1) {
            image.setRGB(jx, jy, color);
          } else if (withBackground) {
            image.setRGB(jx, jy, bgColor);
          }
        }
      }
      x += this.width;
    }
  }

  public void print(BufferedImage image, String s, int x, int y, int color) {
    print(image, s, x, y, color, 0, false);
  }

  public void print(BufferedImage image, String s, int x, int y, int color, int bgColor) {
    print(image, s, x, y, color, bgColor, true);
  }

  public void preview(BufferedImage image) {
    for (int y = 0; y < 16; y++) {
      for (int x = 0; x < 16; x++) {
        print(image, Character.toString((char) (y * 16 + x)), x * width, y * height, 0xFFFF00, 0x0000AA);
      }
    }
  }

}
