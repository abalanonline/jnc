package ab.jnc3;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FontBuilder {

  public static byte[] sha1check(byte[] bytes, String sha1) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new Error();
    }
    byte[] hashBytes = digest.digest(bytes);
    String hash = IntStream.range(0, hashBytes.length).mapToObj(i -> String.format("%02x", hashBytes[i]))
        .collect(Collectors.joining());
    if (hash.equals(sha1)) return bytes;
    throw new IllegalArgumentException(hash);
  }

  public static void arraycopy8(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
    System.arraycopy(src, srcPos * 8, dest, destPos * 8, length * 8);
  }

  public static BitmapFont c64(byte[] b_901225_01_u5) {
    BitmapFont font = new BitmapFont(8, 8);
    byte[] b = sha1check(b_901225_01_u5, "adc7c31e18c7c7413d54802ef2f4193da14711aa");
    arraycopy8(b, 0x020, font.bitmap, 0x20, 0x20); // 20-3F
    arraycopy8(b, 0x000, font.bitmap, 0x40, 0x20); // 40-5F lower mapped to ascii
    arraycopy8(b, 0x100, font.bitmap, 0x60, 0x20); // 60-7F
    arraycopy8(b, 0x060, font.bitmap, 0xA0, 0x20); // A0-BF unshifted upper
    arraycopy8(b, 0x040, font.bitmap, 0xC0, 0x20); // C0-DF
    arraycopy8(b, 0xE9, font.bitmap, 0x10, 1);
    arraycopy8(b, 0xDF, font.bitmap, 0x11, 1);
    font.cacheBitmap();
    return font;
  }

  @Test
  void run() throws IOException {
    Files.write(Paths.get("../assets/c64.psf"), c64(Files.readAllBytes(Paths.get("../assets/901225-01.u5"))).toPsf());
  }

}
