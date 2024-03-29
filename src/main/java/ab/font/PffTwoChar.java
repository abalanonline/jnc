/*
 * Copyright 2023 Aleksei Balan
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

package ab.font;

public class PffTwoChar {
  short width;
  short height;
  short xOffset;
  short yOffset;
  short deviceWidth;
  boolean[][] bitmap;

  public PffTwoChar(short width, short height) {
    this.width = width;
    this.height = height;
    bitmap = new boolean[height][];
    for (int i = 0; i < height; i++) {
      bitmap[i] = new boolean[width];
    }
  }
}
