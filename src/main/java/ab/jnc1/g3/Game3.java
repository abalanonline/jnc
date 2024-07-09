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

package ab.jnc1.g3;

import ab.jnc1.Font;
import ab.jnc1.JncKeyEvent;
import ab.jnc1.Playable;
import ab.jnc1.Resource;
import ab.jnc1.Sprite;

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
 * Cat size = 46 cm, 26.5 pix. Pixel size = 17 mm
 */
public class Game3 implements Playable {

  public static final double CAT_FRAME_RATE = 6000D / 417D; // 417 ms for 6 frames
  public static final int CAT_FRAMES = 12; // 6 for cat, 12 for stars
  // Question of the Nyan Cat universe - how many pixels it travels during full loop?
  public static final double CAT_MOVE_SPEED = 7 * CAT_FRAME_RATE; // which is somewhat 100 pixels / sec
  public static final double CAT_BODY_SPEED = CAT_FRAME_RATE / CAT_FRAMES;
  public static final double PIXEL_SIZE = 0.0173584905660377;
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
  private PhysicsList spacePhysics = new PhysicsList(0);
  private Physics physics;

  private Sprite cat;
  private Sprite spark;
  private Sprite tart;
  private int debugTime = 0;
  private int debugWorld = 0;
  private double debugTransition;

  private double cameraZoom = 1;
  private boolean cameraZoomIn = true;
  private Point2D.Double cameraPoint;
  private Point2D.Double catPosition;
  private Point2D.Double catVelocity;
  private Point2D.Double catAcceleration;
  private boolean catImpulse;

  private boolean isHome = true;
  private String debugText = "debug text:\nThe quick brown fox\njumps over the lazy dog";

  private double jitterTimeFine;
  private double jitterTimeCoarse;
  private final Random random = new Random(0);
  private boolean isStopped = false;

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
    cat.setTransform(r -> new Rectangle(r.x - CAT_BASELINE_X, 240 - r.height - r.y, r.width, r.height));

    tart = re.getSprite("tart");
    tart.setLocation(50, 120);

    spark = re.getSprite("spark");
    spark.setTransform(r -> new Rectangle(r.x - 3, 236 - r.y, r.width, r.height));
    resetSpace();

    re.setObject("font", new ab.jnc1.Font(re.getSprite("font").getFrame(0)));
    re.setObject("debugfont", new ab.jnc1.Font(re.getSprite("4x6font").getFrame(0)));
  }

  public static Point toPoint(Point2D.Double pointDouble) {
    final Point point = new Point();
    point.setLocation(pointDouble.x, pointDouble.y);
    return point;
  }

  void resetSpace() {
    spacePhysics = new PhysicsList(1);

    spacePhysics.add(Physics.ANIMATION());
    spacePhysics.add(Physics.VANILLA());
    for (int i = 0; i < 1000; i++) {
      spacePhysics.add(new Physics(spacePhysics.getRandom().nextLong()));
      //spacePhysics.add(Physics.VANILLA());
    }

    catPosition = new Point2D.Double(42, 120);
    catVelocity = new Point2D.Double(CAT_MOVE_SPEED, 0);
    catAcceleration = new Point2D.Double();
    catImpulse = false;
    cat.setLocation(toPoint(catPosition));

    cameraPoint = new Point2D.Double();
    space.clear();
    space.add(new SpaceAnimation(spacePhysics.getFirst(), cat, spark, -84));
    space.add(new SpaceAnimation(spacePhysics.getFirst(), cat, spark, 0));
    space.add(new SpaceAnimation(spacePhysics.getFirst(), cat, spark, 84));
    for (int i = 168; i < 10000; i += 84) {
      space.add(new Space1x1(spacePhysics.get((double) i), new Rectangle(i, 0, 84, WORLD_HEIGHT), spark, 20));
    }
    physics = spacePhysics.get(catPosition.x);
  }

  void unpark() {
    if (!cameraZoomIn) return; // not parked
    cameraZoomIn = false;
  }

  @Override
  public boolean tick(Instant instant, List<JncKeyEvent> keys) {
    if (isStopped) { return true; }
    if (previousTick == null) previousTick = instant;
    double tickDuration = (double) Duration.between(previousTick, instant).toNanos() / TimeUnit.SECONDS.toNanos(1);
    final Instant tickCurrent = instant;
    final Instant tickPrevious = previousTick;
    previousTick = instant;

    Duration impulseDuration = Duration.ZERO;
    Instant impulseStart = catImpulse ? tickPrevious : null;
    for (JncKeyEvent key : keys) {
      if (key.isReleased()) {
        switch (key.getKeyCode()) {
          case KeyEvent.VK_UP:
            catImpulse = false;
            if (impulseStart == null) { break; }
            impulseDuration = impulseDuration.plus(Duration.between(impulseStart, key.getInstant()));
            impulseStart = null;
            break;
        }
        continue;
      }
      switch (key.getKeyCode()) {
        case KeyEvent.VK_OPEN_BRACKET: debugTransition -= 0.05; break;
        case KeyEvent.VK_CLOSE_BRACKET: debugTransition += 0.05; break;
        case KeyEvent.VK_T: debugTime++; break;
        case KeyEvent.VK_Y: debugTime--; break;
        case KeyEvent.VK_D: debugWorld++; if (debugWorld >= 4) { debugWorld = 0; } break;
        case KeyEvent.VK_BACK_SLASH:
          if (debugTime == 3) {
            tickDuration = 0.417 / 48;
            break; }
          break;
        case KeyEvent.VK_SPACE: unpark(); break;
      }

      if (cameraZoomIn) { continue; } // disable controls in zoom
      switch (key.getKeyCode()) {
        case KeyEvent.VK_UP:
          catImpulse = true;
          if (impulseStart != null) { break; }
          impulseStart = key.getInstant();
          break;
        case KeyEvent.VK_LEFT: catVelocity.x -= 10; break;
        case KeyEvent.VK_RIGHT: catVelocity.x += 10; break;
      }
    }
    if (impulseStart != null) {
      impulseDuration = impulseDuration.plus(Duration.between(impulseStart, tickCurrent));
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
    final double tickJitterPhysics = physics.getJitter();
    jitterTimeFine += tickDuration;
    double jitterTime = jitterTimeFine;
    if (tickJitterPhysics > 0) { // nothing to calculate then
      final int tickJitterStep = 3 - Math.min(Math.max((int) (tickJitterPhysics * 4), 0), 3); // 00112233
      int tickJitter2 = 1 << tickJitterStep; // 88442211
      debugText += "jit: " + tickJitter2 + "   ";
      int tickJitter3 = (int) (jitterTimeFine * CAT_FRAME_RATE * tickJitter2);
      jitterTime = (double) tickJitter3 / (CAT_FRAME_RATE * tickJitter2);
    }
    tickDuration = jitterTime - jitterTimeCoarse;
    jitterTimeCoarse = jitterTime;
    // jittering end - caution

    isHome = cameraZoomIn;
    cameraZoom += (cameraZoomIn ? 1 : -1) * 0.05;
    cameraZoom = Math.min(Math.max(cameraZoom, 0), 1);

    // motion physics
    double t = tickDuration;
    catAcceleration.setLocation(0, -physics.getGravity() / PIXEL_SIZE);
    catVelocity.setLocation(
        catAcceleration.getX() * t + catVelocity.getX(),
        catAcceleration.getY() * t + catVelocity.getY()
            + 400 * (double) impulseDuration.toNanos() / TimeUnit.SECONDS.toNanos(1)
    );
    catPosition.setLocation(
        catVelocity.getX() * t + catPosition.getX(),
        catVelocity.getY() * t + catPosition.getY());
    //catPosition.x += CAT_MOVE_SPEED * tickDuration;

    catB += CAT_BODY_SPEED * tickDuration;
    cameraPoint.x += CAMERA_SPEED * tickDuration;
    if (isHome && (catPosition.x >= 126)) { catPosition.x -= 84; cameraPoint.x = 0; }
    cat.setLocation(toPoint(new Point2D.Double(catPosition.x - cameraPoint.x, catPosition.y - cameraPoint.y))); // toPoint(catPoint)
    physics = spacePhysics.get(catPosition.x);

    worldWindow.setLocation(toPoint(cameraPoint));

    int spriteFrame = (int) (catB * CAT_FRAMES + 0.1); // constant smoothing the truncation
    //assert spriteFrame % 6 * 7 == cat.x % 42 : "frame drop";
    cat.setCurrentFrame(spriteFrame % CAT_FRAMES);
    physics.setCurrentFrame(spriteFrame % CAT_FRAMES);

    isStopped |= ((catPosition.getY() < 0) || (catPosition.getY() > 220) ||
        (catPosition.getX() - cameraPoint.getX() < 16) || (catPosition.getX() - cameraPoint.getX() > 312));

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
      debugGraphics.setColor(Color.ORANGE);
      for (Physics p : spacePhysics) {
        if ((p.getSpaceStop() > worldWindow.x - 100) && (p.getSpaceStart() < worldWindow.x + 500)) {
          int x = (int) p.getSpaceStart() - worldWindow.x;
          debugGraphics.fillRect(x, 0, 1, worldWindow.height);
          debugFont.write("tr", new Point(x + 2, 2), debugGraphics);
          x += p.getSpaceTransition();
          debugGraphics.fillRect(x, 0, 1, worldWindow.height);
          debugFont.setAlignRight(p.getSpaceSustain() <= 0);
          debugFont.write(p.toString(), new Point(x + 2, 2), debugGraphics);
          debugFont.setAlignRight(false);
        }
      }
      debugGraphics.setColor(Color.GREEN);
      for (SpaceSpace p : space) {
        if ((p.x + p.width > worldWindow.x - 100) && (p.x < worldWindow.x + 500)) {
          debugGraphics.fillRect(p.x - worldWindow.x, 0, 1, worldWindow.height);
          debugGraphics.fillRect(p.x - worldWindow.x, WORLD_HEIGHT - 1 - p.y, p.width, 1);
        }
      }
      debugGraphics.setColor(Color.RED);
      debugFont.write(physics.toString(), new Point(cat.x, 240 - cat.y), debugGraphics);
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
        //zoomGraphics.setColor(Color.BLACK);
        //zoomGraphics.fillRect(0, 0, 70, 70);
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
      wall.drawBg(worldGraphics, worldWindowDistance, physics);
    }
    for (SpaceSpace wall : space) {
      wall.drawFg(worldGraphics, worldWindowDistance, physics);
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
