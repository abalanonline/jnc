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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Random;

public class PhysicsList extends LinkedList<Physics> implements Serializable {
  private static final long serialVersionUID = 0x4E43506879736963L;

  public final Random random;

  public PhysicsList(int randomSeed) {
    random = MessageDigest.MD5.newRandom(randomSeed, serialVersionUID);
  }

  @Override
  public boolean add(Physics physics) {
    physics.spaceStart = 0;
    if (!isEmpty()) {
      final Physics last = getLast();
      assert physics != last : "duplicate items in the list";
      physics.spaceStart = last.getSpaceStop(); // attach start point
    }
    return super.add(physics);
  }

  public Physics get(double index) {
    for (int i = size() - 1; i >= 0; i--) {
      Physics p = get(i);
      if ((p.spaceStart <= index) && (index < p.getSpaceStop())) {
        double t0 = p.spaceStart;
        double t1 = t0 + p.spaceTransition;
        if (index < t1) { // in transition
          return p.mix(get(i-1), (t1 - index) / (t1 - t0));
        } else { // in sustain
          return p;
        }
      }
    }
    throw new IndexOutOfBoundsException(String.format("Index: %.2f", index));
  }
}
