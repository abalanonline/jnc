/*
 * Copyright (C) 2025 Aleksei Balan
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

package ab.jnc3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CubicGrid3Test {

  @Test
  void hsl() {
    // saturation 1
    assertEquals(0xFF0000, new CubicGrid3.Color(CubicGrid3.rgb(0 / 6.0, 1, 0.5)).rgb());
    assertEquals(0xFFFF00, new CubicGrid3.Color(CubicGrid3.rgb(1 / 6.0, 1, 0.5)).rgb());
    assertEquals(0x00FF00, new CubicGrid3.Color(CubicGrid3.rgb(2 / 6.0, 1, 0.5)).rgb());
    assertEquals(0x00FFFF, new CubicGrid3.Color(CubicGrid3.rgb(3 / 6.0, 1, 0.5)).rgb());
    assertEquals(0x0000FF, new CubicGrid3.Color(CubicGrid3.rgb(4 / 6.0, 1, 0.5)).rgb());
    assertEquals(0xFF00FF, new CubicGrid3.Color(CubicGrid3.rgb(5 / 6.0, 1, 0.5)).rgb());
    // saturation 0
    assertEquals(0x000000, new CubicGrid3.Color(CubicGrid3.rgb(0.42, 0.42, 0)).rgb());
    assertEquals(0x3F3F3F, new CubicGrid3.Color(CubicGrid3.rgb(0.42, 0, 0.25)).rgb());
    assertEquals(0x7F7F7F, new CubicGrid3.Color(CubicGrid3.rgb(0.42, 0, 0.50)).rgb());
    assertEquals(0xBFBFBF, new CubicGrid3.Color(CubicGrid3.rgb(0.42, 0, 0.75)).rgb());
    assertEquals(0xFFFFFF, new CubicGrid3.Color(CubicGrid3.rgb(0.42, 0.42, 1)).rgb());
    // saturation 0.5
    assertEquals(0xBF3F3F, new CubicGrid3.Color(CubicGrid3.rgb(0 / 6.0, 0.5, 0.5)).rgb());
    assertEquals(0xBFBF3F, new CubicGrid3.Color(CubicGrid3.rgb(1 / 6.0, 0.5, 0.5)).rgb());
    assertEquals(0x3FBF3F, new CubicGrid3.Color(CubicGrid3.rgb(2 / 6.0, 0.5, 0.5)).rgb());
    assertEquals(0x3FBFBF, new CubicGrid3.Color(CubicGrid3.rgb(3 / 6.0, 0.5, 0.5)).rgb());
    assertEquals(0x3F3FBF, new CubicGrid3.Color(CubicGrid3.rgb(4 / 6.0, 0.5, 0.5)).rgb());
    assertEquals(0xBF3FBF, new CubicGrid3.Color(CubicGrid3.rgb(5 / 6.0, 0.5, 0.5)).rgb());
  }

  @Test
  void sxf() {
    CubicGrid3.Sxf sxf = new CubicGrid3.Sxf(new double[][]{{0, 4}, {1, 5}, {2, 6}, {3, 7}});
    assertArrayEquals(new double[]{0, 4}, sxf.get());
    sxf.timecode--;
    assertArrayEquals(new double[]{3, 7}, sxf.get());
    assertEquals(3, sxf.timecode);
    sxf.timecode += 1.5;
    assertArrayEquals(new double[]{0.5, 4.5}, sxf.get());
    sxf.timecode++;
    assertArrayEquals(new double[]{1.5, 5.5}, sxf.get());
    sxf.timecode++;
    assertArrayEquals(new double[]{2.5, 6.5}, sxf.get());
    sxf.timecode++;
    assertArrayEquals(new double[]{1.5, 5.5}, sxf.get());
    assertEquals(1, new CubicGrid3.Sxf(new double[][]{{0}, {1, 2}}).get().length); // don't crash
  }

}
