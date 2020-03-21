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

import lombok.SneakyThrows;

import java.io.FileInputStream;

public class TestAudioSystem implements Runnable {

  public TestAudioSystem(String[] args) {
  }

  @SneakyThrows
  @Override
  public void run() {
    JncClip clip = new JncClip(new FileInputStream("music.mp3"));
    clip.getLine().start();

    Thread.sleep(10000);
  }

  public static void main(String[] args) {
    new TestAudioSystem(args).run();
  }

}
