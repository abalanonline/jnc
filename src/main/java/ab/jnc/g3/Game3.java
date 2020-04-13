/*
 * Copyright 2020 Aleksei Balan
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

package ab.jnc.g3;

import ab.jnc.Font;
import ab.jnc.JncKeyEvent;
import ab.jnc.Playable;
import ab.jnc.Resource;
import ab.jnc.Sprite;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Nyan Cat live in pixel world, so the units are pixels and seconds.
 * Its animation have 6 frames with 417 ms period. The speed is about 7 pixels per frame.
 */
public class Game3 implements Playable {

  public static final double CAT_FRAME_RATE = 6000D / 417D; // 417 ms for 6 frames
  public static final int CAT_FRAMES = 12; // 6 for cat, 12 for stars
  // Question of the Nyan Cat universe - how many pixels it travels during full loop?
  public static final double CAT_MOVE_SPEED = 7 * CAT_FRAME_RATE; // which is somewhat 100 pixels / sec
  public static final double CAT_BODY_SPEED = CAT_FRAME_RATE / CAT_FRAMES;
  public static final double CAMERA_SPEED = CAT_MOVE_SPEED * 0.8;
  public static final int WORLD_WIDTH = 320;
  public static final int WORLD_HEIGHT = 240;
  public static final int CAT_BASELINE_X = 24;

  private Resource re;
  private final BufferedImage worldImage = new BufferedImage(WORLD_WIDTH, WORLD_HEIGHT, BufferedImage.TYPE_INT_RGB);
  private final Graphics2D worldGraphics = worldImage.createGraphics();
  private final Rectangle worldWindow = new Rectangle(WORLD_WIDTH, WORLD_HEIGHT);
  private final BufferedImage debugImage = new BufferedImage(WORLD_WIDTH, WORLD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
  private final Graphics2D debugGraphics = debugImage.createGraphics();
  private final BufferedImage zoomImage = new BufferedImage(70, 70, BufferedImage.TYPE_INT_RGB);
  private final Graphics2D zoomGraphics = zoomImage.createGraphics();
  private double catB;
  private Instant previousTick;

  private List<SpaceSpace> space = new ArrayList<>();
  private List<Physics> spacePhysics = new ArrayList<>();

  private Sprite cat;
  private Sprite spark;
  private Sprite tart;
  private int debugTime = 0;
  private int debugWorld = 0;
  private double debugTransition;

  private double cameraZoom = 1;
  private boolean cameraZoomIn = true;
  private Point2D.Double cameraPoint;
  private Point2D.Double catPoint;
  private boolean isHome = true;
  private String debugText = "debug text:\nThe quick brown fox\njumps over the lazy dog";

  private double tickJitter0;
  private double tickJitter1;
  private final Random random = new Random();

  @Override
  public void load() {
    re = new Resource(this);
    re.getObjectMap().values().stream().filter(v -> v instanceof Sprite).map(v -> (Sprite) v).forEach(s -> {
      s.setMainGraphicsSupplier(() -> worldGraphics);
      s.setDebugGraphicsSupplier(() -> debugGraphics);
    });


    cat = re.getSprite("player");
    // to make proper camera snapping, start x must be divisible by 7 (cat speed in pixels)
    // since everything in this universe is build with 42, let it be start x = 42
    // and cat's 0,0 is the second paw
    cat.setTransform(r -> new Rectangle(r.x - CAT_BASELINE_X - worldWindow.x, 240 - r.height - r.y - worldWindow.y, r.width, r.height));

    tart = re.getSprite("tart");
    tart.setLocation(50, 120);

    spark = re.getSprite("spark");
    spark.setTransform(r -> new Rectangle(r.x - 3, 236 - r.y, r.width, r.height));
    resetSpace();

    re.setObject("font", new ab.jnc.Font(re.getSprite("font").getFrame(0)));
    re.setObject("debugfont", new ab.jnc.Font(re.getSprite("4x6font").getFrame(0)));
  }

  public static Point toPoint(Point2D.Double pointDouble) {
    final Point point = new Point();
    point.setLocation(pointDouble.x, pointDouble.y);
    return point;
  }

  void resetSpace() {
    random.setSeed(1);
    int x = 0;
    Physics physics = new Physics(random.nextLong());
    physics.setSpaceTransition(0);
    physics.setSpaceAction(168);
    for (int i = 0; i < 1000; i++) {
      physics.setSpaceStart(x);
      x = physics.getSpaceStop();
      spacePhysics.add(physics);
      physics = new Physics(random.nextLong());
    }

    catPoint = new Point2D.Double(42, 120);
    cat.setLocation(toPoint(catPoint));
    cameraPoint = new Point2D.Double();
    space.clear();
    final Random physicsRandom = spacePhysics.get(0).getRandom();
    space.add(new SpaceAnimation(cat, spark, -84, physicsRandom));
    space.add(new SpaceAnimation(cat, spark, 0, physicsRandom));
    space.add(new SpaceAnimation(cat, spark, 84, physicsRandom));
    for (int i = 168; i < 10000; i += 84) {
      space.add(new Space1x1(physicsRandom, new Rectangle(i, 0, 84, WORLD_HEIGHT), spark, 20));
    }
  }

  void unpark() {
    if (!cameraZoomIn) return; // not parked
    cameraZoomIn = false;
  }

  @Override
  public boolean tick(Instant instant, List<JncKeyEvent> keys) {
    if (previousTick == null) previousTick = instant;
    double tickDuration = (double) Duration.between(previousTick, instant).toNanos() / TimeUnit.SECONDS.toNanos(1);
    previousTick = instant;

    for (JncKeyEvent key : keys) {
      switch (key.getKeyCode()) {
        case KeyEvent.VK_OPEN_BRACKET: debugTransition -= 0.05; break;
        case KeyEvent.VK_CLOSE_BRACKET: debugTransition += 0.05; break;
        case KeyEvent.VK_T: debugTime++; break;
        case KeyEvent.VK_Y: debugTime--; break;
        case KeyEvent.VK_D: debugWorld++; if (debugWorld >= 4) { debugWorld = 0; } break;
        case KeyEvent.VK_RIGHT:
          if (debugTime == 3) {
            tickDuration = 0.417 / 48;
            break; }
          break;
        case KeyEvent.VK_SPACE: unpark(); break;
      }
    }

    debugTime = Math.min(Math.max(debugTime, 0), 3);
    switch (debugTime) {
      case 0: break;
      case 1: tickDuration /= 8; break;
      case 2: tickDuration /= 64; break;
      case 3: tickDuration = 0; break;
    }

    debugTransition = Math.min(Math.max(debugTransition, 0), 1);
    debugText = String.format("transition: %.2f   ", debugTransition);

    // jittering begin - caution
    tickJitter0 += tickDuration;
    int tickJitter2 = ((int) (debugTransition * 10)) + 1;
    debugText += "jit: " + tickJitter2 + "   ";
    int tickJitter3 = (int) (tickJitter0 * CAT_FRAME_RATE * tickJitter2);
    double tickJitter4 = (double) tickJitter3 / (CAT_FRAME_RATE * tickJitter2);
    // if (tickJitter1 != tickJitter4) { the code is valid for both branches
    tickDuration = tickJitter4 - tickJitter1;
    tickJitter1 = tickJitter4;
    // jittering end - caution

    isHome = cameraZoomIn;
    cameraZoom += (cameraZoomIn ? 1 : -1) * 0.05;
    cameraZoom = Math.min(Math.max(cameraZoom, 0), 1);

    catB += CAT_BODY_SPEED * tickDuration;
    catPoint.x += CAT_MOVE_SPEED * tickDuration;
    cameraPoint.x += CAMERA_SPEED * tickDuration;
    if (isHome && (catPoint.x >= 126)) { catPoint.x -= 84; cameraPoint.x = 0; }
    cat.setLocation(toPoint(catPoint));
    worldWindow.setLocation(toPoint(cameraPoint));

    int spriteFrame = (int) (catB * CAT_FRAMES + 0.1); // constant smoothing the truncation
    //assert spriteFrame % 6 * 7 == cat.x % 42 : "frame drop";
    cat.setCurrentFrame(spriteFrame % CAT_FRAMES);
    return true;
  }

  private double actZoom(double zoom, double width) {
    // arcatangent zoom is smooth with width around Pi
    // with big width it is step, with small - linear
    double minValue = Math.atan(-0.5 * width);
    double value = Math.atan((zoom - 0.5) * width);
    double maxValue = Math.atan(0.5 * width);
    return (value - minValue) / (maxValue - minValue);
  }

  private void drawDebug1() {
    if (debugWorld == 1) {
      debugGraphics.setColor(Color.BLACK);
      debugGraphics.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
      debugGraphics.setColor(Color.WHITE);
      for (int i = -(worldWindow.x % 10); i < 320; i += 10) {
        debugGraphics.fillRect(i, 0, 1, WORLD_HEIGHT);
      }
      for (int i = -(worldWindow.y % 10); i < 240; i += 10) {
        debugGraphics.fillRect(0, 239 - i, WORLD_WIDTH, 1);
      }
      worldGraphics.setComposite(AlphaComposite.SrcOver.derive(0.1f));
      worldGraphics.drawImage(debugImage, 0, 0, null);
      worldGraphics.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }
    if (debugWorld == 1) {
      debugGraphics.setColor(Color.BLACK);
      debugGraphics.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
      debugGraphics.setColor(Color.MAGENTA);
      int transparentColor = new Color(0, 0, 0, 0).getRGB();
      for (int y = 0; y < 240; y++) {
        for (int x = 0; x < 320; x++) {
          debugImage.setRGB(x, y, transparentColor);
        }
      }
    }
  }

  private void drawDebug2() {
    if (debugWorld == 1) {
      worldGraphics.setComposite(AlphaComposite.SrcOver.derive(0.5f));
      worldGraphics.drawImage(debugImage, 0, 0, null);
      worldGraphics.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }

    if (debugWorld == 3) {
      int transparentColor = new Color(0, 0, 0, 0).getRGB();
      for (int y = 0; y < 240; y++) {
        for (int x = 0; x < 320; x++) {
          debugImage.setRGB(x, y, transparentColor);
        }
      }
      final Font debugFont = re.getFont("debugfont");
      debugGraphics.setColor(Color.GREEN);
      for (Physics p : spacePhysics) {
        if ((p.getSpaceStop() > worldWindow.x - 100) && (p.getSpaceStart() < worldWindow.x + 500)) {
          int x = p.getSpaceStart() - worldWindow.x;
          debugGraphics.fillRect(x, 0, 1, worldWindow.height);
          debugFont.write("tr", new Point(x + 2, 16), debugGraphics);
          x += p.getSpaceTransition();
          debugGraphics.fillRect(x, 0, 1, worldWindow.height);
          debugFont.setAlignRight(true);
          debugFont.write("tr", new Point(x, 16), debugGraphics);
          debugFont.setAlignRight(false);
          debugFont.write(p.toString(), new Point(x + 2, 16), debugGraphics);
        }
      }
      debugGraphics.setColor(Color.ORANGE);
      for (SpaceSpace p : space) {
        if ((p.x + p.width > worldWindow.x - 100) && (p.x < worldWindow.x + 500)) {
          debugGraphics.fillRect(p.x - worldWindow.x, 0, 1, worldWindow.height);
          debugGraphics.fillRect(p.x - worldWindow.x, WORLD_HEIGHT - 1 - p.y, p.width, 1);
        }
      }
      //worldGraphics.setComposite(AlphaComposite.SrcOver.derive(0.5f));
      worldGraphics.drawImage(debugImage, 0, 0, null);
      //worldGraphics.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }
  }

  public void drawWithZoom(Graphics2D graphics) {
    // only zoom code beyond this line
    double camz = actZoom(cameraZoom, Math.PI);
    if (camz > 0) {
      Point catLocation = cat.getTransform().apply(cat).getLocation();
      catLocation.translate(-18, -25);
      final int CROP_WIDTH = 25;
      if (camz == 1) {
        // sharp fixed zoom
        zoomGraphics.drawImage(worldImage, -catLocation.x, -catLocation.y, null);
        graphics.drawImage(zoomImage.getScaledInstance(270, 240, Image.SCALE_FAST), 25, 0, null);
      } else graphics.drawImage(worldImage,
          (int) (camz * (-catLocation.x * 27 / 7 + CROP_WIDTH)),
          (int) (camz * -catLocation.y * 24 / 7),
          (int) ((camz * 20 / 7 + 1) * WORLD_WIDTH),
          (int) ((camz * 17 / 7 + 1) * WORLD_HEIGHT),
          null);
      graphics.setColor(Color.BLACK);
      graphics.fillRect((int) (camz * CROP_WIDTH) - CROP_WIDTH, 0, CROP_WIDTH, WORLD_HEIGHT);
      graphics.fillRect(WORLD_WIDTH - (int) (camz * CROP_WIDTH), 0, CROP_WIDTH, WORLD_HEIGHT);
    } else graphics.drawImage(worldImage, 0, 0, null);

  }

  private void drawImage(Sprite s) {
    if (debugWorld == 2) {
      worldGraphics.setColor(Color.GRAY);
      Rectangle r = s.getTransform().apply(s);
      worldGraphics.fillRect(r.x, r.y, r.width, r.height);
    } else s.drawImage();
  }

  @Override
  public void draw(Graphics2D graphics) {
    worldGraphics.setColor(Color.MAGENTA);
    worldGraphics.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
    Point worldWindowDistance = new Point(-worldWindow.x, -worldWindow.y);
    for (SpaceSpace wall : space) {
      wall.drawBg(worldGraphics, worldWindowDistance);
    }
    for (SpaceSpace wall : space) {
      wall.drawFg(worldGraphics, worldWindowDistance);
    }
    drawDebug1();
    drawImage(cat);

    // debug cat's 0,0
    //spark.x = cat.x - worldWindow.x;
    //spark.y = cat.y - worldWindow.y;
    //spark.drawImage();
    tart.setLocation(50 - worldWindow.x, 140);
    drawImage(tart);
    tart.setLocation(50 - worldWindow.x + 84, 140);
    drawImage(tart);

    drawDebug2();
    drawWithZoom(graphics);

    if (debugWorld == 3) {
      graphics.setColor(Color.GRAY);
      re.getFont("debugfont").write(debugText, new Point(16, 232), graphics);
    }

    graphics.setColor(new Color(0xFF, 0xFF, 0xFF, 0x1A)); // youtube filter 10% white
    graphics.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
  }

}
