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

import javax.sound.sampled.Clip;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
    if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
    if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
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
    if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
    if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
  }

}
