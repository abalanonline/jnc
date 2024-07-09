/*
 * Copyright 2021 Aleksei Balan
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

package ab.jnc2;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Nibble {
  private final Nibble[] nodes;
  private final int[] offsets;
  private final boolean array;
  private byte[] bytes;
  private int offset;

  /**
   * External node.
   * @param size in bits
   */
  public Nibble(int size) {
    nodes = null;
    offsets = new int[]{size};
    array = false;
  }

  /**
   * Internal node.
   * @param nibbles children nodes
   */
  public Nibble(Nibble... nibbles) {
    nodes = nibbles;
    offsets = new int[nibbles.length + 1];
    for (int i = 0, o = 0; i < nibbles.length; i++) {
      o += nibbles[i].getSize();
      offsets[i + 1] = o;
    }
    array = false;
  }

  /**
   * Default nibble.
   */
  public Nibble() {
    this(4);
  }

  /**
   * Construct node with identical leafs.
   * Easy syntax: new Nibble(3, new Nibble()) = new Nibble(new Nibble(), new Nibble(), new Nibble())
   * @param size of similar nibbles
   * @param nibble nibble
   */
  public Nibble(int size, Nibble nibble) {
    array = true;
    nodes = new Nibble[]{nibble};
    offsets = new int[]{size, size * nibble.getSize()};
  }

  /**
   * Construct node with leafs.
   * Easy syntax: new Nibble(4, 3, 1) = new Nibble(new Nibble(4), new Nibble(3), new Nibble(1))
   * @param sizes of leafs.
   */
  public Nibble(int... sizes) {
    this(Arrays.stream(sizes).mapToObj(Nibble::new).toArray(Nibble[]::new));
  }


  /**
   * Get nibble from byte array.
   * @param bytes byte array
   * @param offset nibble offset in bits
   * @param path node numbers
   * @return nibble unsigned integer
   */
  int get(byte[] bytes, int offset, int... path) {
    assert (path.length == 0) == (nodes == null);
    if (path.length == 0) {
      int result = 0;
      int h = offset >> 3;
      int l = offset & 0x7;
      for (int i = getSize(), n; i > 0; i -= n) {
        n = 8 - l;
        int b = bytes[h] & ((1 << n) - 1);
        h += 1;
        l = 0;
        if (n > i) {
          b >>= n - i;
          n = i;
        }
        result <<= n;
        result |= b;
      }
      return result;
    }
    return nodes[array ? 0 : path[0]].get(bytes,
        (array ? path[0] * nodes[0].getSize() : offsets[path[0]]) + offset,
        Arrays.copyOfRange(path, 1, path.length));
  }

  int getSize() {
    return nodes == null ? offsets[0] : offsets[nodes.length];
  }

  private Nibble random(Random random) {
    bytes = new byte[getSize() / 8 + 1];
    random.nextBytes(bytes);
    return this;
  }

  public Nibble random() {
    return random(ThreadLocalRandom.current());
  }

  public Nibble random(long seed) {
    return random(new Random(seed));
  }

  public int get(int... path) {
    return get(bytes, 0, path);
  }
}
