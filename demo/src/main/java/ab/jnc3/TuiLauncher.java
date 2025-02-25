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

import ab.tui.Tui;
import ab.tui.TuiConsole;

public class TuiLauncher {

  public static void main(String[] args) {
    Tui tui = new TuiConsole().open();
    Basic basic = new Basic3(null, tui);
    Launcher launcher = new Launcher();
    while (!launcher.stop) basic.load(launcher);
    tui.close();
  }

}
