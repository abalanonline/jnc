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

package ab;

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
