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
