package ab.jnc2;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

class TextFontTest {

  public static final String FOX_S = "The quick brown fox jumps over the lazy dog";
  public static final int FOX_X = 0x84;
  public static final int FOX_Y = 0x20;

  /**
   * PICO-8 font. (CC0 1.0)
   * https://www.lexaloffle.com/pico-8.php?page=faq
   */
  @Test
  @Disabled
  void pico8() throws IOException {
    BufferedImage png = ImageIO.read(new FileInputStream("pico-8_font_022.png"));
    int pngWidth = png.getWidth();
    int pngHeight = png.getHeight();
    assert pngWidth % 0x80 == 0;
    int zoom = pngWidth / 0x80;
    ByteBuffer bytes = ByteBuffer.allocate(0x800);
    int rgb0 = getRgb(png, 0, 0, zoom);
    for (char c = 0; c < 0x100; c++) {
      int yh = c / 0x10;
      int xh = c % 0x10;
      if ((c | 0x20) >= 'a' && (c | 0x20) <= 'z') {
        yh ^= 2; // ASCII uppercase/lowercase feature fix
      }
      if ((yh + 1) * 8 * zoom > pngHeight) break;
      for (int yl = 0; yl < 0x08; yl++) {
        byte b = 0;
        for (int xl = 0; xl < 0x08; xl++) {
          b <<= 1;
          b |= getRgb(png, xh * 8 + xl, yh * 8 + yl, zoom) == rgb0 ? 0 : 1;
        }
        bytes.put(b);
      }
    }
    Files.write(Paths.get("src/main/resources/jnc2/pico-8.fnt"), bytes.array());

    TextFont textFont = new TextFont(bytes.array(), 0, 8, 8).width(4).height(6);
    Screen screen = new Screen(GraphicsMode.CGA_16);
    screen.flicker(10, () -> {
      int fw = (screen.mode.resolution.width - FOX_X) / textFont.width;
      int fl = FOX_S.length();
      for (int fx = 0, y = FOX_Y; fx < fl; fx += fw, y += textFont.height) {
        textFont.print(screen.image, FOX_S.substring(fx, Math.min(fx + fw, fl)), FOX_X, y, -1);
      }
      textFont.preview(screen.image);
    });
  }

  public static byte[] readInterleaved(String... files) {
    byte[][] banks = Arrays.stream(files).map(file -> {
      try {
        return Files.readAllBytes(Paths.get(file));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }).toArray(byte[][]::new);
    int length = banks[0].length;
    for (byte[] bank : banks) {
      assert bank.length == length;
    }
    byte[] result = new byte[length * banks.length];
    for (int i = 0, j = 0; i < length; i++) {
      for (int b = 0; b < banks.length; b++) {
        result[j++] = banks[b][i];
      }
    }
    return result;
  }

  @Test
  @Disabled
  void vgaFont() throws IOException {
    byte[] bytes = null;
    bytes = Arrays.copyOfRange(readInterleaved("15F8366.BIN", "15F8365.BIN"), 0x51B4, 0x7A25); // Model 50
    bytes = Arrays.copyOfRange(readInterleaved("90X6816.BIN", "90X6817.BIN"), 0x5266, 0x7AD7); // Model 60
    Files.write(Paths.get("src/main/resources/jnc2/vga.fnt"), bytes);
  }

  private static int getRgb(BufferedImage png, int x, int y, int zoom) {
    return png.getRGB(x * zoom, y * zoom);
  }
}
