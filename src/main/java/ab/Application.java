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

package ab;

import ab.jnc2.AmigaBall;
import ab.jnc2.GraphicsMode;
import ab.jnc2.Screen;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Application implements Runnable, KeyListener {

  GraphicsMode newMode;

  public static void main(String[] args) {
    new Application().run();
  }

  @Override
  public void run() {
    Screen screen = new Screen(GraphicsMode.ZX, this);
    Runnable basicProgram = new AmigaBall(screen.basic());
    while (true) {
      if (newMode != null) {
        screen.reset(newMode);
        newMode = null;
        basicProgram = new AmigaBall(screen.basic());
      }
      basicProgram.run();
      screen.repaint();
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {
    switch (e.getKeyChar()) {
      case '1': newMode = GraphicsMode.ZX; break;
      case '2': newMode = GraphicsMode.C64; break;
      case '3': newMode = GraphicsMode.CGA_16; break;
      case '4': newMode = GraphicsMode.CGA_HIGH; break;
      case '0': newMode = GraphicsMode.DEFAULT; break;
      case KeyEvent.VK_ESCAPE: System.exit(0);
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

}
