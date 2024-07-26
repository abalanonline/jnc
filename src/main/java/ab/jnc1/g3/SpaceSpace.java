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

package ab.jnc1.g3;

import ab.jnc1.Sprite;

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

  public SpaceSpace(Physics physics, SpaceSpace superSpace) {
    random = MessageDigest.MD5.newDependentRandom(physics.random);
    color = (superSpace == null) ? Color.MAGENTA : superSpace.color;
    color = new Color(rndB(color.getRed()), rndB(color.getGreen()), rndB(color.getBlue()));
  }

  public void drawBg(Graphics graphics, Point distance, Physics physics) {
    Point translated = getLocation(); // new obj
    translated.translate(distance.x, distance.y);
    graphics.setColor(color);
    graphics.fillRect(translated.x, Game3.WORLD_HEIGHT - height - translated.y, width, height);
    subSpace.forEach(m -> m.drawBg(graphics, translated, physics));
  }

  public void drawFg(Graphics graphics, Point distance, Physics physics) {
    Point translated = getLocation(); // new obj
    translated.translate(distance.x, distance.y);
    for (Sprite sprite : sprites) {
      final Point starLocation = sprite.getLocation();
      final int starCurrentFrame = sprite.currentFrame;
      sprite.setLocation(sprite.x + translated.x, sprite.y + translated.y);
      sprite.currentFrame = starCurrentFrame + physics.currentFrame;
      sprite.drawImage();
      sprite.setLocation(starLocation);
      sprite.currentFrame = starCurrentFrame;
    }
    subSpace.forEach(m -> m.drawFg(graphics, translated, physics));
  }

}
