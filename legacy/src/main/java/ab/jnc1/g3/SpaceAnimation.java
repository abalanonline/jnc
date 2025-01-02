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

class SpaceAnimation extends SpaceSpace {
  public static final int CAT_BASELINE_Y = 12;
  public final int[] SPARK_X = {40, 68, 83, 15, 72, 35};
  public final int[] SPARK_Y = {32, 23, 11, -11, -23, -33};
  public final int[] SPARK_F = {1, 3, 1, 4, 1, 2};
  public final int[][] SPARK_DX = new int[][]{
      {  2,  3,  2,  0, -2, -1,  2,  1,  0, -1, -2, -1},
      {  0, -1, -2, -1,  2,  3,  2,  0, -2, -1,  2,  1},
      {  2,  1,  0, -1, -2, -1,  2,  3,  2,  0, -2, -1},
      { -4, -3,  0,  1,  3,  5,  4,  3,  1,  1,  0, -2},
      { -2,  1,  2,  0, -2, -3, -2,  1,  2,  1,  0,  4},
      {  3,  2,  0, -2, -1,  2,  1,  0, -1, -2, -1,  2}};
  public final int[][] SPARK_DF = new int[][]{
      {  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0},
      {  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0},
      {  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0},
      {  1,  1,  1,  0,  0,  0,  0,  0,  0,  1,  1,  1},
      {  0,  4,  2,  0,  4,  2,  0,  4,  2,  0,  4,  4}, // backward with error in the last frame
      {  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0}};
  private Sprite star;

  // the world is created by the cat
  public SpaceAnimation(Physics physics, Sprite cat, Sprite spark, int x) {
    super(physics, null);
    setBounds(x, cat.y + CAT_BASELINE_Y, 84, Game3.WORLD_HEIGHT);
    color = new Color(0x00, 0x33, 0x66);
    color = new Color(rndB(color.getRed()), rndB(color.getGreen()), rndB(color.getBlue()));

    SpaceSpace sm;
    for (int i = 0; i < Game3.WORLD_HEIGHT; i += 12) {
      subSpace.add(new Space1x1(physics, new Rectangle(0, i, 84, 12), spark, i < 36 ? 0 : 1));
      subSpace.add(new Space1x1(physics, new Rectangle(0, -12 - i, 84, 12), spark, i < 36 ? 0 : 1));
    }

    this.star = spark;
  }

  @Override
  public void drawFg(Graphics graphics, Point distance, Physics physics) {
    super.drawFg(graphics, distance, physics);
    Sprite star = new Sprite(this.star);
    for (int i = 0; i < 6; i++) {
      final int currentFrame = physics.currentFrame % 12;
      star.setLocation(x + distance.x + SPARK_X[i] + SPARK_DX[i][currentFrame], y + distance.y + SPARK_Y[i]);
      star.currentFrame = (currentFrame + SPARK_F[i] + SPARK_DF[i][currentFrame]);
      star.drawImage();
    }
  }

}
