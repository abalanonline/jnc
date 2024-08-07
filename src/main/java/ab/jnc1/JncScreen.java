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

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class JncScreen {

  public static final int DEFAULT_WIDTH = 320;
  public static final int DEFAULT_HEIGHT = 240;

  int activePage;
  int visualPage;

  BufferedImage[] images = new BufferedImage[3];
  Graphics2D[] graphics = new Graphics2D[3];
//  private final List<BufferedImage> imageList = new ArrayList<BufferedImage>();
//  private final Clip soundfx;
//  private final Clip music;

  Button button;

  JncCanvas canvas;
  JncKeyListener keyListener;
  //private BufferedImage imageBitmap;
  private final JFrame frame;
//  private final ImageIcon imageIcon;
//  private final Sprite imageIcon2;

  public BlockingQueue<JncKeyEvent> keyEventQueue = new LinkedBlockingDeque<>();

  public JncScreen() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public JncScreen(int width, int height) {
    for (int i = 0; i < 3; i++) {
      images[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      graphics[i] = images[i].createGraphics();
    }

//    imageIcon = new ImageIcon(JncScreen.class.getResource("/player2.gif"));
//    JLabel label = new JLabel(imageIcon);
//    imageIcon2 = new Sprite(JncScreen.class.getResource("/player2.gif"));

//    Graphics2D graphics = imageBitmap.createGraphics();
//    graphics.setColor(Color.MAGENTA);
//    graphics.fillRect(0, 0, 40, 30);

    //imageIcon.getIconWidth();
    //imageIcon.paintIcon(null, graphics, 0, 0);
//    graphics.setColor(Color.MAGENTA);
//    graphics.drawLine(2,2,8,8);
//    graphics.drawString("AB", 2, 20);
//    boolean b = graphics.drawImage(imageIcon.getImage(), 2, 30, null);
//    graphics.dispose();

    canvas = new JncCanvas();
    canvas.addKeyListener(keyListener);
    canvas.requestFocus();

    frame = new JFrame();
    frame.add(canvas);
    //frame.add(label);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(640 + 18, 480 + 41);
    frame.setVisible(true);

//    soundfx = AudioSystem.getClip();
//    soundfx.open(AudioSystem.getAudioInputStream(JncScreen.class.getResource("/sound.wav")));
//    music = AudioSystem.getClip();
//    music.open(AudioSystem.getAudioInputStream(JncScreen.class.getResource("/music.wav")));
    //music.loop(Clip.LOOP_CONTINUOUSLY);
    //music.start(); // enable later

    JncKeyListener2 keyListener = new JncKeyListener2();
    frame.addKeyListener(keyListener);
    canvas.addKeyListener(keyListener);

  }

  public void update() {
    frame.repaint();
  }

  public void close() {
    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
  }

  public void putPixel(int x, int y, boolean enabled) {
//    if (enabled) {
//      Graphics2D graphics = imageBitmap.createGraphics();
//      graphics.setColor(Color.BLUE);
//      graphics.fillRect(0, 0, 40, 30);
//      imageBitmap.getGraphics().drawImage(imageIcon2.getFrame(x), 6, 0, null);
//    }
    images[activePage].setRGB(x, y, (enabled ? Color.BLACK : Color.WHITE).getRGB());
  }

  public void putSprite(int x, int y, Sprite sprite) {
    images[activePage].getGraphics().drawImage(sprite.getFrame(0), x, y, null);
  }

  public boolean isAvailable() {
    return true;
  }

  class JncCanvas extends JButton {
    public JncCanvas() {
      super(" ");
      setEnabled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {

      int COMPONENT_BORDER = 1; // not allowed to draw on this
      // our new dimension is a final one, ok
      final Dimension viewPortSize = new Dimension(getWidth() - 2 * COMPONENT_BORDER, getHeight() - 2 * COMPONENT_BORDER);

      BufferedImage bufferedImage = images[visualPage];

      int sw = bufferedImage.getWidth();
      int sh = bufferedImage.getHeight();
      int dw = viewPortSize.width;
      int dh = viewPortSize.height;
      double zoom = dw * sh > dh * sw ? (double) dh / sh : (double) dw / sw;
      zoom = Math.floor(zoom); if (zoom < 1) zoom = 1; // this line asserts quality but it can be commented out
      dw = (int) (sw * zoom);
      dh = (int) (sh * zoom);
      int x = (viewPortSize.width - dw) / 2;
      int y = (viewPortSize.height - dh) / 2;

      Image newImage = bufferedImage.getScaledInstance(dw, dh, Image.SCALE_FAST);
      g.drawImage(newImage, x + COMPONENT_BORDER, y + COMPONENT_BORDER, null);
    }
  }

  public class JncKeyListener2 extends KeyAdapter {

    @Override
    public void keyPressed(KeyEvent e) {
      keyEventQueue.add(new JncKeyEvent(Instant.now(), e.getKeyCode(), false));
      super.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
      keyEventQueue.add(new JncKeyEvent(Instant.now(), e.getKeyCode(), true));
      super.keyReleased(e);
    }
  }
}
