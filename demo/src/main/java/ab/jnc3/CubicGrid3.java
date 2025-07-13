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
    int a = Math.abs(v - 27);
    double speed = sign * Math.exp((a - 50) * dB / 5);
    if (a < 20) speed *= a / 20.0;
    return speed;
  }

  /**
   * all values 0-1
   * copy pasted from https://en.wikipedia.org/wiki/HSL_and_HSV#Color_conversion_formulae
   */
  public static double[] rgb(double hue, double saturation, double lightness) {
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
    return new double[]{r1 + m, g1 + m, b1 + m};
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
        new Color(mix(c0.r, c1.r, t), mix(c0.g, c1.g, t), mix(c0.b, c1.b, t)));
  }

  public Dot rotate(Dot dot, double yaw, double pitch, double roll) {
    double[] d = {dot.x, dot.y, dot.z};
    double[] a = {2 * Math.PI * roll, 2 * Math.PI * pitch, 2 * Math.PI * yaw};
    for (int i = 0; i < 3; i++) {
      final double s = Math.sin(a[i]);
      final double c = Math.cos(a[i]);
      int i1 = (i + 1) % 3;
      double x = d[i];
      double y = d[i1];
      d[i] = x * c - y * s;
      d[i1] = x * s + y * c;
    }
    return new Dot(d[0], d[1], d[2], dot.color);
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
      for (int j = 0; j < 24;) {
        double h = (random.nextDouble() + random.nextDouble() + 0.1) % 1;
        double s = 0.75 + random.nextDouble() / 4;
        double l = 0.50 + random.nextDouble() / 5;
        double[] rgb = rgb(h, s, l);
        f[j++] = rgb[0];
        f[j++] = rgb[1];
        f[j++] = rgb[2];
      }
      colFilm[i] = f;
    }
    Sxf colSxf = new Sxf(colFilm);
    Dimension size = basic.getSize();
    Point center = new Point(size.width / 2, size.height / 2);
    Dimension displayAspectRatio = basic.getDisplayAspectRatio();
    int displayHeight = Math.min(displayAspectRatio.width, displayAspectRatio.height);
    Dimension square = new Dimension(size.width * displayHeight / displayAspectRatio.width,
        size.height * displayHeight / displayAspectRatio.height);
    double time = 0; // ms
    controller.open();
    while (true) {
      basic.paper(basic.getColorFromRgb(0));
      basic.cls();
      // cube
      double[] cv = colSxf.get();
      Dot[] dots = new Dot[8];
      for (int i = 0; i < 8; i++) {
        Color color = new Color(cv[i * 3], cv[i * 3 + 1], cv[i * 3 + 2]);
        Dot dot = new Dot((i & 1) == 0 ? -1 : 1, (i & 2) == 0 ? -1 : 1, (i & 4) == 0 ? -1 : 1, color);
        dots[i] = dot;
      }
      double ay = time / 1200.0;
      double ap = time / 1400.0;
      double ar = time / 1000.0; // Amanite FX Daishonin
      for (int i = 0; i < 8; i++) dots[i] = rotate(dots[i], ay, ap, ar);
      dots = cube(dots, 16, null);
      Arrays.stream(dots).sorted(Comparator.comparingDouble(a -> a.z)).forEach(dot -> {
        double z = (6 - dot.z) / 2;
        int y = (int) Math.round(dot.y / z * square.height) + center.y;
        if (y < 0 || y >= size.height) return;
        int x = (int) Math.round(dot.x / z * square.width) + center.x;
        if (x < 0 || x >= size.width) return;
        double brightness = Math.min(Math.max(0, dot.z / 3 + 0.6), 1);
        basic.ink(basic.getColorFromRgb(dot.color.rgb(brightness)));
        basic.plot(x, y);
      });
      // controller debug
      basic.ink(basic.getColorFromRgb(0xAAAAAA));
      byte controlChangeLast = controller.controlChangeLast;
      basic.printAt(4, 0, String.format("%3d: %3d", controlChangeLast, controller.controlChange[controlChangeLast]));
      basic.printAt(4, 1, String.format("%.1f", time));
      // time spinner
      basic.ink(basic.getColorFromRgb(new Color(rgb(get(HUE) / 128.0, get(SATURATION) / 128.0, 0.5)).rgb()));
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
    public final double r;
    public final double g;
    public final double b;

    public Color(double... rgb) {
      r = rgb[0];
      g = rgb[1];
      b = rgb[2];
    }

    public static int rgb(double r, double g, double b) {
      int ir = (int) (r * 255.9);
      int ig = (int) (g * 255.9);
      int ib = (int) (b * 255.9);
      return (ir << 16) | (ig << 8) | ib;
    }

    public int rgb() {
      return rgb(r, g, b);
    }

    public int rgb(double brightness) {
      return rgb(r * brightness, g * brightness, b * brightness);
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

    @Override
    public String toString() {
      return String.format("%s, %s, %s, #%06X", x, y, z, color.rgb());
    }
  }

  public static void main(String[] args) {
    new Basic3(new Screen(), null).loadAndClose(new CubicGrid3());
  }

}
