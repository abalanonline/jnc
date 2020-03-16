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

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

@Getter @Setter
public class JncScreen {

  Button button;

  private final Clip soundfx;
  private final Clip music;

  JncCanvas canvas;
  JncKeyListener keyListener;
  private final BufferedImage imageBitmap;
  private final JFrame frame;
  private final ImageIcon imageIcon;
  private final AnimatedImage imageIcon2;

  @SneakyThrows
  public JncScreen() {
    imageIcon = new ImageIcon(JncScreen.class.getResource("/player2.gif"));
    JLabel label = new JLabel(imageIcon);
    imageIcon2 = new AnimatedImage(JncScreen.class.getResource("/player2.gif"));

    imageBitmap = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = imageBitmap.createGraphics();
    graphics.setColor(Color.MAGENTA);
    graphics.fillRect(0, 0, 40, 30);

    //imageIcon.getIconWidth();
    //imageIcon.paintIcon(null, graphics, 0, 0);
    graphics.setColor(Color.MAGENTA);
    graphics.drawLine(2,2,8,8);
    graphics.drawString("AB", 2, 20);
    boolean b = graphics.drawImage(imageIcon.getImage(), 2, 30, null);
    graphics.dispose();

    canvas = new JncCanvas();
    canvas.addKeyListener(keyListener);
    canvas.requestFocus();

    frame = new JFrame();
    frame.add(canvas);
    //frame.add(label);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(640, 480);
    frame.setVisible(true);

    soundfx = AudioSystem.getClip();
    soundfx.open(AudioSystem.getAudioInputStream(JncScreen.class.getResource("/sound.wav")));
    music = AudioSystem.getClip();
    music.open(AudioSystem.getAudioInputStream(JncScreen.class.getResource("/music.wav")));
    //music.loop(Clip.LOOP_CONTINUOUSLY);
    //music.start(); // enable later

    keyListener = new JncKeyListener(soundfx, music);
    frame.addKeyListener(keyListener);
    canvas.addKeyListener(keyListener);

  }

  void putPixel(int x, int y, boolean enabled) {
    if (enabled) {
      Graphics2D graphics = imageBitmap.createGraphics();
      graphics.setColor(Color.BLUE);
      graphics.fillRect(0, 0, 40, 30);
      imageBitmap.getGraphics().drawImage(imageIcon2.getFrame(x), 6, 0, null);
    }
    imageBitmap.setRGB(x, y, (enabled ? Color.BLACK : Color.WHITE).getRGB());
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
      int sw = imageBitmap.getWidth();
      int sh = imageBitmap.getHeight();
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
      Image newImage = imageBitmap.getScaledInstance(dw, dh, Image.SCALE_FAST);
      g.drawImage(newImage, x, y, null);
    }
  }

}
