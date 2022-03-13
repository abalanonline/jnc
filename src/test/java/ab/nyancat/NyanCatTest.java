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

package ab.nyancat;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NyanCatTest {

  private List<BufferedImage> readGif(String resource) {
    List<BufferedImage> images = new ArrayList<>();
    InputStream inputStream = getClass().getResourceAsStream(resource);
    ImageReader gifImageReader = ImageIO.getImageReadersByFormatName("gif").next();
    try {
      gifImageReader.setInput(ImageIO.createImageInputStream(inputStream));
      for (int i = 0; i < gifImageReader.getNumImages(true); i++) {
        images.add(gifImageReader.read(i));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return images;
  }

  private byte[] toByteArray(BufferedImage image) {
    int w = image.getWidth();
    int h = image.getHeight();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    for (int rgb : image.getRGB(0, 0, w, h, null, 0, w)) {
      outputStream.write(rgb >> 16);
      outputStream.write(rgb >> 8);
      outputStream.write(rgb);
    }
    return outputStream.toByteArray();
  }

  private byte[] toByteArraySlow(BufferedImage image) {
    int w = image.getWidth();
    int h = image.getHeight();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int rgb = image.getRGB(x, y);
        outputStream.write(rgb >> 16);
        outputStream.write(rgb >> 8);
        outputStream.write(rgb);
      }
    }
    return outputStream.toByteArray();
  }

  @Test
  void testDraw() {
    List<BufferedImage> images = readGif("/nyancat/poptart1red1.gif");
    byte[] skyColor = new byte[]{0x00, 0x33, 0x66};
    assertArrayEquals(skyColor, Arrays.copyOfRange(toByteArray(images.get(0)), 0, 3));
    for (int i = 0; i < images.size(); i++) {
      BufferedImage image = images.get(i);
      int size = image.getWidth();
      assertEquals(size, image.getHeight());
      assertArrayEquals(toByteArray(image), NyanCat.draw(i, size));
    }
  }

}
