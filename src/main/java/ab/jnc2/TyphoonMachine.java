/*
 * Copyright 2022 Aleksei Balan
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

package ab.jnc2;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class TyphoonMachine implements Runnable, AutoCloseable {
  private final byte[] memory;
  protected TyphoonZ80 z80;
  protected final Queue<Integer> stdin;
  protected final Queue<Integer> stdout;
  protected final Queue<String> stderr;
  private boolean open;

  public TyphoonMachine(Queue<Integer> stdin, Queue<Integer> stdout, Queue<String> stderr) {
    this(stdin, stdout, stderr, 0x10000);
  }

  public TyphoonMachine(Queue<Integer> stdin, Queue<Integer> stdout, Queue<String> stderr, int memory) {
    this.stdin = stdin;
    this.stdout = stdout;
    this.stderr = stderr;
    this.memory = new byte[memory];
  }

  /**
   * Load the required files (if any) from storage.
   * @param storage map of file names and binary content.
   * @throws RuntimeException if failed to load the required file
   */
  public void load(Map<String, byte[]> storage) {
  }

  /**
   * Start the machine.
   */
  public void open() {
    z80 = new TyphoonZ80(this);
    z80.reset();
    open = true;
  }

  /**
   * Is it started?
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Stop the machine.
   */
  @Override
  public void close() {
    open = false;
    z80 = null;
  }

  public int runToAddress(final int address) {
    return z80.runToAddress(address);
  }

  public void rst(int address) {
    if (address == 0x66) {
      z80.setNMI();
      return;
    }
    assert (address & 0xFFC7) == 0;
    byte opcode = (byte) (0xC7 + address);
    int pc = (z80.getProgramCounter() - 1) & 0xFFFF;
    z80.setProgramCounter(pc);
    byte b = memory[pc];
    memory[pc] = opcode;
    z80.executeOneInstruction();
    memory[pc] = b;
  }

  public void write(int address, byte[] data) {
    System.arraycopy(data, 0, memory, address, data.length);
  }

  public void write(int address, Map<String, byte[]> storage, String file, String md5) {
    byte[] data = storage.get(file);
    if (data == null || (md5 != null && !UUID.fromString(md5).equals(UUID.nameUUIDFromBytes(data)))) {
      String checksum = data == null ? "" : " " + UUID.nameUUIDFromBytes(data);
      throw new IllegalStateException("file " + file + " not found or checksum error" + checksum);
    }
    write(address, data);
  }

  public byte[] read(int address, byte[] data) {
    System.arraycopy(memory, address, data, 0, data.length);
    return data;
  }

  @Override
  public void run() {
    if (!isOpen()) return;
    z80.executeOneInstruction();
  }

  public int readByte(int address) {
    return memory[address] & 0xFF;
  }

  public int readWord(int address) {
    return readByte(address) | (readByte((address + 1) & 0xFFFF) << 8);
  }

  public void writeByte(int address, int data) {
    memory[address] = (byte) data;
  }

  public void writeWord(int address, int data) {
    writeByte(address, data);
    writeByte((address + 1) & 0xFFFF, data >> 8);
  }

}
