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

package ab.jnc;

import ab.jnc.g3.Game3;

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
          JncKeyEvent keyEvent = screen.getKeyEventQueue().peek();
          while ((keyEvent != null) && (keyEvent.getInstant().isBefore(nextTick))) {
            JncKeyEvent key = screen.getKeyEventQueue().take();
            inTheLoop &= key.getKeyCode() != KeyEvent.VK_ESCAPE;
            tickKeyList.add(key);
            keyEvent = screen.getKeyEventQueue().peek();
          }
          inTheLoop &= game.tick(nextTick, tickKeyList);
          tick++;
          nextTick = startTime.plusMillis(tick * TICK_MS);
        }
        int activePage = screen.getActivePage() == 0 ? 1 : 0;
        screen.setActivePage(activePage);
        game.draw(screen.getGraphics()[activePage]);
        screen.setVisualPage(activePage);
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
