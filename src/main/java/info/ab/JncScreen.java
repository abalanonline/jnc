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

package info.ab;

import ab.jnc.Sprite;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

@Getter @Setter
public class JncScreen {

  public static final int DEFAULT_WIDTH = 320;
  public static final int DEFAULT_HEIGHT = 240;

  int activePage;
  int visualPage;

  BufferedImage[] images = new BufferedImage[3];
//  private final List<BufferedImage> imageList = new ArrayList<BufferedImage>();
//  private final Clip soundfx;
//  private final Clip music;

  Button button;

  JncCanvas canvas;
  JncKeyListener keyListener;
  private final BufferedImage hwBitmap;
  //private BufferedImage imageBitmap;
  private final JFrame frame;
//  private final ImageIcon imageIcon;
//  private final Sprite imageIcon2;

  BlockingQueue<JncKeyEvent> keyEventQueue = new LinkedBlockingDeque<>();

  public JncScreen() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  @SneakyThrows
  public JncScreen(int width, int height) {
    hwBitmap = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_RGB);
    for (int i = 0; i < 3; i++) images[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

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
    frame.setSize(640 + 16, 480 + 39);
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

  public Sprite loadSprite(String resource) {
    return new Sprite(JncScreen.class.getResource(resource));
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
    }

    @Override
    protected void paintComponent(Graphics g) {

      BufferedImage bufferedImage = images[visualPage];

      if (bufferedImage.getWidth() < hwBitmap.getWidth() && bufferedImage.getHeight() < hwBitmap.getHeight()) {
        int dx = hwBitmap.getWidth() - bufferedImage.getWidth();
        int dy = hwBitmap.getHeight() - bufferedImage.getHeight();
        hwBitmap.getGraphics().drawImage(bufferedImage, dx >> 1, dy >> 1, null);
        bufferedImage = hwBitmap;
      }

      int sw = bufferedImage.getWidth();
      int sh = bufferedImage.getHeight();
      int dw = getWidth();
      int dh = getHeight();
      int x = 0;
      int y = 0;
      if (dw * sh > dh * sw) {
        dw = dh * sw / sh;
        x = (getWidth() - dw) / 2;
      } else {
        dh = dw * sh / sw;
        y = (getHeight() - dh) / 2;
      }
      Image newImage = bufferedImage.getScaledInstance(dw, dh, Image.SCALE_FAST);
      g.drawImage(newImage, x, y, null);
    }
  }

  @Getter @Setter
  public class JncKeyListener2 extends KeyAdapter {

    @Override
    public void keyPressed(KeyEvent e) {
      keyEventQueue.add(new JncKeyEvent(Instant.now(), e.getKeyCode()));
    }

  }
}
