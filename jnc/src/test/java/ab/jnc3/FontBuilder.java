package ab.jnc3;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FontBuilder {

  public static byte[] filesReadAllBytes(String path) {
    try {
      return Files.readAllBytes(Paths.get(path));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

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
    // https://github.com/mamedev/mame/blob/master/src/mame/commodore/c64.cpp
    byte[] b = sha1check(b_901225_01_u5, "adc7c31e18c7c7413d54802ef2f4193da14711aa");
    BitmapFont font = new BitmapFont(8, 8);
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

  public static BitmapFont msx(byte[] b_hb10bios_ic12) {
    // https://github.com/mamedev/mame/blob/master/src/mame/msx/msx1.cpp
    byte[] b = sha1check(b_hb10bios_ic12, "302afb5d8be26c758309ca3df611ae69cced2821");
    BitmapFont font = new BitmapFont(6, 8);
    System.arraycopy(b, 0x1BBF, font.bitmap, 0, 0x800);
    font.cacheBitmap();
    return font;
  }

  public static BitmapFont zx(byte[] b_spectrum_rom) {
    // https://github.com/mamedev/mame/blob/master/src/mame/sinclair/spectrum.cpp
    byte[] b = sha1check(b_spectrum_rom, "5ea7c2b824672e914525d1d5c419d71b84a426a2");
    BitmapFont font = new BitmapFont(8, 8);
    System.arraycopy(b, 0x3D00, font.bitmap, 0x100, 0x0300);
    font.cacheBitmap();
    return font;
  }

  public static BitmapFont[] vga(byte[] b_90x7423_zm14, byte[] b_90x7426_zm16) {
    // https://github.com/mamedev/mame/blob/master/src/mame/pc/ps2.cpp
    byte[] b0 = sha1check(b_90x7423_zm14, "1af7faa526585a7cfb69e71d90a75e1f1c541586");
    byte[] b1 = sha1check(b_90x7426_zm16, "228f67dc915d84519da7fc1a59b7f9254278f3a0");
    byte[] b = new byte[0x10000];
    for (int i = 0, p = 0; i < 0x8000; i++) {
      b[p++] = b0[i];
      b[p++] = b1[i];
    }
    BitmapFont default8x16 = BitmapFont.fromPsf(filesReadAllBytes("/usr/share/kbd/consolefonts/default8x16.psfu.gz"));
    BitmapFont[] fonts = new BitmapFont[4];
    int[] address = new int[]{8, 0x5266, 14, 0x5A66, 16, 0x6993};
    for (int i = 0, p = 0; i < 3; i++) {
      int h = address[p++];
      BitmapFont font = new BitmapFont(8, h);
      System.arraycopy(b, address[p++], font.bitmap, 0, h * 0x100);
      font.unicodeCache = default8x16.unicodeCache;
      fonts[i] = font;
    }
    return fonts;
  }

  @Disabled
  @Test
  void run() throws IOException {
    Files.write(Paths.get("../assets/c64.psf"), c64(filesReadAllBytes("../assets/901225-01.u5")).toPsf());
    Files.write(Paths.get("../assets/msx.psf"), msx(filesReadAllBytes("../assets/hb10bios.ic12")).toPsf());
    Files.write(Paths.get("../assets/zx.psf"), zx(filesReadAllBytes("../assets/spectrum.rom")).toPsf());
    byte[] b_90x7423_zm14 = filesReadAllBytes("../assets/90x7423.zm14");
    byte[] b_90x7426_zm16 = filesReadAllBytes("../assets/90x7426.zm16");
    BitmapFont[] fonts = vga(b_90x7423_zm14, b_90x7426_zm16);
    Files.write(Paths.get("../assets/vga8.psf"), fonts[0].toPsf());
    Files.write(Paths.get("../assets/vga14.psf"), fonts[1].toPsf());
    Files.write(Paths.get("../assets/vga16.psf"), fonts[2].toPsf());
  }

}
