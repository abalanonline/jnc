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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NyanCatTest {

  private List<BufferedImage> readGif(String resource) {
    try {
      List<BufferedImage> images = new ArrayList<>();
      InputStream inputStream = getClass().getResourceAsStream(resource);
      ImageReader gifImageReader = ImageIO.getImageReadersByFormatName("gif").next();
      gifImageReader.setInput(ImageIO.createImageInputStream(inputStream));
      BufferedImage master = null;
      for (int i = 0; i < gifImageReader.getNumImages(true); i++) {
        NodeList nodes = gifImageReader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0").getChildNodes();
        NamedNodeMap imageDescriptor = IntStream.range(0, nodes.getLength()).mapToObj(nodes::item)
            .filter(n -> "ImageDescriptor".equals(n.getNodeName())).findAny().get().getAttributes();
        BufferedImage image = gifImageReader.read(i);
        if (i == 0) {
          master = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }
        master.getGraphics().drawImage(image,
            Integer.parseInt(imageDescriptor.getNamedItem("imageLeftPosition").getNodeValue()),
            Integer.parseInt(imageDescriptor.getNamedItem("imageTopPosition").getNodeValue()),
            null);
        image = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_RGB);
        image.getGraphics().drawImage(master, 0, 0, null);
        images.add(image);
      }
      return images;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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

  private BufferedImage to70(BufferedImage image) {
    int w = image.getWidth();
    int h = image.getHeight();
    int sw = (w + 40) / 70 / 2;
    int sh = (h + 40) / 70 / 2;
    BufferedImage image70 = new BufferedImage(70, 70, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 70; y++) {
      for (int x = 0; x < 70; x++) {
        image70.setRGB(x, y, image.getRGB(w * x / 70 + sw, h * y / 70 + sh));
      }
    }
    return image70;
  }

  void assertImageEquals(byte[] expected, byte[] actual) {
    for (int y = 0, i = 0; y < 70; y++) {
      for (int x = 0; x < 70; x++, i += 3) {
        assertArrayEquals(Arrays.copyOfRange(expected, i, i + 3), Arrays.copyOfRange(actual, i, i + 3), x + ", " + y);
      }
    }
  }

  @Test
  void testDraw() {
    List<BufferedImage> images = readGif("/nyancat/poptart1red1.gif");
    byte[] skyColor = new byte[]{0x00, 0x33, 0x66};
    assertArrayEquals(skyColor, Arrays.copyOfRange(toByteArray(images.get(0)), 0, 3));
    int size = 1; //images.size();
    for (int i = 0; i < size; i++) {
      BufferedImage image = images.get(i);
      assertEquals(image.getWidth(), image.getHeight());
      assertImageEquals(toByteArray(to70(image)), new NyanCat().draw(i));
      assertArrayEquals(toByteArray(to70(image)), new NyanCat().draw(i));
    }
  }

}
