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

public class Space1x1 extends SpaceSpace {
  public Space1x1(Physics physics, Rectangle rectangle, Sprite sprite, int spriteCount) {
    super(physics, null);
    color = physics.color;
    setBounds(rectangle);
    for (int i = 0; i < spriteCount; i++) {
      Sprite s = new Sprite(sprite);
      s.currentFrame = nextInt(6);
      s.x = nextInt(width);
      s.y = nextInt(height);
      sprites.add(s);
    }
  }
}
