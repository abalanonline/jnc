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

import ab.jnc1.g3.Game3;

import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Jnc implements Runnable {

  public Jnc(String[] args) {
  }
//  public BufferedImage flip(BufferedImage bufferedImage) {
//    BufferedImage flippedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getTransparency());
//    Graphics2D g2d = flippedImage.createGraphics();
//    g2d.setTransform(AffineTransform.getScaleInstance(1, -1));
//    g2d.drawImage(bufferedImage, 0, -bufferedImage.getHeight(), null);
//    g2d.dispose();
//    return flippedImage;
//  }

  @Override
  public void run() {
    Playable game = new Game3();
    game.load();
    JncScreen screen = new JncScreen();
    try {
      final int TICK_MS = 20; // 50 Hz
      Instant startTime = Instant.now();
      Instant nextTick = startTime;
      int tick = 0;
      boolean inTheLoop = true;
      while (inTheLoop) {
        while (nextTick.isBefore(Instant.now())) {
          // keys processing // if key pressed before nextTick
          List<JncKeyEvent> tickKeyList = new ArrayList<>();
          JncKeyEvent keyEvent = screen.keyEventQueue.peek();
          while ((keyEvent != null) && (keyEvent.instant.isBefore(nextTick))) {
            JncKeyEvent key = screen.keyEventQueue.take();
            inTheLoop &= key.keyCode != KeyEvent.VK_ESCAPE;
            tickKeyList.add(key);
            keyEvent = screen.keyEventQueue.peek();
          }
          inTheLoop &= game.tick(nextTick, tickKeyList);
          tick++;
          nextTick = startTime.plusMillis(tick * TICK_MS);
        }
        int activePage = screen.activePage == 0 ? 1 : 0;
        screen.activePage = activePage;
        game.draw(screen.graphics[activePage]);
        screen.visualPage = activePage;
        screen.update();
        Thread.sleep(10); // smooth
      }
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      screen.close();
    }
  }

  public static void main(String[] args) {
    new Jnc(args).run();
  }

}
