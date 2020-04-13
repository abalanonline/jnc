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

package ab.jnc.g3;

import ab.MessageDigest;
import ab.jnc.Sprite;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class SpaceSpace extends Rectangle {

  protected Color color;
  protected List<SpaceSpace> subSpace = new ArrayList<>();
  protected List<Sprite> sprites = new ArrayList<>();
  protected final Random random;

  protected int nextInt(int i) {
    return random.nextInt(i);
  }

  protected int rndB(int i) {
    return Math.min(Math.max(i + nextInt(21) - 10, 0x00), 0xFF);
  }

  public SpaceSpace(Random initRandom, SpaceSpace superSpace) {
    random = MessageDigest.MD5.newDependentRandom(initRandom);
    color = (superSpace == null) ? Color.MAGENTA : superSpace.color;
    color = new Color(rndB(color.getRed()), rndB(color.getGreen()), rndB(color.getBlue()));
  }

  public void drawBg(Graphics graphics, Point distance) {
    Point translated = getLocation(); // new obj
    translated.translate(distance.x, distance.y);
    graphics.setColor(color);
    graphics.fillRect(translated.x, Game3.WORLD_HEIGHT - height - translated.y, width, height);
    subSpace.forEach(m -> m.drawBg(graphics, translated));
  }

  public void drawFg(Graphics graphics, Point distance) {
    Point translated = getLocation(); // new obj
    translated.translate(distance.x, distance.y);
    for (Sprite sprite : sprites) {
      final Point starLocation = sprite.getLocation();
      sprite.setLocation(sprite.x + translated.x, sprite.y + translated.y);
      sprite.drawImage();
      sprite.setLocation(starLocation);
    }
    subSpace.forEach(m -> m.drawFg(graphics, translated));
  }

}
