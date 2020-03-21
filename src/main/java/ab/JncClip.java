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

import fr.delthas.javamp3.Sound; // MIT License
import lombok.Getter;
import lombok.SneakyThrows;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class JncClip {

//    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("music.wav"));
//    DataLine.Info info = new DataLine.Info(Clip.class, audioInputStream.getFormat());
//    Clip line = (Clip)AudioSystem.getLine(info);
//    line.open(audioInputStream.getFormat(), bytes, 0, (int)audioInputStream.getFrameLength());
//    line.open(sound.getAudioFormat(), outputStream.toByteArray(), 0, outputStream.size());
//    line.start();

  @Getter
  private Clip line;

  @SneakyThrows
  public JncClip(InputStream in) {

    // create decoder object
    Sound sound = new Sound(in);

    // decode to byte array
    final int BUFFER_SIZE = 4096;
    ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
    int byteCount = 0;
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
      out.write(buffer, 0, bytesRead);
      byteCount += bytesRead;
    }

    // audio system
    AudioFormat audioFormat = sound.getAudioFormat();
    DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
    line = (Clip) AudioSystem.getLine(info);
    line.open(audioFormat, out.toByteArray(), 0, out.size());
  }

}
