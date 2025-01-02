/*
 * Copyright (C) 2020 Aleksei Balan
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

package ab.jnc1.g3;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class MessageDigest implements Serializable {
  private static final long serialVersionUID = 0x4E79616E20436174L;

  public static final MessageDigest MD5 = new MessageDigest("MD5");
  public static final MessageDigest SHA1 = new MessageDigest("SHA-1");
  public static final MessageDigest SHA256 = new MessageDigest("SHA-256");
  private final java.security.MessageDigest messageDigest;

  public MessageDigest(String algorithm) {
    try {
      messageDigest = java.security.MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public byte[] apply(byte[] bytes) { // new object required for thread safety
    messageDigest.reset();
    messageDigest.update(bytes);
    return messageDigest.digest();
  }

  public long apply(long l1, long l2) {
    return ByteBuffer.wrap(apply(ByteBuffer.allocate(16).putLong(l1).putLong(l2).array())).getLong();
  }

  public Random newRandom(long l1, long l2) {
    return new Random(apply(l1, l2));
  }

  public Random newDependentRandom(Random random) {
    return newRandom(random.nextLong(), serialVersionUID);
  }

}
