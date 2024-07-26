/*
 * Copyright (C) 2022 Aleksei Balan
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Onna Sanshirou - Typhoon Gal.
 * Emulator of 1985 arcade machine on the modern hardware.
 * Game controls: left, right, up, down
 * Z/X - jump/attack
 * X/C - attack/jump (mirrored)
 * SPACE to start, F1 no hit cheat code, F2 change music, F3 ext midi
 */
public class TyphoonGal implements Runnable, KeyListener {

  TyphoonMachine cpu;
  TyphoonMachine sound;
  TyphoonMachine videoMemory;
  Screen screen;
  int pianoSound;
  int extMidi;
  final Queue<String> systemConsole = new LinkedBlockingDeque<>();
  final Queue<Integer> audioFeed = new LinkedBlockingDeque<>();
  final Queue<Integer> gameController = new LinkedBlockingDeque<>();

  void logInfo(String s) {
    systemConsole.add(s);
    videoMemory.run();
  }

  public Map<String, byte[]> getZipContent(Path path) {
    logInfo("loading " + path + " ...");
    Map<String, byte[]> map = new LinkedHashMap<>();
    try {
      final ZipFile zipFile = new ZipFile(path.toFile());
      for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
        ZipEntry entry = entries.nextElement();
        byte[] bytes = new byte[(int) entry.getSize()];
        new DataInputStream(zipFile.getInputStream(entry)).readFully(bytes);
        map.put(entry.getName(), bytes);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return map;
  }

  public void loader() {
    logInfo("ERR11,jnc");

    byte[] font8x8 = new TextFont(8, 8).font;
    byte[] bufferFont = new byte[0x1000];
    for (int i = 0; i < 0x800; i++) {
      byte b = font8x8[i];
      bufferFont[i * 2] = (byte) ((b >> 7 & 0x01) | (b >> 5 & 0x02) | (b >> 3 & 0x04) | (b >> 1 & 0x08));
      bufferFont[i * 2 + 1] = (byte) ((b >> 3 & 0x01) | (b >> 1 & 0x02) | (b << 1 & 0x04) | (b << 3 & 0x08));
    }
    videoMemory.write(0x0C000, bufferFont);

    Map<String, byte[]> zipContent;
    zipContent = getZipContent(Paths.get("onna34ro.zip"));

    videoMemory.load(zipContent);
    videoMemory.open();

    sound.load(zipContent);

    zipContent = getZipContent(Paths.get("onna34roa.zip"));
    cpu.load(zipContent);
    cpu.write(0xD800, new byte[]{(byte) 0xC0, (byte) 0x00, (byte) 0xA0, (byte) 0x3F, (byte) 0xFF, (byte) 0x00, (byte) 0xFF});
    sound.open();
    logInfo("ready");
    cpu.open();
  }

  public TyphoonGal(Screen screen) {
    this.screen = screen;
    screen.keyListener = this;
    cpu = new Cpu(gameController, audioFeed, systemConsole);
    sound = new TyphoonSound(audioFeed, null, systemConsole);
    videoMemory = new TyphoonVideo(cpu, screen, systemConsole);
    logInfo("");
    logInfo("JNC2");
    logInfo(String.format("screen %dx%d%s - %s", screen.mode.resolution.width, screen.mode.resolution.height,
        screen.mode.colorMap == null ? "" : "x" + screen.mode.colorMap.length,
        screen.mode.resolution.width >= 256 && screen.mode.resolution.height >= 224
            && screen.mode.colorMap == null ? "ok" : "poor"));

    try {
      loader();
    } catch (UncheckedIOException | IllegalStateException e) {
      logInfo(e.getMessage());
    } catch (RuntimeException | NoClassDefFoundError e) {
      logInfo(e.getClass().getName() + ": " + e.getMessage());
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      sound.close();
    }));
  }

  @Override
  public void run() {
    cpu.run();
    videoMemory.run();
    sound.run();
  }

  public static void main(String[] args) {
    Screen screen = new Screen(new GraphicsMode(256, 224));
    screen.flicker(59.94, new TyphoonGal(screen));
  }

  int mask = 0;
  @Override
  public void keyTyped(KeyEvent e) {
    byte[] system = new byte[0x10];
    cpu.read(0xD800, system);
    switch (e.getKeyChar()) {
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
      case '0':
        int i = e.getKeyChar() - '0';
        mask ^= 1 << i;
        systemConsole.add(String.format("ERR40,%03X", mask));
        break;
    }

  }

  @Override
  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    switch (keyCode) {
      case KeyEvent.VK_UP:
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_Z:
      case KeyEvent.VK_X:
      case KeyEvent.VK_SPACE:
      case KeyEvent.VK_F1:
        gameController.add(keyCode);
        break;
      case KeyEvent.VK_C:
        gameController.add(KeyEvent.VK_Z);
        break;
      case KeyEvent.VK_F2:
        int[] sounds = {6, 3, 91, 8}; // fm, electric, synth, clavi
        pianoSound = (pianoSound + 1) % sounds.length;
        audioFeed.add(0x100 + sounds[pianoSound]);
        break;
      case KeyEvent.VK_F3:
        extMidi = (extMidi + 1) % TyphoonSound.MAX_MIDI_DEVICES;
        audioFeed.add(0x200 + extMidi);
        break;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    int keyCode = e.getKeyCode();
    switch (keyCode) {
      case KeyEvent.VK_UP:
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_Z:
      case KeyEvent.VK_X:
        gameController.add(-keyCode);
        break;
      case KeyEvent.VK_C:
        gameController.add(-KeyEvent.VK_Z);
        break;
    }
  }

  public static class TyphoonVideo extends TyphoonMachine {
    public static final int VIDEO_RAM = 0xC000;
    public static final int SPRITE_RAM = 0xDC00;
    public static final int SCRLRAM = 0xDCA0;
    private final BufferedImage image;
    private final TyphoonMachine cpu;
    private final Screen screen;
    private final Queue<String> console;
    private final AtomicInteger cursor;
    private boolean isDebug;

    public TyphoonVideo(TyphoonMachine cpu, Screen screen, Queue<String> console) {
      super(null, null, null, 0x20000);
      this.cpu = cpu;
      this.screen = screen;
      this.console = console;
      cursor = new AtomicInteger(VIDEO_RAM + 0x80);
      this.image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public void load(Map<String, byte[]> storage) {
      write(0x00000, storage, "a52-04.11v", "d5f70b81-47c5-3910-967a-a501cd2d767a");
      write(0x04000, storage, "a52-06.10v", "226cedec-da4c-3b9e-94f5-2812893264d6");
      write(0x08000, storage, "a52-08.09v", "1da39213-5d70-3c68-9b78-ffe3b49cdbcf");
      write(0x0C000, storage, "a52-10.08v", "89329886-446d-3108-8376-efb21a75d8f7");
      write(0x10000, storage, "a52-05.35v", "729d9204-bdca-3b01-8c20-4eadd11f0505");
      write(0x14000, storage, "a52-07.34v", "6b46397a-202a-383a-9839-0375821178ad");
      write(0x18000, storage, "a52-09.33v", "e88102c7-71cf-3643-a23c-a94d6d6997ba");
      write(0x1C000, storage, "a52-11.32v", "34774280-4304-3725-b8e6-8a043deb3d88");
    }

    @Override
    public void open() {
      for (int i = 0; i < 0x20000; i++) {
        writeByte(i, readByte(i) ^ 0xFF);
      }
    }

    public void draw8(int x, int y, int addr, int colorAddr, boolean flip, boolean flipv, int opacity) {
      for (int y1 = 0; y1 < 8; y1++) {
        int y2 = flipv ? 7 - y1 : y1;
        int a = addr + y2 * 2;
        byte m0 = (byte) readByte(a);
        byte m1 = (byte) readByte(a + 1);
        int b0 = m0 & 0x0F | ((m1 & 0x0F) << 4);
        int b1 = (m0 >> 4) & 0x0F | (((m1 >> 4) & 0x0F) << 4);
        a += 0x10000;
        byte m2 = (byte) readByte(a);
        byte m3 = (byte) readByte(a + 1);
        int b2 = m2 & 0x0F | ((m3 & 0x0F) << 4);
        int b3 = (m2 >> 4) & 0x0F | (((m3 >> 4) & 0x0F) << 4);
        for (int x1 = 0; x1 < 8; x1++) {
          int x2 = flip ? 7 - x1 : x1;
          int c = ((b0 >> x2) & 1) != 0 ? 0b01 : 0;
          c += ((b1 >> x2) & 1) != 0 ? 0b10 : 0;
          c += ((b2 >> x2) & 1) != 0 ? 0b0100 : 0;
          c += ((b3 >> x2) & 1) != 0 ? 0b1000 : 0;
          if (((1 << c) & opacity) == 0) continue;

          Color rgb = new Color(c * 0x11, c * 0x11, c * 0x11);
          int ca = c + colorAddr;
          int cb0 = cpu.readByte(ca);
          int cb1 = cpu.readByte(ca + 0x100);
          int r = cb0 & 0x0F;
          int g = (cb0 >> 4) & 0x0F;
          int b = cb1 & 0x0F;
          rgb = new Color(r * 0x11, g * 0x11, b * 0x11);
          image.setRGB((x + x1) & 0xFF, (y + y1) & 0xFF, rgb.getRGB());
        }
      }
    }

    public void draw16(int x, int y, int addr, int colorAddr, boolean flip, boolean flipv) {
      for (int i = 0; i < 4; i++) {
        int i1 = (i & 1) == 0 ^ flip ? 0 : 8;
        int i2 = (i & 2) == 0 ^ flipv ? 0 : 8;
        draw8(i1 + x, i2 + y, 0x10 * i + addr, colorAddr, flip, flipv, 0x7FFF);
      }
    }

    public static final Pattern REALTIME_ERROR = Pattern.compile("ERR(1?[\\dA-F]{2}),(.*)");
    @Override
    public void run() {
      boolean cheatCode = (cpu.readByte(0xD802) & 1) != 0;
      if (cheatCode) {
        for (int i = 0; i < 0x200; i++) {
          int a = 0xC800 + (i & 0x100) * 2 + (i & 0xFF);
          int c = cpu.readByte(a) | (cpu.readByte(a + 0x100) << 8);
          int brightness = ((c << 1 & 0x1E) + (c >> 2 & 0x3C) + (c >> 8 & 0xF)) * 0x27 >> 8;
          int bh = 15 - brightness;
          int bl = bh / 2;
          c = bh * 0x010 + bl * 0x101 + (c & 0xF000);
          cpu.writeByte(a + 0x2000, c);
          cpu.writeByte(a + 0x2100, c >> 8);
        }
      }

      for (String s = console.poll(); s != null; s = console.poll()) {
        Matcher matcher = REALTIME_ERROR.matcher(s);
        if (matcher.matches()) {
          int i = Integer.parseInt(matcher.group(1), 16);
          if (i == 0xFF && "1".equals(matcher.group(2))) {
            isDebug = true;
            continue;
          }
          if (!isDebug) continue;
          AtomicInteger addr = new AtomicInteger(VIDEO_RAM + 0x80 + i * 4);
          matcher.group(2).toUpperCase().chars().forEach(c -> cpu.writeWord(addr.getAndAdd(2), c));
          continue;
        }

        if (!cpu.isOpen()) {
          for (int i = 0; i < 8; i++) {
            cpu.writeWord(i * 2 + Cpu.PALETTE_BANK_0, 0xFF00);
            cpu.writeWord(i * 2 + Cpu.PALETTE_BANK_0 + 0x100, 0x1F00);
          }
          s.toUpperCase().chars().forEach(c -> cpu.writeWord(cursor.getAndAdd(2), c));
          cursor.set(((cursor.get() + 0x40) & 0xFFC0) + 4);
        }
      }

      for (int frontHalf = 0; frontHalf < 2; frontHalf++) {
        for (int y = 0; y < 32; y++) {
          for (int x = 0; x < 32; x++) {
            int w = cpu.readWord(y * 0x40 + x * 2 + VIDEO_RAM);
            boolean flip = (w & 0x0800) != 0;
            boolean flipv = (w & 0x1000) != 0;
            int ch = ((w >> 6) & 0x300) | (w & 0xFF);
            int col = (w & 0xF00) >> 8;
            draw8(x * 8, y * 8 - cpu.readByte(SCRLRAM + x),
                ch * 0x10 + VIDEO_RAM, (col << 4) + Cpu.PALETTE_BANK_0 + (cheatCode ? 0x2000 : 0),
                flip, flipv, frontHalf > 0 ? 0xC000 : 0xFFFF);
          }
        }

        if (isDebug) for (int i = 0; i < 0x100; i++) {
          int i1 = i + Cpu.PALETTE_BANK_0;
          int c0 = cpu.readByte(i1);
          int c1 = cpu.readByte(i1 + 0x100);
          int r = c0 & 0x0F;
          int g = (c0 >> 4) & 0x0F;
          int b = c1 & 0x0F;
          int a = (c1 >> 4) & 0x0F;
          Color rgb = new Color(r * 0x11, g * 0x11, b * 0x11);
          Color rgba = new Color(a * 0x11, a * 0x11, a * 0x11);
          int x = (i << 1) & 0xFF;
          int y = 0x10 + ((i >> 5) & 4);
          image.setRGB(x, y, rgb.getRGB()); image.setRGB(x + 1, y, rgb.getRGB());
          image.setRGB(x, y + 1, rgb.getRGB()); image.setRGB(x + 1, y + 1, rgb.getRGB());
          image.setRGB(x, y + 2, rgb.getRGB()); image.setRGB(x + 1, y + 2, rgba.getRGB());
          image.setRGB(x, y + 3, rgb.getRGB()); image.setRGB(x + 1, y + 3, rgba.getRGB());
        }

        if (frontHalf > 0) break;
        for (int i = 0; i < 0x20; i++) {
          int i1 = cpu.readByte(SPRITE_RAM + 0x9F - i) & 0x1F;
          int addr = i1 * 4 + SPRITE_RAM;
          int x = cpu.readByte(addr + 3);
          int y = 0xEF - cpu.readByte(addr);
          int w = cpu.readWord(addr + 1);
          boolean flip = (w & 0x0040) != 0;
          boolean flipv = (w & 0x0080) != 0;
          int ch = ((w << 4) & 0x300) | ((w >> 8) & 0xFF);
          int col = w & 0x000F;
          draw16(x, y, ch * 0x40, (col << 4) + Cpu.PALETTE_BANK_1 + (cheatCode ? 0x2000 : 0), flip, flipv);
        }
      }
      screen.image.getGraphics().drawImage(image, 0, -16, null);
      screen.repaint();
    }

  }

  public static class Cpu extends TyphoonMachine {
    public static final int PALETTE_BANK = 0xDD00;
    public static final int PALETTE_BANK_0 = 0xC800;
    public static final int PALETTE_BANK_1 = 0xCA00;
    public static final int GFXCTRL = 0xDF03;
    public static final int SOUND_LATCH = 0xD400;

    private int upDown;
    private int leftRight;
    private boolean btnAttack;
    private boolean btnJump;
    private boolean btnStart;
    private HighScore highScore;

    public enum ControllerCommand {
      UP(KeyEvent.VK_UP), DOWN(KeyEvent.VK_DOWN), LEFT(KeyEvent.VK_LEFT), RIGHT(KeyEvent.VK_RIGHT),
      ATTACK(KeyEvent.VK_X), JUMP(KeyEvent.VK_Z), START(KeyEvent.VK_SPACE), CHEAT(KeyEvent.VK_F1),
      UP0(-KeyEvent.VK_UP), DOWN0(-KeyEvent.VK_DOWN), LEFT0(-KeyEvent.VK_LEFT), RIGHT0(-KeyEvent.VK_RIGHT),
      ATTACK0(-KeyEvent.VK_X), JUMP0(-KeyEvent.VK_Z);
      private int code;
      ControllerCommand(int code) {
        this.code = code;
      }
      public static ControllerCommand valueOf(int i) {
        return Arrays.stream(ControllerCommand.values()).filter(cmd -> cmd.code == i).findAny().orElseThrow(() ->
            new IllegalArgumentException("No enum constant " + ControllerCommand.class.getCanonicalName() + "." + i));
      }
    }

    public Cpu(Queue<Integer> stdin, Queue<Integer> stdout, Queue<String> stderr) {
      super(stdin, stdout, stderr);
    }

    @Override
    public void load(Map<String, byte[]> storage) {
      write(0x0000, storage, "ry-08.rom", "f38e82b8-b972-380d-b9df-35879e136da3");
      write(0x4000, storage, "ry-07.rom", "5b5cf70e-f7f4-36b7-851c-e56b6b3021b0");
      write(0x8000, storage, "ry-06.rom", "d1f11a1e-876f-343b-9287-0fe0022739b8");
    }

    @Override
    public void open() {
      highScore = new HighScore(this);
      super.open();
    }

    public static final int IDLE_ADDRESS = 0x00A5;
    @Override
    public void run() {
      if (!isOpen()) return;
      for (Integer i = stdin.poll(); i != null; i = stdin.poll()) {
        try {
          switch (ControllerCommand.valueOf(i)) {
            case UP: upDown = 1; break;
            case DOWN: upDown = -1; break;
            case RIGHT: leftRight = 1; break;
            case LEFT: leftRight = -1; break;
            case ATTACK: btnAttack = true; break;
            case JUMP: btnJump = true; break;
            case UP0: if (upDown > 0) upDown = 0; break;
            case DOWN0: if (upDown < 0) upDown = 0; break;
            case RIGHT0: if (leftRight > 0) leftRight = 0; break;
            case LEFT0: if (leftRight < 0) leftRight = 0; break;
            case ATTACK0: btnAttack = false; break;
            case JUMP0: btnJump = false; break;
            case START: btnStart = true; break;
            case CHEAT:
              writeByte(0xD802, readByte(0xD802) | 1);
              break;
          }
        } catch (IllegalArgumentException e) {
          // ignore poison messages
        }
      }
      int controller = 0xFF;
      if (upDown > 0) controller ^= 32;
      if (upDown < 0) controller ^= 16;
      if (leftRight > 0) controller ^= 8;
      if (leftRight < 0) controller ^= 4;
      if (btnJump) controller ^= 2;
      if (btnAttack) controller ^= 1;
      stderr.add(String.format("ERR1C,%s%s%s%s%02X%02X",
          upDown > 0 ? 'U' : (upDown < 0 ? 'D' : '-'),
          leftRight > 0 ? 'R' : (leftRight < 0 ? 'L' : '-'),
          btnJump ? 'J' : '-',
          btnAttack ? 'A' : '-',
          readByte(0xD804), controller
      ));
      writeByte(0xD804, controller);
      runToAddress(IDLE_ADDRESS);
      rst(0x38);
      if (btnStart) {
        int coin = readByte(0xD803);
        writeByte(0xD803, coin & 0xEE);
        for (int i = 0; i < 8; i++) {
          runToAddress(IDLE_ADDRESS);
          rst(0x38);
        }
        writeByte(0xD803, coin | 0x11);
        btnStart = false;
      }
      highScore.run();
    }

    @Override
    public int readByte(int address) {
      if ((address < 0xC800) || // 0000-C000 rom + C000-C800 video ram
          (address >= 0xE000 && address < 0xE800) // ram
      ) {
        return super.readByte(address);
      }
      // 95% ----
      if ((address >= PALETTE_BANK) && (address < PALETTE_BANK + 0x200)) { // palette
        address -= PALETTE_BANK;
        address += (super.readByte(GFXCTRL) & 0x20) == 0 ? PALETTE_BANK_0 : PALETTE_BANK_1;
      }
      return super.readByte(address);
    }

    @Override
    public void writeByte(int address, int data) {
      if ((address >= 0xC000 && address < 0xC800) || // video ram
          (address >= 0xE000) && (address < 0xE800) // ram
      ) {
        super.writeByte(address, data);
        return;
      }
      // 95% ----
      if (address == SOUND_LATCH) { // soundlatch
        stdout.add(data);
      }
      if ((address >= PALETTE_BANK) && (address < PALETTE_BANK + 0x200)) { // palette
        address -= PALETTE_BANK;
        address += (super.readByte(GFXCTRL) & 0x20) == 0 ? PALETTE_BANK_0 : PALETTE_BANK_1;
      }
      super.writeByte(address, data);
    }
  }

  public static class HighScore implements Runnable {

    public static class Score {
      int score;
      String name;
      public Score(int score, String name) {
        this.score = score;
        this.name = name;
      }
      @Override
      public int hashCode() {
        return name.hashCode() + score;
      }
      @Override
      public boolean equals(Object obj) {
        return ((obj instanceof Score)
            && (((Score) obj).score == this.score)
            && (((Score) obj).name.equals(this.name)));
      }
    }

    private final Path path;
    private final TyphoonMachine memory;
    private UUID checksum;
    private final Set<Score> table;
    private boolean writtenOnce;

    public HighScore(TyphoonMachine memory) {
      this.memory = memory;
      try {
        Path tempFile = Files.createTempFile("TyphoonGal.", ".txt");
        Files.deleteIfExists(tempFile);
        this.path = tempFile.resolveSibling("TyphoonGal.txt");
        table = Files.exists(path) ? Files.readAllLines(path).stream().map(s -> {
          String[] split = s.split("\\s", 2);
          if (split.length < 2) return null;
          return new Score(Integer.parseInt(split[0]), split[1]);
        }).filter(Objects::nonNull).collect(Collectors.toSet()) : new HashSet<>();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public static final int ADDRESS_TOP10_TEXT = 0xC158;
    public static final int ADDRESS_TOP10 = 0xE1D7;
    public static final int ADDRESS_PLAYER_SCORE = 0xE26A;
    public static final int ADDRESS_HIGH_SCORE = 0xE188;
    public static final UUID TOP10 = UUID.fromString("b7a0efac-cade-347b-9c23-4087cfdc3ec3");
    @Override
    public void run() {
      if (!TOP10.equals(UUID.nameUUIDFromBytes(memory.read(ADDRESS_TOP10_TEXT, new byte[18]))) &&
          writtenOnce) return;
      byte[] m = new byte[130];
      memory.read(ADDRESS_TOP10, m);
      UUID uuid = UUID.nameUUIDFromBytes(m);
      if (uuid.equals(checksum)) return;
      checksum = uuid;
      for (int i = 0; i < 10; i++) {
        int score = 0;
        for (int j = 2; j >= 0; j--) {
          score += (m[i * 3 + j] >> 4) & 15;
          score *= 10;
          score += m[i * 3 + j] & 15;
          score *= 10;
        }
        byte[] bytes = Arrays.copyOfRange(m, i * 10 + 30, i * 10 + 40);
        String s = new String(bytes, StandardCharsets.ISO_8859_1);
        table.add(new Score(score, s));
      }
      List<Score> top = table.stream().sorted(Comparator.comparingInt(e -> -e.score)).collect(Collectors.toList());
      String content = top.stream().map(e -> e.score + " " + e.name).collect(Collectors.joining("\n"));
      try {
        Files.writeString(path, content);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      if (writtenOnce) return;
      writtenOnce = true;
      for (int i = 0; i < 10; i++) {
        Score score = top.get(i);
        int sc = score.score;
        for (int j = 0; j < 3; j++) {
          sc /= 10;
          m[i * 3 + j] = (byte) (sc % 10);
          sc /= 10;
          m[i * 3 + j] |= ((sc % 10) << 4);
        }
        byte[] bytes = score.name.getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(bytes, 0, m, i * 10 + 30, 10);
      }
      memory.write(ADDRESS_TOP10, m);
      memory.write(ADDRESS_HIGH_SCORE, Arrays.copyOf(m, 3));
    }
  }
}
