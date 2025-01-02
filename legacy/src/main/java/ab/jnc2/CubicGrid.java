/*
 * Copyright (C) 2021 Aleksei Balan
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

package ab.jnc2;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class CubicGrid implements Runnable {

  public static final int SPACING = 20;
  public static final double SPEED = 0.03;

  private final int width;
  private final int height;
  Basic basic;
  int centerx;
  int centery;
  double radius;
  double rx;
  double ry;
  private RndAcc rndAcc = new RndAcc(30, 5, SPEED);

  public CubicGrid(Basic basic) {
    this.basic = basic;
    basic.paper(basic.getColorFromRgb(0));
    width = basic.getWidth();
    height = basic.getHeight();
    centerx = width / 2;
    centery = height / 2;
    radius = basic.getHeight() * basic.getPixelHeight() / 2;
    rx = Math.max(radius, radius * basic.getPixelHeight());
    ry = Math.max(radius, radius / basic.getPixelHeight());
  }

  public void trimPlot(double x, double y) {
    if (x < 0 || x >= width || Double.isInfinite(x) ||
        y < 0 || y >= height || Double.isInfinite(y)) return;
    basic.plot((int) Math.floor(x), (int) Math.floor(y));
  }

  public double px(double x, double z) {
    return rx * x / z + centerx;
  }

  public double py(double y, double z) {
    return ry * y / z + centery;
  }

  public void rotate(double[][] d, int i0, int i1, double angle) {
    angle =  2 * Math.PI * angle;
    final double s = Math.sin(angle);
    final double c = Math.cos(angle);
    for (int i = d[0].length - 1; i >= 0; i--) {
      double x = d[i0][i];
      double y = d[i1][i];
      d[i0][i] = s * y + c * x;
      d[i1][i] = c * y - s * x;
    }
  }

  public void cube(double polar, double azimuthal, double third) {
    double[][] cubeXyz = {{-1, -1, -1, -1, 1, 1, 1, 1}, {-1, -1, 1, 1, -1, -1, 1, 1}, {-1, 1, -1, 1, -1, 1, -1, 1},
        {0, 1, 0, 1, 0, 1, 0, 1}, {0, 0, 1, 1, 0, 0, 1, 1}, {0, 0, 0, 0, 1, 1, 1, 1}};
    rotate(cubeXyz, 1, 0, polar);
    rotate(cubeXyz, 0, 2, azimuthal);
    rotate(cubeXyz, 2, 1, third);
    for (int i = 3, k = 6; i < 6; i++) {
      for (int j = 0; j < 8; j++) {
        cubeXyz[i][j] = (rndAcc.get(k++, 1) + 1) / 2;
      }
    }
//    for (int i = 0; i < 8; i++) {
//      final double dz = cubeXyz[0][i] + 4;
//      final double dy = cubeXyz[1][i];
//      final double dx = cubeXyz[2][i];
//      basic.ink((int) (cubeXyz[3][i] * 0xFF0000 + cubeXyz[4][i] * 0xFF00 + cubeXyz[5][i] * 0xFF));
//      trimPlot(px(dx, dz), py(dy, dz));
//    }
    final int spacing = SPACING;
    final int spacing3 = spacing * spacing * spacing;
    final double[] vt = new double[cubeXyz.length];
    Predicate<Void> vtPass = a -> vt[0] > -1 && vt[0] < 1;
    final List<Dot> dots = new ArrayList<>();
    double xd = rndAcc.get(3, 1) / 4;
    double yd = rndAcc.get(4, 1) / 4;
    double zd = rndAcc.get(5, 1) / 4;
    for (int zi = 0; zi <= spacing; zi++) {
      double z0 = zi;
      double z1 = spacing - zi;
      for (int yi = 0; yi <= spacing; yi++) {
        double y0 = yi;
        double y1 = spacing - yi;
        for (int xi = 0; xi <= spacing; xi++) {
          double x0 = xi;
          double x1 = spacing - xi;
          for (int i = 0; i < cubeXyz.length; i++) {
            final double[] v = cubeXyz[i];
            vt[i] = v[0] * z1 * y1 * x1 +
                    v[1] * z0 * y1 * x1 +
                    v[2] * z1 * y0 * x1 +
                    v[3] * z0 * y0 * x1 +
                    v[4] * z1 * y1 * x0 +
                    v[5] * z0 * y1 * x0 +
                    v[6] * z1 * y0 * x0 +
                    v[7] * z0 * y0 * x0;
            vt[i] /= spacing3;
            if (i == 0 && !vtPass.test(null)) break;
          }
          if (!vtPass.test(null)) continue;
          final double dz = vt[0] + 3;
          final double dy = vt[1] * 5 + yd;
          final double dx = vt[2] * 5 + xd;
          final int brightness = Math.min(Math.max(0, (int) Math.round(0xFF * (1 - vt[0]))), 0xFF);
          final int cr = (int) (vt[3] * brightness);
          final int cg = (int) (vt[4] * brightness);
          final int cb = (int) (vt[5] * brightness);
          dots.add(new Dot(px(dx, dz), py(dy, dz), dz, cr << 16 | cg << 8 | cb));
        }
      }
    }
    dots.stream().sorted().forEach(dot -> {
      basic.ink(basic.getColorFromRgb(dot.c));
      trimPlot(dot.x, dot.y);
    });
  }

  @Override
  public void run() {
    rndAcc.run();
    basic.cls();
    cube(rndAcc.get(0), rndAcc.get(1), rndAcc.get(2));
  }

  public static class Dot implements Comparable {
    public double x;
    public double y;
    public double z;
    public int c;

    public Dot(double x, double y, double z, int c) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.c = c;
    }

    @Override
    public int compareTo(Object o) {
      return Double.compare(((Dot) o).z, this.z);
    }
  }

  public static class RndAcc implements Runnable {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final int n;
    private final double periodSeconds;
    private final double periodNano;
    private final double speed;
    private long timeLast;
    long[] rt;
    long[] rt0;
    double[] rv0;
    double[][] rv;
    private final long timeCreated;

    public RndAcc(int n, double periodSeconds, double speed) {
      this.n = n;
      this.periodSeconds = periodSeconds;
      this.periodNano = periodSeconds * 1_000_000_000;
      this.rt = new long[n];
      this.rt0 = new long[n];
      this.rv0 = new double[n];
      this.rv = new double[4][n];
      this.speed = speed;
      this.timeCreated = System.nanoTime();
      for (int i = 0; i < n; i++) {
        rv[0][i] = random.nextDouble(-1.0, 1.0);
        rv[2][i] = random.nextDouble(-1.0, 1.0);
      }
    }

    public double mix(double x, double y, double a) {
      return x * (1 - a) + y * a;
    }

    private long getNanoTime() {
      return Math.round((System.nanoTime() - timeCreated) * speed);
    }

    @Override
    public void run() {
      final long nanoTime = getNanoTime();
      for (int i = 0; i < n; i++) {
        if (nanoTime >= rt[i]) {
          rt0[i] = nanoTime;
          rt[i] = nanoTime + (long) random.nextDouble(periodNano / 4, periodNano);
          rv0[i] = rv[0][i];
          rv[0][i] = random.nextDouble(-1.0, 1.0);
        }
      }
      long diffNanoTime = timeLast - nanoTime;
      for (int i = 0; i < n; i++) {
        final double dt = rt[i] - rt0[i];
        rv[1][i] = mix(rv0[i], rv[0][i], (nanoTime - rt0[i]) / dt);
        rv[2][i] += rv[1][i] * diffNanoTime / dt;
      }
      timeLast = nanoTime;
    }

    public double get(int index, int integration) {
      return rv[integration][index];
    }

    public double get(int index) {
      return get(index, 2);
    }

  }

  public static void main(String[] args) {
    Screen screen = new Screen(new GraphicsMode(320, 180).aspectRatio(new Dimension(16, 9)));
    new Application(screen).run(new CubicGrid(new Basic(screen)));
  }

}
