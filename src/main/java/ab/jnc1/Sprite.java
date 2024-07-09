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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

public class Sprite extends Rectangle {

  public final ArrayList<BufferedImage> frames;
  public int currentFrame;

  private BufferedImage attr;
  private Color attrColor;
  public Point attrTune = new Point();
  private Rectangle hitbox;

  public Function<Rectangle, Rectangle> transform = r -> r;
  public Supplier<Graphics> mainGraphicsSupplier;
  private Supplier<Graphics> attributeGraphicsSupplier;
  public Supplier<Graphics> debugGraphicsSupplier;

  // copy constructor
  public Sprite(Sprite sprite) {
    super(sprite);
    frames = new ArrayList<>(sprite.frames);
    if (attributeGraphicsSupplier != null) {
      // create attribute image only in attribute-aware environments
      attr = new BufferedImage(sprite.attr.getWidth(), sprite.attr.getHeight(), sprite.attr.getType());
    }
    attrTune = new Point(sprite.attrTune);
    hitbox = new Rectangle(sprite.hitbox);

    transform = sprite.transform;
    mainGraphicsSupplier = sprite.mainGraphicsSupplier;
    attributeGraphicsSupplier = sprite.attributeGraphicsSupplier;
    debugGraphicsSupplier = sprite.debugGraphicsSupplier;
  }

  public Sprite(InputStream inputStream) {
    frames = new ArrayList<>();
    //ImageReader gifImageReader = new GIFImageReader(new GIFImageReaderSpi());
    // FIXME: 2021-12-12 gif
    ImageReader gifImageReader = ImageIO.getImageReadersByFormatName("gif").next();
    try {
      gifImageReader.setInput(ImageIO.createImageInputStream(inputStream));
      for (int i = 0; i < gifImageReader.getNumImages(true); i++) {
        BufferedImage image = gifImageReader.read(i);
        frames.add(image);
        Dimension frameSize = new Dimension(image.getWidth(null), image.getHeight(null));
        setSize(Math.max(width, frameSize.width), Math.max(height, frameSize.height));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    attr = new BufferedImage((width + 7) >> 3, (height + 7) >> 3, BufferedImage.TYPE_INT_RGB);
    hitbox = new Rectangle(getSize());
  }

  public BufferedImage getFrame(int i) {
    return frames.get(i % frames.size());
  }

  public void attrPrepare() {
    BufferedImage image = frames.get(0);
    int white = Color.WHITE.getRGB();
    int black = Color.BLACK.getRGB();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb = image.getRGB(x, y);
        if (rgb != black) {
          attr.setRGB(x >> 3, y >> 3, rgb);
          image.setRGB(x, y, white);
        }
      }
    }
    attrColor = null;
  }

  public void attrFineTune(int dx, int dy) {
    attrTune.setLocation(dx, dy);
  }

//  public void setAttrColor(Color c) {
//    Graphics graphics = attr.getGraphics();
//    graphics.setColor(c);
//    graphics.fillRect(0, 0, attr.getWidth(), attr.getHeight());
//    graphics.dispose();
//  }

  public void drawImage(Point point, Graphics graphics) {
    graphics.drawImage(getFrame(currentFrame), point.x, point.y, null);

    Graphics attribute = attributeGraphicsSupplier == null ? null : attributeGraphicsSupplier.get();
    if (attribute != null) {
      Point attrPoint = new Point(point);
      attrPoint.translate(attrTune.x, attrTune.y);
      if (attrColor == null) {
        attribute.drawImage(this.attr, attrPoint.x >> 3, attrPoint.y >> 3, null);
      } else {
        int aw = ((attrPoint.x + width - 1) >> 3) - (attrPoint.x >> 3) + 1;
        int ah = ((attrPoint.y + height - 1) >> 3) - (attrPoint.y >> 3) + 1;
        attribute.setColor(attrColor);
        attribute.fillRect(attrPoint.x >> 3, attrPoint.y >> 3, aw, ah);
      }
    }

    Graphics debugGraphics = debugGraphicsSupplier == null ? null : debugGraphicsSupplier.get();
    if (debugGraphics != null) {
      Rectangle hb = getTransformedHitbox();
      debugGraphics.drawRect(hb.x, hb.y, hb.width-1, hb.height-1);
    }
  }

  public void drawImage() {
    drawImage(transform.apply(this).getLocation(), mainGraphicsSupplier.get());
  }

  public Rectangle getTransformedHitbox() {
    Point hitboxLocation = new Point(hitbox.getLocation());
    hitboxLocation.translate(x, y);
    return transform.apply(new Rectangle(hitboxLocation, hitbox.getSize()));
  }

  public boolean intersects(Sprite s) {
    return getTransformedHitbox().intersects(s.getTransformedHitbox());
  }

}
