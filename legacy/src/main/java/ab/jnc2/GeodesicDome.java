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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * I always wondered how Sierpinski triangle looks on Dymaxion map.
 */
public class GeodesicDome implements Runnable {

  public static final int DARK = 3;
  public static final int BRIGHT = 11;
  Basic basic;
  int centerx;
  int centery;
  double radius;
  double rx;
  double ry;

  public GeodesicDome(Basic basic) {
    this.basic = basic;
    centerx = basic.getWidth() / 2;
    centery = basic.getHeight() / 2;
    radius = basic.getHeight() * basic.getPixelHeight() / 2;
    rx = Math.min(radius, radius * basic.getPixelHeight());
    ry = Math.min(radius, radius / basic.getPixelHeight());
  }

  public Model3d cube() {
    double[][] vertices = {
        {-1, -1, -1}, {-1, -1,  1},
        {-1,  1, -1}, {-1,  1,  1},
        { 1, -1, -1}, { 1, -1,  1},
        { 1,  1, -1}, { 1,  1,  1},
    };
    int[][] faces = {
        {0, 1, 3, 2},
        {4, 0, 2, 6},
        {5, 4, 6, 7},
        {1, 5, 7, 3},
        {3, 7, 6, 2},
        {0, 4, 5, 1},
    };
    normalizeVectors(vertices);
    return new Model3d(vertices, faces);
  }

  public Model3d teapot() {
    InputStream resource = getClass().getResourceAsStream("/jnc2/teapot.obj");
    List<String> lines = new BufferedReader(new InputStreamReader(resource)).lines().collect(Collectors.toList());
    double[][] vertices = lines.stream().filter(s -> s.startsWith("v "))
        .map(s -> Arrays.stream(Arrays.copyOfRange(s.split("\\s+"), 1, 4))
            .mapToDouble(Double::parseDouble).toArray())
        .toArray(double[][]::new);
    int[][] faces = lines.stream().filter(s -> s.startsWith("f "))
        .map(s -> Arrays.stream(Arrays.copyOfRange(s.split("\\s+"), 1, 4))
            .mapToInt(s1 -> Integer.parseInt(s1) - 1).toArray())
        .toArray(int[][]::new);
    for (int i = 0; i < vertices.length; i++) {
      vertices[i][0] /= 2.6;
      vertices[i][1] /= 2.6;
      vertices[i][2] /= 2.6;
      vertices[i][1] -= 0.6;
    }
    return new Model3d(vertices, faces);
  }

  public void normalizeVectors(double[][] dots) {
    for (int i = 0; i < dots.length; i++) {
      double x = dots[i][0];
      double y = dots[i][1];
      double z = dots[i][2];
      double norm = Math.sqrt(x * x + y * y + z * z);
      dots[i][0] = x / norm;
      dots[i][1] = y / norm;
      dots[i][2] = z / norm;
    }
  }

  public Model3d subdivision(Model3d model) {
    List<double[]> newVertices = new ArrayList<>();
    List<int[]> newFaces = new ArrayList<>();
    int vl = model.vertices.length;
    for (int[] face : model.faces) {
      if (face.length > 3) {
        double[] newVertex = IntStream.range(0, 3) // 3d
            .mapToDouble(d -> Arrays.stream(face).mapToDouble(f -> model.vertices[f][d]).sum())
            .toArray();
        newVertices.add(newVertex);
        vl++;
        for (int i = 0; i < face.length; i++) {
          int i1 = i == 0 ? face.length - 1 : i - 1;
          newFaces.add(new int[]{face[i1], face[i], vl - 1});
        }
      } else if (face.length == 3) {
        for (int i = 0; i < 3; i++) {
          double[] v0 = model.vertices[face[i]];
          double[] v1 = model.vertices[face[(i + 1) % 3]];
          double[] newVertex = new double[]{
              v0[0] + v1[0],
              v0[1] + v1[1],
              v0[2] + v1[2],
          };
          newVertices.add(newVertex);
          vl++;
        }
        for (int i = 0; i < 3; i++) {
          newFaces.add(new int[]{face[i], vl - 3 + i, vl - 3 + ((i + 2) % 3)});
        }
        newFaces.add(new int[]{vl - 3, vl - 2, vl - 1}); // comment this line out for Sierpinski triangle
      }
    }
    double[][] vertices = newVertices.toArray(new double[0][]);
    normalizeVectors(vertices);
    return new Model3d(
        Stream.of(model.vertices, vertices).flatMap(Stream::of).toArray(double[][]::new),
        newFaces.toArray(new int[0][]));
  }

  public double[][] rotateVertices(double[][] vertices, double day, double year) {
    double[][] d = Arrays.stream(vertices).map(a -> Arrays.copyOf(a, a.length)).toArray(double[][]::new);
    day = 2 * Math.PI * day; // belongs to [0,1)
    for (int i = 0; i < vertices.length; i++) {
      double x = d[i][0];
      double z = d[i][2];
      d[i][0] = Math.sin(day) * z + Math.cos(day) * x;
      d[i][2] = Math.cos(day) * z - Math.sin(day) * x;
    }
    double tilt = -23.4 / 360 * 2 * Math.PI;
    for (int i = 0; i < vertices.length; i++) {
      double x = d[i][0];
      double y = d[i][1];
      d[i][0] = Math.sin(tilt) * y + Math.cos(tilt) * x;
      d[i][1] = Math.cos(tilt) * y - Math.sin(tilt) * x;
    }
    year = 2 * Math.PI * year; // belongs to [0,1)
    for (int i = 0; i < vertices.length; i++) {
      double x = d[i][0];
      double z = d[i][2];
      d[i][0] = Math.sin(year) * z + Math.cos(year) * x;
      d[i][2] = Math.cos(year) * z - Math.sin(year) * x;
    }
    return d;
  }

  public void plotDots(Model3d model, UnaryOperator<double[][]> rotation) {
    double[][] dots = rotation.apply(model.vertices);
    double rx = 0.9 * this.rx;
    double ry = 0.9 * this.ry;
    for (int i = 0; i < dots.length; i++) {
      double z = (dots[i][2] + 4.8) / 5;
      dots[i][0] = dots[i][0] * rx * z + centerx;
      dots[i][1] = dots[i][1] * ry * z + centery;
    }
    basic.ink(DARK);
    for (double[] dot : dots) {
      if (dot[2] >= 0) continue;
      basic.plot((int) Math.round(dot[0]), (int) Math.round(dot[1]));
    }
    basic.ink(BRIGHT);
    for (double[] dot : dots) {
      if (dot[2] < 0) continue;
      basic.plot((int) Math.round(dot[0]), (int) Math.round(dot[1]));
    }
  }

  public void drawLines(Model3d model, UnaryOperator<double[][]> rotation) {
    double[][] v = rotation.apply(model.vertices);
    double rx = 0.9 * this.rx;
    double ry = 0.9 * this.ry;
    for (int i = 0; i < v.length; i++) {
      double z = (v[i][2] + 4.8) / 5;
      v[i][0] = v[i][0] * rx * z + centerx;
      v[i][1] = v[i][1] * ry * z + centery;
    }
    for (int[] face : model.faces) {
      double[] v0 = v[face[0]];
      double[] v1 = v[face[1]];
      double[] v2 = v[face[2]];
      double[] vl = v[face[face.length - 1]];
      double ax = v1[0] - v0[0];
      double ay = v1[1] - v0[1];
      double bx = v2[0] - v0[0];
      double by = v2[1] - v0[1];
      double nz = ax * by - ay * bx; // normal of the triangle
      if (nz < 0) continue;
      basic.plot((int) Math.round(vl[0]), (int) Math.round(vl[1]));
      for (int f : face) {
        basic.draw((int) Math.round(v[f][0]), (int) Math.round(v[f][1]));
      }
    }
  }

  @Override
  public void run() {
    Model3d model = cube();
    for (int i = 0; i < 3; i++) {
      model = subdivision(model);
    }
    basic.cls();
    Instant instant = Instant.now();
    basic.printAt(0, 2, "solid:");
    basic.printAt(0, 1, "cube");
    basic.printAt(0, 0, "iter: 3");
//    plotDots(model, v -> rotateVertices(v, instant.toEpochMilli() / 6_000.0, instant.toEpochMilli() / 60_000.0));
    drawLines(model, v -> rotateVertices(v, instant.toEpochMilli() / 6_000.0, instant.toEpochMilli() / 60_000.0));
  }

  public static void main(String[] args) {
    Screen screen = new Screen(GraphicsMode.CGA_16);
    GeodesicDome application = new GeodesicDome(new Basic(screen));
    new Application(screen).run(application);
  }

  public static class Model3d {
    double[][] vertices;
    int[][] faces;

    public Model3d(double[][] vertices, int[][] faces) {
      this.vertices = vertices;
      this.faces = faces;
    }
  }
}
