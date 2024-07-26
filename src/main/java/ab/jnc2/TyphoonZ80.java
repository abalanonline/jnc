/*
 * Copyright (C) 2022 Aleksei Balan
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
