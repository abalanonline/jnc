/*
 * Copyright (C) 2021 Aleksei Balan
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

package ab.jnc2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.time.Duration;
import java.time.Instant;

/**
 * Screen is a physical screen for writing and drawing. Should be available after instantiating.
 */
public class Screen extends JComponent implements KeyListener, AutoCloseable {

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
    //((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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

  private void createJFrame() {
    jFrame = new JFrame();
    jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    if (fullscreen) {
      jFrame.setUndecorated(true);
      if ("Linux".equals(System.getProperty("os.name")) &&
          jFrame.getGraphicsConfiguration().getDevice().isFullScreenSupported()) {
        jFrame.getGraphicsConfiguration().getDevice().setFullScreenWindow(jFrame);
      } else {
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
    setBackground(new Color(this.mode.getRgbColor(this.mode.bgColor)));
  }

  public Screen(GraphicsMode mode) {
    title = new Throwable().getStackTrace()[1].getClassName().replaceAll(".*\\.", "") + ".java";
    setPreferredSize(new Dimension(640, 480));
    setMode(mode);
    setFocusTraversalKeysEnabled(false);
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

  @Override
  public void close() {
    jFrame.dispatchEvent(new WindowEvent(jFrame, WindowEvent.WINDOW_CLOSING));
  }

  /**
   * Run the runnable and repaint the screen with the defined refresh rate.
   * @param refreshRate in Hz
   * @param runnable that update the screen
   * TODO: 2022-08-27 give a cool name for the method
   */
  public void flicker(double refreshRate, Runnable runnable) {
    int durationNanos = (int) Math.round(1_000_000_000 / refreshRate);
    Instant now = Instant.now();
    while (true) {
      runnable.run();
      this.repaint();
      now = now.plusNanos(durationNanos);
      Duration duration = Duration.between(Instant.now(), now);
      if (duration.isNegative()) {
        now = Instant.now();
        continue;
      }
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException e) {
        break;
      }
    }

  }

}
