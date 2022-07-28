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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Typhoon Sound System.
 */
public class TyphoonSound extends TyphoonMachine {

  public static final int YM_ADDR = 0xC800;
  public static final int YM_DATA = 0xC801;
  public static final int YM_VOLUME = 0x1000; // MAX/8
  public static final int MSM_REGISTER = 0xCA00;
  public static final int SOUND_LATCH = 0xD800;
  public static final int A440_MIDI = 69;
  public static final int MIDI_DEFAULT_VELOCITY = 0x60;
  public static final int SAMPLE_RATE = 44100;
  public static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

  MidiChannel[] midi;
  Instrument[] instruments;
  Synthesizer synthesizer;
  SourceDataLine line;
  AtomicReference<int[]> wav = new AtomicReference<>();
  Msm5232 msm;
  Ym2149 ym;

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

  public static class Ym2149 implements Runnable {
    public static final int[] DA_VOLTAGE = {
        5, 6, 7, 8, 9, 11, 13, 16, 19, 22, 26, 31, 37, 44, 53, 63,
        74, 88, 105, 125, 149, 177, 210, 250, 297, 354, 420, 500, 595, 707, 841, 1000,
    };
    private final int clockRate;
    private byte[] registers = new byte[0x10];
    private final AtomicReference<int[]> wav;
    private final byte[] noise;
    private int[] chd = new int[5]; // divisor
    private int[] chq = new int[5]; // quotient
    private int[] chr = new int[5]; // remainder

    public Ym2149(int clockRate, AtomicReference<int[]> wav) {
      this.clockRate = clockRate;
      this.wav = wav;
      noise = new byte[0x1000];
      ThreadLocalRandom.current().nextBytes(noise);
    }

    @Override
    public void run() {
      final int[] wav = this.wav.get();
      for (int sample = 0; sample < wav.length; sample++) {
        int v = 0;
        int ticks = 45; // FIXME: 2022-07-27 fixed point 2_000_000 / 44_100

        // increment oscillators
        for (int ch = 0; ch < 5; ch++) {
          int d = (registers[2 * ch] & 0xFF) | (registers[2 * ch + 1] << 8);
          d &= ch < 3 ? 0x0FFF : 0x001F;
          if (ch == 4) {
            d = (registers[11] & 0xFF) | (registers[12] << 8);
          }
          d <<= 3;
          if (d == 0) { d = 1; } // undocumented
          if (chr[ch] >= d) { // decreased during playback
            chq[ch]++;
            chr[ch] = 0;
          }
          int t = chr[ch] + ticks;
          chd[ch] = d;
          chq[ch] += t / d;
          chr[ch] = t % d;
        }

        // noise and envelope volume
        boolean n = (noise[(chq[3] >> 3) & 0xFFF] & (1 << (chq[3] & 7))) == 0;
        int envv = chq[4]; // volume
        int envs = registers[13] & 0x0F; // shape
        envs = envs < 8 ? (envs < 4 ? 1 : 7) : envs - 8;
        if (envv >= 0x20) {
          envv = (envs & 1) == 0 ? envv : -1;
          envv = envv ^ ((envs & (envv >> 4) & 2) == 0 ? 0 : -1); // chiptunes are about bit logic
        }
        envv = (envv ^ ((envs & 4) == 0 ? -1 : 0)) & 0x1F;

        // make tone/noise with fixed/variable volume
        for (int ch = 0; ch < 3; ch++) {
          int volume = registers[8 + ch];
          volume = volume > 0x0F ? envv : volume * 2 + 1;
          volume = DA_VOLTAGE[volume] * YM_VOLUME / 1000;
          if ((registers[7] & 1 << ch) == 0) {
            v += ((chq[ch] & 1) == 0 ? 1 : -1) * volume;
          }
          if ((registers[7] & 8 << ch) == 0) {
            v += (n ? 1 : -1) * volume;
          }
        }
        wav[sample] += v;
      }
    }

    void set(int register, byte data) {
      registers[register] = data;
      if (register == 13) { // envelope reset
        chq[4] = 0;
        chr[4] = 0;
      }
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
      int pingMs = 50; // 17 - min NTSC, 2*min - ok, 50 - good, 500 - java default
      line = AudioSystem.getSourceDataLine(AUDIO_FORMAT);
      line.open(AUDIO_FORMAT, (int) (AUDIO_FORMAT.getFrameRate() * pingMs / 1000) * AUDIO_FORMAT.getFrameSize());
    } catch (LineUnavailableException e) {
      throw new IllegalStateException(e);
    }
    line.start();
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
    ym = new Ym2149(2_000_000, wav);

    super.open();
  }

  @Override
  public void close() {
    synthesizer.close();
    line.drain();
    line.stop();
    line.close();
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

    final int[] wav = new int[line.available() / AUDIO_FORMAT.getFrameSize()];
    this.wav.set(wav);
    ym.run();
    if (wav.length > 0) {
      byte[] bytes = new byte[wav.length * AUDIO_FORMAT.getFrameSize()];
      for (int wi = 0, i = 0; wi < wav.length; wi++) {
        int v = wav[wi];
        v = Math.min(v, Short.MAX_VALUE);
        v = Math.max(v, Short.MIN_VALUE);
        for (int channel = 0; channel < AUDIO_FORMAT.getChannels(); channel++) {
          bytes[i++] = (byte) v;
          bytes[i++] = (byte) (v >> 8);
        }
      }
      line.write(bytes, 0, bytes.length);
    }
  }

  public static int midiNoteNumber(double frequency) {
    double log2 = Math.log(frequency / 440.0) / Math.log(2);
    double note = A440_MIDI + log2 * 12;
    return (int) Math.round(note);
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
      int ymAddr = super.readByte(YM_ADDR);
      ym.set(ymAddr, (byte) data);
      super.writeByte(address, data);
      stderr.add(String.format("ERR%02X,%02X", 0x190 + ymAddr, data));
      return;
    }
    if ((address >= MSM_REGISTER && address < MSM_REGISTER + 0x0E)) {
      // audiocpu.mb@2b1=0
      msm.set(address - MSM_REGISTER, (byte) data);
      super.writeByte(address, data);
      stderr.add(String.format("ERR%02X,%02X", 0x180 + address - MSM_REGISTER, data));
      return;
    }
    super.writeByte(address, data);
  }
}
