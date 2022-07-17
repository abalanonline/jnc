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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * Screen is a physical screen for writing and drawing. Should be available after instantiating.
 */
public class Screen extends JComponent implements KeyListener {

  private JFrame jFrame;
  private String title;
  private boolean fullscreen = true;
  public KeyListener keyListener;
  public BufferedImage image;
  public GraphicsMode mode;

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

  public BufferedImage createImage() {
    if (mode.colorMap == null) {
      return new BufferedImage(mode.resolution.width, mode.resolution.height, BufferedImage.TYPE_INT_RGB);
    }

    IndexColorModel colorModel =
        new IndexColorModel(8, mode.colorMap.length, mode.colorMap, 0, false, -1, DataBuffer.TYPE_BYTE);
    return new BufferedImage(mode.resolution.width, mode.resolution.height, BufferedImage.TYPE_BYTE_INDEXED,
        colorModel);
  }

  public Color backgroundColor() {
    return mode.colorMap == null ? new Color(mode.bgColor) : new Color(mode.colorMap[mode.bgColor]);
  }

  private void createJFrame() {
    jFrame = new JFrame();
    jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    if (fullscreen) {
      if ("Linux".equals(System.getProperty("os.name")) &&
          jFrame.getGraphicsConfiguration().getDevice().isFullScreenSupported()) {
        jFrame.getGraphicsConfiguration().getDevice().setFullScreenWindow(jFrame);
      } else {
        jFrame.setUndecorated(true);
        jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
      }
    }
    jFrame.add(this);
    setBackground(getBackground()); // update parent background
    jFrame.setTitle(title);
    jFrame.pack();
    requestFocusInWindow();
    jFrame.setVisible(true);
  }

  private void setMode(GraphicsMode mode) {
    this.mode = mode;
    image = createImage();
    setBackground(backgroundColor());
  }

  public Screen(GraphicsMode mode) {
    title = new Throwable().getStackTrace()[1].getClassName().replaceAll(".*\\.", "") + ".java";
    setPreferredSize(new Dimension(640, 480));
    setMode(mode);
    addKeyListener(this);
    createJFrame();
  }

  public void reset(GraphicsMode mode) {
    setMode(mode);
    repaint();
  }

  public void setFullscreen(boolean b) {
    fullscreen = b;
    jFrame.remove(this);
    jFrame.dispose();
    createJFrame();
  }

  @Override
  public void keyTyped(KeyEvent e) {
    if (keyListener != null) keyListener.keyTyped(e);
    if ((e.getKeyChar() == KeyEvent.VK_ENTER) && (e.getModifiersEx() == InputEvent.ALT_DOWN_MASK)) {
      setFullscreen(!fullscreen);
    }
    if ((e.getKeyChar() == KeyEvent.VK_ESCAPE) && (e.getModifiersEx() == 0)) {
      System.exit(0);
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (keyListener != null) keyListener.keyPressed(e);
    if ((e.getKeyCode() == KeyEvent.VK_F11) && (e.getModifiersEx() == 0)) {
      setFullscreen(!fullscreen);
    }
    if ((e.getKeyCode() == KeyEvent.VK_F11) && (e.getModifiersEx() == InputEvent.ALT_DOWN_MASK)) {
      int width = mode.resolution.height * mode.aspectRatio.width / mode.aspectRatio.height;
      int height = mode.resolution.width * mode.aspectRatio.height / mode.aspectRatio.width;
      setPreferredSize(new Dimension(Math.max(width, mode.resolution.width), Math.max(height, mode.resolution.height)));
      setFullscreen(false);
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (keyListener != null) keyListener.keyReleased(e);
  }

  public void setTitle(String title) {
    this.title = title;
    jFrame.setTitle(title);
  }

  @Override
  public void setBackground(Color bg) {
    super.setBackground(bg);
    if (getParent() instanceof JComponent) getParent().setBackground(bg);
  }

}
