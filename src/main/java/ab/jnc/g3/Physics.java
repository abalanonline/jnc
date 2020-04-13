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
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Random;

@Getter @Setter
public class Physics {

  private final String hash;
  private final Random random;

  private int spaceStart;
  private int spaceTransition;
  private int spaceAction;
  private double jitter;

  public Physics(long seed) {
    random = MessageDigest.MD5.randomWithSalt(seed);
    hash = Long.toHexString(seed).substring(0, 4);
    spaceTransition = random.nextInt(256) + 64;
    spaceAction = random.nextInt(256) + 64;
  }

  public int getSpaceStop() {
    return spaceStart + spaceTransition + spaceAction;
  }

  @Override
  public String toString() {
    return "hsh:" + hash;
  }
}
