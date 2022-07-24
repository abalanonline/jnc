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

import com.codingrodent.microprocessor.IMemory;
import com.codingrodent.microprocessor.ProcessorException;
import com.codingrodent.microprocessor.Z80.Z80Core;

public class TyphoonZ80 extends Z80Core {
  public TyphoonZ80(TyphoonMachine memory) {
    super(new Memory(memory), null);
  }

  public int runToAddress(final int address) {
    int counter = 0;
    try {
      do {
        super.executeOneInstruction();
        counter++;
      } while (getProgramCounter() != address);
    } catch (ProcessorException e) {
      throw new IllegalStateException(e);
    }
    return counter;
  }

  public void executeOneInstruction() {
    try {
      super.executeOneInstruction();
    } catch (ProcessorException e) {
      throw new IllegalStateException(e);
    }
  }

  public static class Memory implements IMemory {
    private final TyphoonMachine memory;

    public Memory(TyphoonMachine memory) {
      this.memory = memory;
    }

    @Override
    public int readByte(int address) {
      return memory.readByte(address);
    }

    @Override
    public int readWord(int address) {
      return memory.readWord(address);
    }

    @Override
    public void writeByte(int address, int data) {
      memory.writeByte(address, data);
    }

    @Override
    public void writeWord(int address, int data) {
      memory.writeWord(address, data);
    }

  }
}
