/*
 * Copyright (C) 2020 Aleksei Balan
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

package ab.jnc1;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class Font {

  private BufferedImage bitmap;
  private Dimension charDimension;
  private Function<Rectangle, Rectangle> transform = r -> r;
  private Supplier<Graphics> mainGraphicsSupplier = () -> null;
  public boolean alignRight;

  public Font() {
    //setSize(256, 256);
    bitmap = new BufferedImage(0x100, 0x100, BufferedImage.TYPE_BYTE_BINARY);
  }

  public Font(InputStream inputStream, int cw, int ch) {
    //ImageReader gifImageReader = new GIFImageReader(new GIFImageReaderSpi());
    // FIXME: 2021-12-12 gif
    ImageReader gifImageReader = ImageIO.getImageReadersByFormatName("gif").next();
    try {
      gifImageReader.setInput(ImageIO.createImageInputStream(inputStream));
      bitmap = gifImageReader.read(0);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
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
