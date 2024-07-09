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

package ab.jnc1;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter @Setter
public class Font {

  private BufferedImage bitmap;
  private Dimension charDimension;
  private Function<Rectangle, Rectangle> transform = r -> r;
  private Supplier<Graphics> mainGraphicsSupplier = () -> null;
  private boolean alignRight;

  public Font() {
    //setSize(256, 256);
    bitmap = new BufferedImage(0x100, 0x100, BufferedImage.TYPE_BYTE_BINARY);
  }

  @SneakyThrows
  public Font(InputStream inputStream, int cw, int ch) {
    //ImageReader gifImageReader = new GIFImageReader(new GIFImageReaderSpi());
    // FIXME: 2021-12-12 gif
    ImageReader gifImageReader = ImageIO.getImageReadersByFormatName("gif").next();
    gifImageReader.setInput(ImageIO.createImageInputStream(inputStream));
    bitmap = gifImageReader.read(0);
    charDimension = new Dimension(cw, ch);
  }

  public Font(BufferedImage bufferedImage, int cw, int ch) {
    bitmap = bufferedImage;
    charDimension = new Dimension(cw, ch);
  }

  public Font(BufferedImage bufferedImage) {
    bitmap = bufferedImage;
    charDimension = new Dimension(bitmap.getWidth() >> 4, bitmap.getHeight() >> 4);
  }

  public Font withAlignRight(boolean alignRight) {
    this.alignRight = alignRight;
    return this;
  }

  public void write(String text, Point point, Graphics graphics) {
    Point p = new Point(point);
    int black = Color.BLACK.getRGB();
    int white = graphics.getColor().getRGB(); // current color
    int charImgWidth = bitmap.getWidth() >> 4;
    int charImgHeight = bitmap.getHeight() >> 4;
    while (!text.isEmpty()) {
      final byte[] b;
      int lineFeedIndex = text.indexOf('\n');
      if (lineFeedIndex >= 0) {
        b = text.substring(0, lineFeedIndex).getBytes();
        text = text.substring(lineFeedIndex + 1);
      } else {
        b = text.getBytes();
        text = "";
      }
      int w = b.length;
      if ((w > 0) && (b[w - 1] == '\r')) { w--; }
      w *= charDimension.width;
      BufferedImage tmp = new BufferedImage(w, charDimension.height, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < charDimension.height; y++) {
        for (int x = 0; x < w; x++) {
          byte b1 = b[x / charDimension.width];
          int x1 = ((b1 & 0x0F) * charImgWidth) + (x % charDimension.width);
          int y1 = ((b1 >> 4) * charImgHeight) + y;
          if (bitmap.getRGB(x1, y1) != black) tmp.setRGB(x, y, white);
        }
      }
      graphics.drawImage(tmp, alignRight ? p.x - tmp.getWidth() : p.x, p.y, null);
      p.translate(0, charDimension.height);
    }
  }

}
