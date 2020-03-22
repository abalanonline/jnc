/*
 * Copyright 2020 Aleksei Balan
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

package ab.jnc;

import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.gif.GIFImageReaderSpi;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;

//@NoArgsConstructor
@Getter
public class Sprite {

  private int width;
  private int height;
  private final ArrayList<Image> frames;
  private final BufferedImage attr;
  private int attrX;
  private int attrY;


  // copy constructor
  public Sprite(Sprite sprite) {
    width = sprite.getWidth();
    height = sprite.getHeight();
    attr = new BufferedImage((width - 1) / 8 + 1, (height - 1) / 8 + 1, BufferedImage.TYPE_INT_RGB);
    frames = new ArrayList<>(sprite.getFrames());
  }

  @SneakyThrows
  public Sprite(URL url) {
    frames = new ArrayList<>();
    ImageReader gifImageReader = new GIFImageReader(new GIFImageReaderSpi());
    gifImageReader.setInput(ImageIO.createImageInputStream(url.openStream()));
    for (int i = 0; i < gifImageReader.getNumImages(true); i++) {
      BufferedImage image = gifImageReader.read(i);
      frames.add(image);
      width = Math.max(width, image.getWidth(null));
      height = Math.max(height, image.getHeight(null));
    }
    attr = new BufferedImage((width - 1) / 8 + 1, (height - 1) / 8 + 1, BufferedImage.TYPE_INT_RGB);
  }

  public Image getFrame(int i) {
    return frames.get(i); // (i % frames.size());
  }

  public void attrFineTune(int x, int y) {
    attrX = x;
    attrY = y;
  }
  public void drawImage(int x, int y, Graphics graphics, Graphics attribute) {
    graphics.drawImage(getFrame(0), x, y, null);
    attribute.drawImage(this.attr, (x + attrX) >> 3, (y + attrY) >> 3, null);
  }

}
