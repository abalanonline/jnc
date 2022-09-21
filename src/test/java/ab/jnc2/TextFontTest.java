package ab.jnc2;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TextFontTest {

  /**
   * PICO-8 font. (CC0 1.0)
   * https://www.lexaloffle.com/pico-8.php?page=faq
   */
  @Test
  @Disabled
  void pico8() throws IOException {
    BufferedImage png = ImageIO.read(new FileInputStream("pico-8_font.png"));
    assert png.getWidth() == 0x80;
    assert png.getHeight() == 0x80;
    ByteBuffer bytes = ByteBuffer.allocate(0x800);
    int rgb0 = png.getRGB(0, 0);
    for (int y = 0; y < 0x10; y++) {
      for (int x = 0; x < 0x10; x++) {
        int yh = y;
        int xh = x;
        if ((yh | 2) == 6 && xh > 0 || (yh | 2) == 7 && xh < 0x0B) {
          yh ^= 2; // ASCII uppercase/lowercase feature fix
        }
        for (int yl = 0; yl < 0x08; yl++) {
          byte b = 0;
          for (int xl = 0; xl < 0x08; xl++) {
            b <<= 1;
            b |= png.getRGB(xh * 8 + xl, yh * 8 + yl) == rgb0 ? 0 : 1;
          }
          bytes.put(b);
        }
      }
    }
    Files.write(Paths.get("src/main/resources/pico-8.fnt"), bytes.array());
  }
}
