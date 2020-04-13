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

import ab.jnc.Sprite;

import java.awt.*;
import java.util.Random;

public class Space1x1 extends SpaceSpace {
  public Space1x1(Random initRandom, Rectangle rectangle, Sprite sprite, int spriteCount) {
    super(initRandom, null);
    color = new Color(0x00, 0x33, 0x66);
    setBounds(rectangle);
    for (int i = 0; i < spriteCount; i++) {
      Sprite s = new Sprite(sprite);
      s.setCurrentFrame(nextInt(6));
      s.x = nextInt(width);
      s.y = nextInt(height);
      sprites.add(s);
    }
  }
}
