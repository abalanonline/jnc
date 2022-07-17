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

import com.codingrodent.microprocessor.IBaseDevice;
import com.codingrodent.microprocessor.IMemory;
import com.codingrodent.microprocessor.ProcessorException;
import com.codingrodent.microprocessor.Z80.CPUConstants;
import com.codingrodent.microprocessor.Z80.Z80Core;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * Can this object be both cpu and memory? Technically not because memory must be an argument for cpu
 * constructor. And it is the same object, so there is no reference to it before constructing.
 * Forward compatibility is not a concern. Is it the time to unlock private variables? Not yet.
 * There are two cpus, superclass inactive "is-a" and constructed with memory "has-a".
 * With all the overridden methods rewired to "has-a" object, there is no difference for the object users.
 * The power of duck typing, quack!
 */
public class TyphoonZ80 extends Z80Core implements IMemory, IBaseDevice, Runnable {
  private final byte[] memory;
  private final Z80Core z80;
  protected final Queue<Integer> stdin;
  protected final Queue<Integer> stdout;
  protected final Queue<String> stderr;

  public TyphoonZ80(Queue<Integer> stdin, Queue<Integer> stdout, Queue<String> stderr) {
    this(stdin, stdout, stderr, 0x10000);
  }

  public TyphoonZ80(Queue<Integer> stdin, Queue<Integer> stdout, Queue<String> stderr, int memory) {
    super(null, null);
    this.stdin = stdin;
    this.stdout = stdout;
    this.stderr = stderr;
    this.memory = new byte[memory];
    z80 = new Z80Core(this, this);
    z80.reset();
    executeOneInstruction();
    executeOneByteInstruction(0x76); // halt
  }

  public int runToAddress(final int address) {
    int counter = 0;
    try {
      do {
        z80.executeOneInstruction();
        counter++;
      } while (z80.getProgramCounter() != address);
    } catch (ProcessorException e) {
      throw new IllegalStateException(e);
    }
    return counter;
  }

  private void executeOneByteInstruction(int opcode) {
    int pc = (getProgramCounter() - 1) & 0xFFFF;
    setProgramCounter(pc);
    byte b = memory[pc];
    memory[pc] = (byte) opcode;
    executeOneInstruction();
    memory[pc] = b;
  }

  public void rst(int address) {
    if (address == 0x66) {
      setNMI();
      return;
    }
    assert (address & 0xFFC7) == 0;
    executeOneByteInstruction(0xC7 + address);
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

  /**
   * Load the required files (if any) from storage.
   * @param storage map of file names and binary content.
   * @throws RuntimeException if failed to load the required file
   */
  public void load(Map<String, byte[]> storage) {
  }

  public void read(int address, byte[] data) {
    System.arraycopy(memory, address, data, 0, data.length);
  }

  @Override
  public void run() {
    if (getHalt()) return;
    executeOneInstruction();
  }

  @Override
  public int readByte(int address) {
    return memory[address] & 0xFF;
  }

  @Override
  public int readWord(int address) {
    return readByte(address) | (readByte((address + 1) & 0xFFFF) << 8);
  }

  @Override
  public void writeByte(int address, int data) {
    memory[address] = (byte) data;
  }

  @Override
  public void writeWord(int address, int data) {
    writeByte(address, data);
    writeByte((address + 1) & 0xFFFF, data >> 8);
  }

  @Override
  public int IORead(int address) {
    throw new IllegalStateException("IO read " + address);
  }

  @Override
  public void IOWrite(int address, int data) {
    throw new IllegalStateException("IO write " + address);
  }

  @Override
  public boolean blockMoveInProgress() {
    return z80.blockMoveInProgress();
  }

  @Override
  public void reset() {
    z80.reset();
  }

  @Override
  public void setNMI() {
    z80.setNMI();
  }

  @Override
  public boolean getHalt() {
    return z80.getHalt();
  }

  @Override
  public int getProgramCounter() {
    return z80.getProgramCounter();
  }

  @Override
  public void setProgramCounter(int pc) {
    z80.setProgramCounter(pc);
  }

  @Override
  public void setResetAddress(int address) {
    z80.setResetAddress(address);
  }

  @Override
  public int getRegisterValue(CPUConstants.RegisterNames name) {
    return z80.getRegisterValue(name);
  }

  @Override
  public int getSP() {
    return z80.getSP();
  }

  @Override
  public void executeOneInstruction() {
    try {
      z80.executeOneInstruction();
    } catch (ProcessorException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public long getTStates() {
    return z80.getTStates();
  }

  @Override
  public void resetTStates() {
    z80.resetTStates();
  }
}
