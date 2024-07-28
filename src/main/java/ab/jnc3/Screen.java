/*
 * Copyright (C) 2024 Aleksei Balan
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

package ab.jnc3;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class Screen implements AutoCloseable {

  public Consumer<String> keyListener;
  public final Component eventSupplier; // provides addKeyListener with keyReleased event, addMouseListener, etc
  public BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
  private final Dimension preferredSize = new Dimension(640, 480);
  public boolean interpolation;

  private final Canvas canvas;
  private final Frame frame;
  private final WindowListener windowListener;
  private boolean fullScreen = true;

  public Screen() {
    canvas = new Canvas();
    windowListener = new WindowListener(this::onEvent);
    frame = new Frame() {
      @Override
      public void update(Graphics g) {
        if (isShowing()) paint(g); // reduce flickering
      }
    };
    frame.add(canvas);
    frame.addWindowStateListener(windowListener);
    frame.addWindowListener(windowListener);
    frame.addKeyListener(windowListener);
    frame.setFocusTraversalKeysEnabled(false); // enable Tab
    resetFrame();
    eventSupplier = frame; // TODO: 2024-07-13 replace with a proxy Component
  }

  private void setFullScreenWindow(Window window) {
    if ("Linux".equals(System.getProperty("os.name"))
        && frame.getGraphicsConfiguration().getDevice().isFullScreenSupported())
      frame.getGraphicsConfiguration().getDevice().setFullScreenWindow(window);
  }

  private void resetFrame() {
    frame.dispose();
    frame.setUndecorated(fullScreen);
    frame.setExtendedState(fullScreen ? Frame.MAXIMIZED_BOTH : Frame.NORMAL);
    setFullScreenWindow(fullScreen ? frame : null);
    canvas.setPreferredSize(preferredSize);
    frame.pack();
    frame.setVisible(true);
  }

  public void setFullScreen(boolean fullScreen) {
    if (fullScreen == this.fullScreen) return;
    this.fullScreen = fullScreen;
    resetFrame();
  }

  public void setBackground(int rgb) {
    canvas.setBackground(new Color(rgb));
  }

  public void update() {
    canvas.repaint();
  }

  private void onEvent(String event) {
    switch (event) {
      case "F11": case "Alt+Enter": setFullScreen(!fullScreen); return;
      case "Maximize": setFullScreen(true); return;
      case "Restore": setFullScreen(false); return;
      case "Close": close(); // then notify keylistener
    }
    Consumer<String> keyListener = this.keyListener;
    if (keyListener != null) keyListener.accept(event);
  }

  @Override
  public void close() {
    frame.dispose();
    frame.dispose(); // FIXME: 2024-06-16 not always disposing from the first attempt
  }

  private class Canvas extends Component {

    public Canvas() {
      setFocusable(false);
      setBackground(Color.BLACK);
    }

    @Override
    public void paint(Graphics graphics) {
      Dimension aspectRatio = preferredSize;
      if (interpolation && graphics instanceof Graphics2D) ((Graphics2D) graphics).setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      Rectangle clipBounds = graphics.getClipBounds();
      int width = clipBounds.width;
      int height = clipBounds.height;
      int w = height * aspectRatio.width / aspectRatio.height;
      int h = width * aspectRatio.height / aspectRatio.width;
      w = Math.min(width, w);
      h = Math.min(height, h);
      int x = (width - w) / 2;
      int y = (height - h) / 2;
      graphics.drawImage(image, x, y, w, h, this);
      if (x > 0) {
        graphics.clearRect(0, 0, x, h);
        x += w;
        graphics.clearRect(x, 0, width - x, h);
      }
      if (y > 0) {
        graphics.clearRect(0, 0, w, y);
        y += h;
        graphics.clearRect(0, y, w, height - y);
      }
    }
  }

  /**
   * Listens to the events of window and keyboard. Creates a sensible text describing the event
   * and sends it to the consumer. Marked final to show that it designed properly and should not be refactored.
   */
  private static final class WindowListener extends WindowAdapter implements KeyListener {
    private final Consumer<String> eventListener;

    public WindowListener(Consumer<String> eventListener) {
      this.eventListener = eventListener;
    }

    @Override
    public void windowClosing(WindowEvent e) {
      eventListener.accept("Close");
    }

    @Override
    public void windowStateChanged(WindowEvent e) {
      if (e.getOldState() == Frame.NORMAL && e.getNewState() == Frame.MAXIMIZED_BOTH) eventListener.accept("Maximize");
      if (e.getOldState() == Frame.MAXIMIZED_BOTH && e.getNewState() == Frame.NORMAL) eventListener.accept("Restore");
    }

    private static String fromKeyChar(char c) {
      switch (c) {
        case KeyEvent.VK_ENTER: return "Enter";
        case KeyEvent.VK_ESCAPE: return "Esc";
        case KeyEvent.VK_TAB: return "Tab";
        case KeyEvent.VK_BACK_SPACE: return "Backspace";
        case KeyEvent.VK_DELETE: return "Delete";
        default: return String.format("typed \\u%04X", (int) c);
      }
    }

    private static char primaryLevelUnicode(KeyEvent e) {
      // TODO: 2024-06-20 find a legit way of distinguishing between Ctrl+j and Ctrl+Enter in X11
      String es = e.toString();
      String plu = "primaryLevelUnicode=";
      int l = es.indexOf(plu);
      if (l < 0) return 0;
      l += plu.length();
      int r = es.indexOf(',', l);
      if (r < 0) return 0;
      return (char) Integer.parseInt(es.substring(l, r));
    }

    @Override
    public void keyTyped(KeyEvent e) {
      char plu = primaryLevelUnicode(e);
      char c = e.getKeyChar();
      String key;
      if (c < 0x20 || c == 0x7F) {
        key = plu == c ? fromKeyChar(c) : new String(new char[]{plu});
        if (e.isShiftDown()) key = key.length() < 2 ? key.toUpperCase() : "Shift+" + key;
      } else key = new String(new char[]{c});
      StringBuilder keyNotation = new StringBuilder();
      if (e.isControlDown()) keyNotation.append("Ctrl+");
      if (e.isAltDown()) keyNotation.append("Alt+");
      eventListener.accept(keyNotation.append(key).toString());
    }

    private static String fromKeyCode(int keyCode) {
      switch (keyCode) {
        case KeyEvent.VK_CONTROL:
        case KeyEvent.VK_ALT:
        case KeyEvent.VK_SHIFT:
          return null;
        case KeyEvent.VK_F1: return "F1";
        case KeyEvent.VK_F2: return "F2";
        case KeyEvent.VK_F3: return "F3";
        case KeyEvent.VK_F4: return "F4";
        case KeyEvent.VK_F5: return "F5";
        case KeyEvent.VK_F6: return "F6";
        case KeyEvent.VK_F7: return "F7";
        case KeyEvent.VK_F8: return "F8";
        case KeyEvent.VK_F9: return "F9";
        case KeyEvent.VK_F10: return "F10";
        case KeyEvent.VK_F11: return "F11";
        case KeyEvent.VK_F12: return "F12";
        case KeyEvent.VK_LEFT: return "Left";
        case KeyEvent.VK_DOWN: return "Down";
        case KeyEvent.VK_UP: return "Up";
        case KeyEvent.VK_RIGHT: return "Right";
        case KeyEvent.VK_HOME: return "Home";
        case KeyEvent.VK_PAGE_UP: return "PageUp";
        case KeyEvent.VK_PAGE_DOWN: return "PageDown";
        case KeyEvent.VK_END: return "End";
        default: return String.format("pressed %04X", keyCode);
      }
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) return; // handled by keyTyped
      String key = fromKeyCode(e.getKeyCode());
      if (key == null) return;
      StringBuilder keyNotation = new StringBuilder();
      if (e.isControlDown()) keyNotation.append("Ctrl+");
      if (e.isAltDown()) keyNotation.append("Alt+");
      if (e.isShiftDown()) keyNotation.append("Shift+");
      eventListener.accept(keyNotation.append(key).toString());
    }

    @Override
    public void keyReleased(KeyEvent e) {}
  }

}
