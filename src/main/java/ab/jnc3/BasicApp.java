/*
 * Copyright (C) 2024 Aleksei Balan
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

import ab.jnc2.GraphicsMode;

public interface BasicApp extends Runnable, AutoCloseable {
  /**
   * @return null if no preferences
   */
  GraphicsMode preferredMode();
  /**
   * After closing, the app can be re-opened with a different basic.
   * Preserving the internal state is not required.
   */
  void open(Basic basic);
  void close();
}
