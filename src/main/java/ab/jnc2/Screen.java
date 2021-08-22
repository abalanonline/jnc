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

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * Screen is a physical screen for writing and drawing. Should be available after instantiating.
 */
public class Screen extends JComponent {

  private JFrame jFrame;
  BufferedImage image;
  GraphicsMode mode;

  @Override
  protected void paintComponent(Graphics g) {
    int cw = getWidth(); // component width
    int ch = getHeight();
    int aw = ch * mode.aspectRatio.width / mode.aspectRatio.height; // aspect width
    int ah = cw * mode.aspectRatio.height / mode.aspectRatio.width;
    aw = Math.min(cw, aw);
    ah = Math.min(ch, ah);
    g.drawImage(image, (cw - aw) / 2, (ch - ah) / 2, aw, ah, null);
  }

  public static BufferedImage createImage(GraphicsMode mode) {
    if (mode.colorMap == null) {
      return new BufferedImage(mode.resolution.width, mode.resolution.height, BufferedImage.TYPE_INT_RGB);
    }

    IndexColorModel colorModel =
        new IndexColorModel(8, mode.colorMap.length, mode.colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);
    return new BufferedImage(mode.resolution.width, mode.resolution.height, BufferedImage.TYPE_BYTE_INDEXED,
        colorModel);
  }

  public Screen(GraphicsMode mode) {
    this.mode = mode;
    setPreferredSize(new Dimension(640, 480));
    image = createImage(mode);
    jFrame = new JFrame();
    jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    jFrame.add(this);
    jFrame.pack();
    jFrame.setVisible(true);
  }

  public Basic basic() {
    return new Basic(this);
  }

}
