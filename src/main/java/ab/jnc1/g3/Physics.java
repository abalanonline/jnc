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

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.Random;

@Getter @Setter
public class Physics {

  public final static Color MIDNIGHT_BLUE = new Color(0x00, 0x33, 0x66);
  public final static double GRAVITY_MOON = 1.62;

  private final Random random;
  private final String hash;

  private double spaceStart;
  private double spaceTransition;
  private double spaceSustain;

  private double jitter;
  private Color color = MIDNIGHT_BLUE;
  private double gravity = GRAVITY_MOON;
  private int currentFrame;

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
    p.setSpaceTransition(0);
    p.setSpaceSustain(168);
    p.setColor(MIDNIGHT_BLUE);
    p.setJitter(1);
    p.setGravity(0);
    return p;
  }

  public static Physics VANILLA() { // transition to real physics
    Physics p = new Physics(0);
    p.setSpaceTransition(168);
    p.setSpaceSustain(0);
    p.setColor(MIDNIGHT_BLUE);
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
    p.setJitter(mix(getJitter(), ext.getJitter(), ratio));
    p.setColor(mix(getColor(), ext.getColor(), ratio));
    return p;
  }
}
