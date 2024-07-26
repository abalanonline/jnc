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

import java.awt.*;
import java.util.Random;

public class Physics {

  public final static Color MIDNIGHT_BLUE = new Color(0x00, 0x33, 0x66);
  public final static double GRAVITY_MOON = 1.62;

  public final Random random;
  private final String hash;

  public double spaceStart;
  public double spaceTransition;
  public double spaceSustain;

  public double jitter;
  public Color color = MIDNIGHT_BLUE;
  public double gravity = GRAVITY_MOON;
  public int currentFrame;

  private Physics(Random random, String hash) {
    this.random = random;
    this.hash = hash;
  }

  public Physics(long randomSeed) {
    random = MessageDigest.MD5.newRandom(randomSeed, 0);
    hash = Long.toHexString(randomSeed | 0xFFFFFFFF00000000L).substring(12, 16);
    spaceTransition = nextInt(256) + 64;
    spaceSustain = nextInt(256) + 64;
    //color = new Color(nextInt(4) * 0x55, nextInt(4) * 0x55, nextInt(4) * 0x55);
  }

  public static Physics ANIMATION() { // starting animation
    Physics p = new Physics(0);
    p.spaceTransition = 0;
    p.spaceSustain = 168;
    p.color = MIDNIGHT_BLUE;
    p.jitter = 1;
    p.gravity = 0;
    return p;
  }

  public static Physics VANILLA() { // transition to real physics
    Physics p = new Physics(0);
    p.spaceTransition = 168;
    p.spaceSustain = 0;
    p.color = MIDNIGHT_BLUE;
    return p;
  }

  protected int nextInt(int i) {
    return random.nextInt(i);
  }

  public double getSpaceStop() {
    return spaceStart + spaceTransition + spaceSustain;
  }

  @Override
  public String toString() {
    String s = "hsh:" + hash + "\n";
    String c = Integer.toHexString(color.getRGB());
    s += "col:" + c.charAt(2) + c.charAt(4) + c.charAt(6) + "\n";
    s += String.format("jit:%.2f%n", jitter);
    return s;
  }

  public static double mix(double x, double y, double a) {
    return x * (1 - a) + y * a;
  }

  public static int mix(int x, int y, double a) {
    return (int) Math.floor(mix((double) x, (double) y, a) + 0.5);
  }

  public static Color mix(Color x, Color y, double a) {
    return new Color(
        mix(x.getRed(), y.getRed(), a),
        mix(x.getGreen(), y.getGreen(), a),
        mix(x.getBlue(), y.getBlue(), a),
        mix(x.getAlpha(), y.getAlpha(), a));
  }

  public Physics mix(Physics ext, double ratio) {
    assert ratio >= 0; assert ratio <= 1;
    final Physics p = new Physics(random, "");
    p.jitter = mix(this.jitter, ext.jitter, ratio);
    p.color = mix(this.color, ext.color, ratio);
    return p;
  }
}
