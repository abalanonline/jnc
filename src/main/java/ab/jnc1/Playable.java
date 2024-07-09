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
