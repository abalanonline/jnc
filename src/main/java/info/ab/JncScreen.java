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

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.io.File;

@Getter @Setter
public class JncScreen extends Frame {

  Button button;
  private final JncWindowAdapter windowAdapter;

  private final Clip soundfx;
  private final Clip music;

  boolean[][] bitmap = new boolean[40][30];
  JncCanvas canvas;
  @Getter
  JncKeyListener keyListener;

  @SneakyThrows
  public JncScreen() {
    soundfx = AudioSystem.getClip();
    soundfx.open(AudioSystem.getAudioInputStream(new File("sound.wav")));
    music = AudioSystem.getClip();
    music.open(AudioSystem.getAudioInputStream(new File("music.wav")));
    music.loop(Clip.LOOP_CONTINUOUSLY);
    music.start();
    keyListener = new JncKeyListener(soundfx, music);

    bitmap[10][10] = true;

    windowAdapter = new JncWindowAdapter();
    addWindowListener(windowAdapter);

    canvas = new JncCanvas();
    canvas.addKeyListener(keyListener);
    this.add(canvas);
    canvas.requestFocus();

    this.setSize(640, 480);
    this.setLayout(null);
    this.setVisible(true);

    addKeyListener(keyListener);
  }

  void putPixel(int x, int y, boolean enabled) {
    bitmap[x][y] = enabled;
  }

  public boolean isAvailable() {
    boolean windowClosing = windowAdapter.isWindowClosing();
    if (windowClosing) dispose();
    return !windowClosing;
  }

  class JncCanvas extends Canvas {
    public JncCanvas() {
      setSize(640, 480);
    }

    @Override
    public void paint(Graphics g) {
      for (int x = 0; x < 40; x++) {
        for (int y = 0; y < 30; y++) {
          g.setColor(bitmap[x][y] ? Color.BLACK : Color.WHITE);
          g.fillRect(x * 16, y * 16, x * 16 + 16, y * 16 + 16);
        }
      }
    }
  }

}
