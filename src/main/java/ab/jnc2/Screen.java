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

/**
 * Screen is a physical screen for writing and drawing. Should be available after instantiating.
 */
public class Screen extends JComponent {

  private JFrame jFrame;
  BufferedImage image;

  @Override
  protected void paintComponent(Graphics g) {
    g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
  }

  public Screen() {
    int width = 160;
    int height = 120;
    setPreferredSize(new Dimension(width, height));
    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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
