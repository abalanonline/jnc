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

import ab.tns.VirMidi;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static ab.jnc3.CubicGrid3.Control.*;

/**
 * CubicGrid redux.
 * OpenGL coordinate system, y-axis upwards, z-axis towards the camera
 */
public class CubicGrid3 implements BasicApp {

  public static final int SPEED = 127 - 50;
  public static final double dB = Math.log(10) / 10; // 10 units change the value 10 times
  private VirMidi controller;

  public CubicGrid3() {
    controller = new VirMidi();
  }

  @Override
  public TextMode preferredMode() {
    return TextMode.defaultMode();
  }

  public enum Control { // TODO: read controller settings from config
    MASTER(10, SPEED, 9), PAUSE(11, 20),
    SATURATION(7, 0), HUE(8, 0);
    public final int controlChange;
    public final int defaultValue;
    public final int controlChangeAlt;
    Control(int controlChange, int defaultValue, int controlChangeAlt) {
      this.controlChange = controlChange;
      this.defaultValue = defaultValue;
      this.controlChangeAlt = controlChangeAlt;
    }
    Control(int controlChange, int defaultValue) {
      this(controlChange, defaultValue, 0);
    }
  }

  private int get(Control control) {
    int cc = control.controlChange;
    int cca = control.controlChangeAlt;
    if (cca > 0 && controller.controlChangeTime[cca] > controller.controlChangeTime[cc]) cc = cca;
    byte v = controller.controlChange[cc];
    return v == 0 ? control.defaultValue : v;
  }

  /**
   * 0 - default, 1-26 = -0.3--0.1, 27 = 0, 28-77-127 = 0.1-1-10
   */
  private double getSpeed(Control control) {
    int v = get(control);
    if (v == 27) return 0;
    int sign = v > 27 ? 1 : -1;
    int a = Math.abs(v - 27) - 50;
    double speed = sign * Math.exp(a * dB / 5);
    return speed;
  }

  /**
   * all values 0-1
   * copy pasted from https://en.wikipedia.org/wiki/HSL_and_HSV#Color_conversion_formulae
   */
  public static int rgb(double hue, double saturation, double lightness) {
    assert hue >= 0 && hue < 1 && saturation >= 0 && saturation <= 1 && lightness >= 0 && lightness <= 1;
    double c = (1 - Math.abs(2 * lightness - 1)) * saturation;
    double h1 = hue * 6;
    double x = c * (1 - Math.abs(h1 % 2 - 1));
    double r1 = 0, g1 = 0, b1 = 0;
    switch ((int) h1) {
      case 0: r1 = c; g1 = x; break;
      case 1: r1 = x; g1 = c; break;
      case 2: g1 = c; b1 = x; break;
      case 3: g1 = x; b1 = c; break;
      case 4: b1 = c; r1 = x; break;
      case 5: b1 = x; r1 = c; break;
    }
    double m = lightness - c / 2;
    int r = (int) ((r1 + m) * 255.9);
    int g = (int) ((g1 + m) * 255.9);
    int b = (int) ((b1 + m) * 255.9);
    return (r << 16) | (g << 8) | b;
  }

  /**
   * https://en.wikipedia.org/wiki/Linear_interpolation#Programming_language_support
   */
  public static double mix(double v0, double v1, double t) {
    return (1 - t) * v0 + t * v1;
  }

  public static Dot mix(Dot v0, Dot v1, double t) {
    final Color c0 = v0.color;
    final Color c1 = v1.color;
    return new Dot(mix(v0.x, v1.x, t), mix(v0.y, v1.y, t), mix(v0.z, v1.z, t),
        new Color(mix(c0.hue, c1.hue, t), mix(c0.saturation, c1.saturation, t), mix(c0.lightness, c1.lightness, t)));
  }

  /**
   * Create a cube of dots.
   * @param dots 8 dots defining the color of the cube and its position in space
   * @param size x size x size - the cube size
   * @param fine array of 3 doubles 0-1 shifting the dots of the cube along the axises
   * @return the unordered array of dots forming the cube
   */
  public static Dot[] cube(Dot[] dots, int size, double[] fine) {
    Dot[] dz = dots;
    Dot[] dy = new Dot[4];
    Dot[] dx = new Dot[2];
    dots = new Dot[size * size * size];
    for (int z = 0, dp = 0; z < size; z++) {
      double tz = (double) z / (size - 1);
      for (int i = 0; i < 4; i++) dy[i] = mix(dz[i], dz[i + 4], tz);
      for (int y = 0; y < size; y++) {
        double ty = (double) y / (size - 1);
        for (int i = 0; i < 2; i++) dx[i] = mix(dy[i], dy[i + 2], ty);
        for (int x = 0; x < size; x++) dots[dp++] = mix(dx[0], dx[1], (double) x / (size - 1));
      }
    }
    return dots;
  }

  @Override
  public void open(Basic basic) {
    int colSize = 8;
    double[][] colFilm = new double[colSize][];
    Random random = ThreadLocalRandom.current();
    for (int i = 0; i < colSize; i++) {
      double[] f = new double[24];
      for (int j = 0; j < 24; j++) f[j] = random.nextDouble();
      colFilm[i] = f;
    }
    Sxf colSxf = new Sxf(colFilm);
    Dimension size = basic.getSize();
    Point center = new Point(size.width / 2, size.height / 2);
    int time = 0; // ms
    controller.open();
    while (true) {
      basic.cls();
      // cube
      double[] cv = colSxf.get();
      Dot[] dots = new Dot[8];
      for (int i = 0; i < 8; i++) {
        Color color = new Color(cv[i * 3], cv[i * 3 + 1], cv[i * 3 + 2]);
        Dot dot = new Dot((i & 1) == 0 ? -1 : 1, (i & 2) == 0 ? -1 : 1, (i & 4) == 0 ? -1 : 1, color);
        dots[i] = dot;
      }
      dots = cube(dots, 11, null);
      Arrays.stream(dots).sorted(Comparator.comparingDouble(a -> a.z)).forEach(dot -> {
        double z = (6 - dot.z) / 600;
        int y = (int) Math.round(dot.y / z) + center.y;
        if (y < 0 || y >= size.height) return;
        int x = (int) Math.round(dot.x / z) + center.x;
        if (x < 0 || x >= size.width) return;
        basic.ink(basic.getColorFromRgb(dot.color.rgb()));
        basic.plot(x, y);
      });
      // controller debug
      basic.ink(basic.getColorFromRgb(0xFFFFFF));
      byte controlChangeLast = controller.controlChangeLast;
      basic.printAt(4, 0, String.format("%3d: %3d", controlChangeLast, controller.controlChange[controlChangeLast]));
      basic.printAt(4, 1, Long.toString(time));
      // time spinner
      basic.ink(basic.getColorFromRgb(rgb(get(HUE) / 128.0, get(SATURATION) / 128.0, 0.5)));
      double angle = 2 * Math.PI * time / 1000;
      basic.plot(8, size.height - 8);
      basic.draw(8 + (int) Math.round(Math.sin(angle) * 7),
          size.height - 8 + (int) Math.round(Math.cos(angle) * 7));
      // done
      basic.update();
      int p = get(PAUSE);
      time += p * getSpeed(MASTER);
      colSxf.timecode += p * getSpeed(MASTER) / 1000.0;
      basic.pause(p);
    }
  }

  @Override
  public void close() {
    controller.close();
  }

  /**
   * Snapshots with a crossfade. Dumb name. And worse yet, they loop endlessly.
   */
  public static class Sxf {
    private final double[][] film;
    public double timecode;

    public Sxf(double[][] film) {
      this.film = film; // deep copy
    }

    public double[] get() {
      int length = film.length;
      timecode = timecode % length;
      if (timecode < 0) timecode += length;
      int ti = (int) timecode;
      double tr = timecode - ti;
      double[] f0 = film[ti];
      double[] f1 = film[(ti + 1) % length];
      int n = Math.min(f0.length, f1.length);
      double[] f = new double[n];
      for (int i = 0; i < n; i++) f[i] = mix(f0[i], f1[i], tr);
      return f;
    }

  }

  public static class Color {
    public final double hue;
    public final double saturation;
    public final double lightness;

    public Color(double hue, double saturation, double lightness) {
      this.hue = hue;
      this.saturation = saturation;
      this.lightness = lightness;
    }

    public int rgb() {
      return CubicGrid3.rgb(hue, saturation, lightness);
    }

  }

  public static class Dot {
    public final double x;
    public final double y;
    public final double z;
    public final Color color;

    public Dot(double x, double y, double z, Color color) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.color = color;
    }
  }

  public static void main(String[] args) {
    new Basic3(new Screen(), null).loadAndClose(new CubicGrid3());
  }

}
