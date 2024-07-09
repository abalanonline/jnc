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

import lombok.Getter;
import lombok.Setter;

import javax.sound.sampled.Clip;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@Getter @Setter
public class JncKeyListener extends KeyAdapter {

  boolean leftPressed;
  boolean rightPressed;
  private final Clip soundfx;
  private final Clip music;

  public JncKeyListener(Clip soundfx, Clip music) {
    this.soundfx = soundfx;
    this.music = music;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_LEFT) setLeftPressed(true);
    if (e.getKeyCode() == KeyEvent.VK_RIGHT) setRightPressed(true);
    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
      soundfx.setFramePosition(0);
      soundfx.start();
    }
    if (e.getKeyCode() == KeyEvent.VK_UP) {
      music.setFramePosition(0);
      music.start();
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_LEFT) setLeftPressed(false);
    if (e.getKeyCode() == KeyEvent.VK_RIGHT) setRightPressed(false);
  }

}
