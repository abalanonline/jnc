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

package info.ab;

import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.gif.GIFImageReaderSpi;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;

public class AnimatedImage {

  private final ArrayList<Image> frames = new ArrayList<>();

  @SneakyThrows
  public AnimatedImage(URL url) {
    ImageReader gifImageReader = new GIFImageReader(new GIFImageReaderSpi());
    gifImageReader.setInput(ImageIO.createImageInputStream(url.openStream()));
    for (int i = 0; i < gifImageReader.getNumImages(true); i++) {
      frames.add(gifImageReader.read(i));
    }
  }

  public Image getFrame(int i) {
    return frames.get(i % frames.size());
  }

}
