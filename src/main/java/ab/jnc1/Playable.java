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

import java.awt.*;
import java.time.Instant;
import java.util.List;

public interface Playable {

  /**
   * Loads the resources and prepare objects.
   * Called once before other methods. Can take time.
   */
  void load();

  /**
   * Calculate object changes. Called with even intervals.
   *
   * @param instant the sequential moment to be calculated
   * @param keys a list of keys pressed/released during interval
   * @return false if there is nothing else to do (true most of the time)
   */
  // TODO Speed up the planet and eliminate the leap seconds
  boolean tick(Instant instant, List<JncKeyEvent> keys);

  /**
   * Draw current objects on the provided graphics.
   * Should not change object, can be called from 1000 times a second to never.
   *
   * @param graphics is the drawable object
   */
  void draw(Graphics2D graphics);

}
