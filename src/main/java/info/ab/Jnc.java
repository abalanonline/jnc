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

import ab.jnc.Game1;
import ab.jnc.Playable;
import lombok.SneakyThrows;

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
    Playable game = new Game1();
    game.loadResources();
    JncScreen screen = new JncScreen(game.getWidth(), game.getHeight());
    try {
      game.initHardware(screen);
      while (game.update()) {
        screen.update();
        Thread.sleep(10); // smooth
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      screen.close();
    }
  }

  public static void main(String[] args) {
    new Jnc(args).run();
  }

}
