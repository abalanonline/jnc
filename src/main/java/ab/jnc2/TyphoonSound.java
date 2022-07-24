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

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;

/**
 * Typhoon Sound System.
 */
public class TyphoonSound extends TyphoonMachine {

  public static final int YM_ADDR = 0xC800;
  public static final int YM_DATA = 0xC801;
  public static final int YM_REGISTER = 0xC810;
  public static final int MSM_REGISTER = 0xCA00;
  public static final int SOUND_LATCH = 0xD800;
  public static final int A440_MIDI = 69;
  public static final int MIDI_DEFAULT_VELOCITY = 0x60;

  MidiChannel[] midi;
  Instrument[] instruments;
  Synthesizer synthesizer;
  Msm5232 msm;

  public static class Msm5232 implements Runnable {
    public static final int A440_MIDI = 69;
    public static final int A440_MSM = 0x21;
    public static final int[] ATTACK = {2, 4, 8, 16, 32, 64, 32, 64};
    public static final int[] DECAY = {40, 80, 160, 320, 640, 1300, 640, 1300,
        330, 500, 1000, 2000, 4000, 8000, 4000, 8000};
    private final MidiChannel midi1;
    private final MidiChannel midi2;
    private final MidiChannel percussion;

    private byte[] registers = new byte[0x0E];
    private int[] notes = new int[8];
    private Instant[] noteOff = new Instant[8];

    public Msm5232(MidiChannel left, MidiChannel right, MidiChannel percussion) {
      this.midi1 = left;
      this.midi2 = right;
      this.percussion = percussion;
    }

    @Override
    public void run() {
      Instant now = Instant.now();
      for (int tg = 0; tg < 8; tg++) {
        if (noteOff[tg] != null && now.isAfter(noteOff[tg])) {
          noteOff(tg);
        }
      }
    }

    void set(int register, byte data) {
      registers[register] = data;
      if (register >= 8) return;
      int group = (register < 4) ? 1 : 2;
      noteOn(register, data, ATTACK[registers[7 + group] & 0x07], DECAY[registers[9 + group] & 0x0F]);
    }

    private void noteOff(int tg) {
      if (noteOff[tg] != null) {
        (tg < 4 ? midi1 : midi2).noteOff(notes[tg]);
        noteOff[tg] = null;
      }
    }

    private void noteOn(int tg, byte note, int attack, int decay) {
      noteOff(tg); // release the key before playing next
      if ((note & 0x80) == 0) {
        return;
      }
      note = (byte) (note & 0x7F);
      if (note >= 0x58) {
        percussion.noteOn(49, MIDI_DEFAULT_VELOCITY); // TODO: 2022-07-17 noteOff
        return;
      }
      int midiNote = note - A440_MSM + A440_MIDI;
      notes[tg] = midiNote;
      (tg < 4 ? midi1 : midi2).noteOn(midiNote, MIDI_DEFAULT_VELOCITY);
      noteOff[tg] = Instant.now().plusMillis(attack + decay);
    }
  }

  public TyphoonSound(Queue<Integer> stdin, Queue<Integer> stdout, Queue<String> stderr) {
    super(stdin, stdout, stderr);
  }

  @Override
  public void load(Map<String, byte[]> storage) {
    write(0x0000, storage, "a52-12.08s", "8c2287fb-2796-3482-864c-dcefece10dc9");
    write(0x2000, storage, "a52-13.09s", "78b50951-d48a-3157-96e1-f63403b03d19");
    write(0x4000, storage, "a52-14.10s", "ddc80e6d-3cc1-3ba1-9bce-5b9c429ce3b8");
    write(0x6000, storage, "a52-15.37s", "e50f914e-77c6-3acb-9a82-b204e75a95b2");
    write(0x8000, storage, "a52-16.38s", "11a72fe4-3e14-3cb4-9e21-ae8cc07e530a");
  }

  @Override
  public void open() {
    try {
      synthesizer = MidiSystem.getSynthesizer();
      synthesizer.open();
    } catch (MidiUnavailableException e) {
      throw new IllegalStateException(e);
    }
    instruments = synthesizer.getDefaultSoundbank().getInstruments();
    midi = synthesizer.getChannels();
    synthesizer.loadInstrument(instruments[5]); // 6 - FM Electric Piano

    midi[7].programChange(5);
    midi[7].controlChange(8, 0); // left
    midi[8].programChange(5);
    midi[8].controlChange(8, 127); // right
    msm = new Msm5232(midi[7], midi[8], midi[9]);

    super.open();
  }

  @Override
  public void close() {
    synthesizer.close();
    super.close();
  }

  public void programChangeMsm(int programNumber) {
    synthesizer.loadInstrument(instruments[programNumber - 1]);
    for (int i = 7; i < 9; i++) {
      midi[i].programChange(programNumber - 1);
    }
  }

  public static final int IDLE_ADDRESS = 0x0160;
  @Override
  public void run() {
    if (!isOpen()) return;
    msm.run();
    runToAddress(IDLE_ADDRESS);
    Integer command = stdin.poll();
    if (command != null) {
      if ((command | 0xFF) == 0xFF) {
        writeByte(SOUND_LATCH, command);
        rst(0x66);
      }
      if ((command | 0xFF) == 0x01FF) {
        programChangeMsm(command & 0xFF);
      }
    }
    runToAddress(IDLE_ADDRESS);
    rst(0x38);
    runToAddress(IDLE_ADDRESS);
    rst(0x38);
    //playYm();
  }

  public static int midiNoteNumber(double frequency) {
    double log2 = Math.log(frequency / 440.0) / Math.log(2);
    double note = A440_MIDI + log2 * 12;
    return (int) Math.round(note);
  }

  public void playYm() {
    int mixerMute = super.readByte(YM_REGISTER + 7);
    for (int i = 0; i < 3; i++) {
      if (((mixerMute >> i) & 1) != 0) {
        midi[i].allNotesOff();
        continue;
      }
      int mixerVolume = super.readByte(YM_REGISTER + 8 + i);
      mixerVolume &= 0x0F;
      midi[i].controlChange(7, (mixerVolume << 1) + 40);
      int frequency = super.readWord(YM_REGISTER + 2 * i);
      frequency &= 0x0FFF;
      midi[i].noteOn(midiNoteNumber(2_000_000 / 16.0 / frequency), MIDI_DEFAULT_VELOCITY);
    }
  }

  @Override
  public int readByte(int address) {
    if ((address < 0xA000) || // rom
        (address >= 0xC300 && address < 0xC800) // ram
    ) {
      return super.readByte(address);
    }
    // 95% ----
    if ((address == YM_ADDR) || // writeByte(YM_DATA) readByte(YM_ADDR)
        (address == 0xE000) // rom extension
    ) {
      return super.readByte(address);
    }
    if (address == SOUND_LATCH) { // soundlatch
      return super.readByte(address);
    }
    return super.readByte(address);
  }

  @Override
  public void writeByte(int address, int data) {
    if (address >= 0xC000 && address < 0xC800) { // ram
      super.writeByte(address, data);
      return;
    }
    // 95% ----
    if (address == YM_DATA) {
      int ymAddr = readByte(YM_ADDR);
      super.writeByte(address, data);
      stderr.add(String.format("ERR%02X,%02X", 0x190 + ymAddr, data));
      return;
    }
    if ((address >= MSM_REGISTER && address < MSM_REGISTER + 0x0E)) {
      msm.set(address - MSM_REGISTER, (byte) data);
      super.writeByte(address, data);
      stderr.add(String.format("ERR%02X,%02X", address - MSM_REGISTER + 0x180, data));
      return;
    }
    super.writeByte(address, data);
  }
}
