/*
 * Copyright 2022 Aleksei Balan
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

import ab.jnc.JncKeyEvent;
import ab.jnc.Playable;
import ab.jnc2.Basic;
import ab.jnc2.GraphicsMode;
import ab.jnc2.GraphicsModeZx;
import ab.jnc2.Screen;
import ab.jnc2.TextFont;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Application implements Runnable, KeyListener {

  public static final String[] CLASSES = new String[]{ // TODO: 2022-02-27 class scan
      "ab.jnc2.SystemTerm",
      "ab.jnc2.BasicClock",
      "ab.jnc2.AmigaBall",
      "ab.jnc2.NohzDyve",
      "ab.jnc.g3.Game3",
      "ab.jnc2.GeodesicDome",
      "ab.jnc2.TyphoonGal",
      "ab.jnc2.BmpPattern",
      "ab.jnc2.CubicGrid",
  };

  Screen screen;
  GraphicsModeZx zxm;
  private TextFont textFont;
  private int selected;
  List<JncKeyEvent> keyEventList;
  private GraphicsMode newMode;
  private String newApplication;
  private Runnable program;
  String footer = "\u007F 2022 Apache License v2.0";

  /**
   * JNC2 applets can be run with this launcher to elevate its mode switching ability.
   * new Application(screen).run(program);
   * another way, simpler and more low-level
   * screen.flicker(50, program);
   *
   * @param screen screen
   */
  public Application(Screen screen) {
    this.screen = screen;
    this.zxm = new GraphicsModeZx();
    textFont = TextFont.ZX.get();
    if (screen != null) {
      screen.keyListener = this;
    }
  }

  public void menu(String title, String[] items, String footer, int selected) {
    int maxWidth = Arrays.stream(items).mapToInt(String::length).max().orElse(0);
    int maxHeight = items.length;
    int w = Math.max(14, (maxWidth + 3) & 0xFFFE);
    int h = Math.max(5, maxHeight + 1);
    int x = Math.max(0, 14 - w / 2);
    int y = Math.max(1, 11 - (h + 1) / 2);
    zxm.cls(0, 7); // dark black and white

    zxm.attrRect(x, y - 1, w, 1, GraphicsModeZx.WHITE, GraphicsModeZx.BLACK);
    textFont.print(zxm.pixel, title, x * 8, y * 8 - 8, -1);

    zxm.attrRect(x, y, w, h, GraphicsModeZx.BLACK, GraphicsModeZx.WHITE);
    zxm.clearRect(x * 8, y * 8, w * 8, h * 8, zxm.pixel, -1);
    zxm.clearRect(x * 8 + 1, y * 8, w * 8 - 2, h * 8 - 1, zxm.pixel, 0);
    for (int i = 0; i < items.length; i++) {
      textFont.print(zxm.pixel, items[i], x * 8 + 8, (y + i) * 8, -1);
    }
    zxm.attrRect(x, y + selected, w, 1, GraphicsModeZx.BLACK, GraphicsModeZx.CYAN);

    zxm.uiRainbow(x + w - 6, y - 1);

    String[] footers = footer.split("\n");
    for (int i = 0; i < footers.length; i++) {
      textFont.print(zxm.pixel, footers[i], 0, (i - footers.length) * 8 + GraphicsModeZx.HEIGHT, -1);
    }
  }

  @Override
  public void run() {
    menu("JNC2",
        Arrays.stream(CLASSES).map(s -> s.replaceAll("ab\\.jnc2?\\.", "")).toArray(String[]::new),
        footer, this.selected);
    zxm.draw(screen.image);
  }

  void run(String className) {
    Class<?> c;
    try {
      c = Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
    screen.setTitle(c.getSimpleName() + ".java");
    try {
      program = (Runnable) c.getDeclaredConstructor(Basic.class).newInstance(new Basic(screen));
      return;
    } catch (NoSuchMethodException e) {
      // try next constructor
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
    try {
      program = (Runnable) c.getDeclaredConstructor(Screen.class).newInstance(screen);
      return;
    } catch (NoSuchMethodException e) {
      // try next constructor
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
    try {
      Playable playable = (Playable) c.getDeclaredConstructor().newInstance();
      playable.load();
      screen.reset(GraphicsMode.DEFAULT);
      Graphics2D graphics = screen.image.createGraphics();
      keyEventList = new ArrayList<>();
      program = () -> {
        List<JncKeyEvent> list = keyEventList;
        keyEventList = new ArrayList<>();
        playable.tick(Instant.now(), list);
        playable.draw(graphics);
      };
    } catch (NoSuchMethodException e) {
      // try next constructor
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
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
      case KeyEvent.VK_ENTER:
        if (newApplication == null) {
          newApplication = CLASSES[selected];
          try {
            run(newApplication);
          } catch (IllegalStateException ex) {
            newApplication = null;
            footer = ex.getMessage();
          }
        }
        break;
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_UP:
        this.selected = (this.selected + CLASSES.length - 1) % CLASSES.length;
        break;
      case KeyEvent.VK_DOWN:
        this.selected = (this.selected + 1) % CLASSES.length;
        break;
    }
    if (keyEventList != null) {
      keyEventList.add(new JncKeyEvent(Instant.now(), e.getKeyCode(), false));
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    if (keyEventList != null) {
      keyEventList.add(new JncKeyEvent(Instant.now(), e.getKeyCode(), true));
    }
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.ZX);
    screen.setTitle("JNC2 Launcher");
    Application application = new Application(screen);
    application.run(application);
  }

  public void run(Runnable application) {
    program = application;
    if (application != this) {
      newApplication = application.getClass().getName();
    }

    screen.flicker(50, () -> {
      if ((newMode != null) && (newApplication != null)) {
        screen.reset(newMode);
        newMode = null;
        run(newApplication);
      }
      program.run();
    });
  }

}
