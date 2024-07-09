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

package ab.jnc1;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Resource {

  private Map<String, byte[]> byteMap = new HashMap<>();
  @Getter
  private Map<String, Object> objectMap = new HashMap<>();
  private Map<String, String> nameMap = new HashMap<>();

  @Getter @Setter
  private String currentPage = "";

  public Resource(Object object) {
    // read archive to memory
    try {
      readZip(byteMap, object.getClass());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // instantiate objects
    for (String key : byteMap.keySet()) {
      nameMap.put(key, key);
      if (key.endsWith(".gif")) {
        nameMap.put(key.substring(0, key.length() - 4), key);
        objectMap.put(key, new Sprite(new ByteArrayInputStream(byteMap.get(key))));
      }
    }

    // if there is "#" file it have aliases and numbers
  }

  @SneakyThrows
  public void loadStream(String name) {
    InputStream inputStream = this.getClass().getResourceAsStream("/" + name);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    copyStream(inputStream, outputStream);
    byteMap.put(name, outputStream.toByteArray());
    nameMap.put(name, name);
  }

  private static void readZip(Map<String, byte[]> byteMap, Class<?> cl) throws IOException {
    ZipInputStream inputStream = new ZipInputStream(
        cl.getResourceAsStream("/jnc1/" + cl.getSimpleName() + ".zip"));
    ZipEntry zipEntry;
    while ((zipEntry = inputStream.getNextEntry()) != null) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      copyStream(inputStream, outputStream);
      byteMap.put(zipEntry.getName(), outputStream.toByteArray());
    }
    inputStream.close();
  }

  public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[0x10000];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) >= 0) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.flush();
  }

  public byte[] getByteArray(String key) {
    return byteMap.get(nameMap.get(key));
  }

  public Object getObject(String key) {
    String k1 = currentPage;
    String k2 = nameMap.get(key);

    // try to find object with page
    Object object = null;
    if (k1 != null && k1.length() > 0) object = objectMap.get(k1 + "." + k2);

    // and without page
    if (object == null) object = objectMap.get(k2);

    // object is not found
    if (object == null) throw new NullPointerException(key);
    return object;
  }

  // setter for paged object
  public void setObject(String key, Object object, String page) {
    nameMap.putIfAbsent(key, key); // if it is a new name
    objectMap.put((page != null && page.length() > 0 ? page + "." : "") + nameMap.get(key), object);
  }

  public void setObject(String key, Object object) {
    setObject(key, object, null);
  }

  public Sprite getSprite(String key) {
    return (Sprite) getObject(key);
  }

  public Font getFont(String key) {
    return (Font) getObject(key);
  }

}
