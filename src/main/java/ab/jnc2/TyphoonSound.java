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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Typhoon Sound System.
 */
public class TyphoonSound extends TyphoonMachine {

  public static final int YM_ADDR = 0xC800;
  public static final int YM_DATA = 0xC801;
  public static final int YM_VOLUME = 0x4000; // personal opinion
  public static final int MSM_REGISTER = 0xCA00;
  public static final int SOUND_LATCH = 0xD800;
  public static final int SAMPLE_RATE = 44100;
  public static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

  SourceDataLine line;
  AtomicReference<int[]> wav = new AtomicReference<>();
  ByteArrayOutputStream dacInput;
  Set<ByteArrayInputStream> dacOutput = new HashSet<>();
  Msm5232 msm;
  Ym2149 ym;

  public static class Msm5232 implements Runnable, AutoCloseable {
    public static final int CHANNEL_DCO1 = 7;
    public static final int CHANNEL_DCO2 = 8;
    public static final int CHANNEL_PERCUSSION = 9;
    public static final int MIDI_DEFAULT_VELOCITY = 0x60;
    public static final int MIDI_DEFAULT_INSTRUMENT = 6; // fm piano

    public static final int C4_MIDI = 60;
    public static final int C4_MSM = 0x24;
    public static final int[] ATTACK = {2, 4, 8, 16, 32, 64, 32, 64};
    public static final int[] DECAY = {40, 80, 160, 320, 640, 1300, 640, 1300,
        330, 500, 1000, 2000, 4000, 8000, 4000, 8000};

    private int[] tgMidiNoteNumber = new int[8];
    private int[] tgMidiChannel = new int[8];
    private Instant[] tgNoteOff = new Instant[8];
    private int[] groupOctave = new int[2];
    private int[] groupWaveform = new int[2];
    private int[] groupAttack = new int[2];
    private int[] groupDecay = new int[2];

    private final Queue<Integer> tgData = new LinkedBlockingDeque<>();
    private Receiver midiReceiver;

    public Msm5232(Receiver midiReceiver) {
      changeDevice(midiReceiver);
      changeInstrument(MIDI_DEFAULT_INSTRUMENT);
    }

    public void changeInstrument(int programNumber) {
      sendShortMessage(ShortMessage.PROGRAM_CHANGE, CHANNEL_DCO1, programNumber - 1, 0);
      sendShortMessage(ShortMessage.PROGRAM_CHANGE, CHANNEL_DCO2, programNumber - 1, 0);
    }

    /**
     * @param midiReceiver nullable
     */
    public void changeDevice(Receiver midiReceiver) {
      this.midiReceiver = midiReceiver;
      sendShortMessage(ShortMessage.CONTROL_CHANGE, CHANNEL_DCO1, 10, 0); // left
      sendShortMessage(ShortMessage.CONTROL_CHANGE, CHANNEL_DCO2, 10, 127); // right
    }

    private void sendShortMessage(int command, int channel, int data1, int data2) {
      if (midiReceiver == null) return;
      try {
        ShortMessage message = new ShortMessage(command, channel, data1, data2);
        midiReceiver.send(message, -1);
      } catch (InvalidMidiDataException ignore) {
      }
    }

    @Override
    public void run() {
      while (!tgData.isEmpty()) {
        int data = tgData.remove();
        int tg = data >> 8;
        if (tgNoteOff[tg] != null) { // release the key before playing next
          sendShortMessage(ShortMessage.NOTE_OFF, tgMidiChannel[tg], tgMidiNoteNumber[tg], MIDI_DEFAULT_VELOCITY);
          tgNoteOff[tg] = null;
        }
        int dco = tg >> 2;
        if ((data & 0x80) == 0 || groupWaveform[dco] == 0) {
          continue;
        }
        byte note = (byte) (data & 0x7F);
        tgMidiChannel[tg] = tg < 4 ? CHANNEL_DCO1 : CHANNEL_DCO2;
        tgMidiNoteNumber[tg] = note - C4_MSM + C4_MIDI + groupOctave[dco] * 12;
        // percussion in song 0x41: 5F606264, in song 0x51: 5F6062, 5E6062, 5F6062
        if (note >= 0x58) {
          int percussion;
          switch (note) {
            case 0x5E: percussion = 49; break;
            case 0x5F: percussion = 57; break;
            case 0x60: percussion = 42; break;
            case 0x62: percussion = 55; break;
            case 0x64: percussion = 49; break;
            default:
              percussion = 42; // default closed hi-hat
          }
          tgMidiChannel[tg] = CHANNEL_PERCUSSION;
          tgMidiNoteNumber[tg] = percussion;
        }
        sendShortMessage(ShortMessage.NOTE_ON, tgMidiChannel[tg], tgMidiNoteNumber[tg], MIDI_DEFAULT_VELOCITY);
        tgNoteOff[tg] = Instant.now().plusMillis(groupAttack[dco] + groupDecay[dco]);
      }

      Instant now = Instant.now();
      for (int tg = 0; tg < 8; tg++) {
        if (tgNoteOff[tg] != null && now.isAfter(tgNoteOff[tg])) {
          sendShortMessage(ShortMessage.NOTE_OFF, tgMidiChannel[tg], tgMidiNoteNumber[tg], MIDI_DEFAULT_VELOCITY);
          tgNoteOff[tg] = null;
        }
      }
    }

    void set(int register, byte data) {
      if ((register | 7) == 7) {
        assert data == 0 || (data & 0x80) == 0x80;
        tgData.add(register << 8 | data & 0xFF);
        return;
      }
      switch (register | 1) {
        case 0x09: // attack time data
          groupAttack[register & 1] = ATTACK[data & 0x07];
          break;
        case 0x0B: // decay time data
          groupDecay[register & 1] = DECAY[data & 0x0F];
          break;
        case 0x0D: // control data
          assert data == 0 || (data & 0xF0) == 0x20;
          data &= 0x0F;
          int dco = register & 1; // DCO1 DCO2 or TG Group 1 TG Group 2
          groupOctave[dco] = 0;
          groupWaveform[dco] = data; // FIXME: 2022-10-09 data == 0 make no sound
          for (int i = 0; (i < 4) && ((groupWaveform[dco] & 1) == 0); i++) {
            groupOctave[dco]++;
            groupWaveform[dco] >>= 1;
          }
          break;
        default:
          throw new IllegalStateException();
      }
    }

    @Override
    public void close() {
      for (int tg = 0; tg < 8; tg++) {
        if (tgNoteOff[tg] != null) {
          sendShortMessage(ShortMessage.NOTE_OFF, tgMidiChannel[tg], tgMidiNoteNumber[tg], MIDI_DEFAULT_VELOCITY);
          tgNoteOff[tg] = null;
        }
      }
      if (midiReceiver != null) {
        midiReceiver.close();
      }
    }
  }

  public static class Ym2149 implements Runnable {
    public static final int[] DA_VOLTAGE = {
        1, 3, 5, 7, // made this part linear, by specs it must be 5, 6, 7, 8
        9, 11, 13, 16, 19, 22, 26, 31, 37, 44, 53, 63,
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
          volume += (registers[14] >> 3 & 0x1F) - 0x1F; // port A volume control, not in spec
          volume = volume < 0 ? 0 : DA_VOLTAGE[volume] * YM_VOLUME / 1000;
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

    Receiver midiReceiver;
    try {
      MidiDevice midiDevice = MidiSystem.getSynthesizer();
      midiDevice.open();
      midiReceiver = midiDevice.getReceiver();
    } catch (MidiUnavailableException e) {
      throw new IllegalStateException(e);
    }
    msm = new Msm5232(midiReceiver);
    ym = new Ym2149(2_000_000, wav);

    super.open();
  }

  @Override
  public void close() {
    if (isOpen()) {
      msm.close();
      line.stop();
      line.close();
    }
    super.close();
  }

  public static final int MAX_MIDI_DEVICES = 3;
  public void extMidi(int number) {
    Set<String> noReceiver = new HashSet<>(Arrays.asList("RealTimeSequencer", "MidiInDevice"));
    List<MidiDevice> midiDevices = Arrays.stream(MidiSystem.getMidiDeviceInfo()).map(info -> {
      try {
        return MidiSystem.getMidiDevice(info);
      } catch (MidiUnavailableException e) {
        return null;
      }
    })
        .filter(Objects::nonNull)
        .filter(device -> !noReceiver.contains(device.getClass().getSimpleName()))
        .collect(Collectors.toList());
    Receiver midiReceiver = null; // valid value
    if (number > 0 && midiDevices.size() > MAX_MIDI_DEVICES) {
      number += midiDevices.size() - MAX_MIDI_DEVICES; // first and last two
    }
    if (number < midiDevices.size()) {
      MidiDevice midiDevice = midiDevices.get(number);
      try {
        midiDevice.open();
        midiReceiver = midiDevice.getReceiver();
      } catch (MidiUnavailableException e) {
        // could not use unknown device, expected exception
      }
    }
    msm.changeDevice(midiReceiver);
  }

  public static final int IDLE_ADDRESS = 0x0160;
  @Override
  public void run() {
    if (!isOpen()) return;
    dacInput = new ByteArrayOutputStream();
    runToAddress(IDLE_ADDRESS);
    Integer command = stdin.poll();
    if (command != null) {
      if ((command | 0xFF) == 0xFF) {
        stderr.add(String.format("ERR1AF,%02X", command));
        writeByte(SOUND_LATCH, command);
        rst(0x66);
      }
      if ((command | 0xFF) == 0x01FF) {
        msm.changeInstrument(command & 0xFF);
      }
      if ((command | 0xFF) == 0x02FF) {
        extMidi(command & 0xFF);
      }
    }
    runToAddress(IDLE_ADDRESS);
    rst(0x38);
    runToAddress(IDLE_ADDRESS);
    rst(0x38);

    msm.run();
    if (dacInput.size() > 0) {
      // dac code have a loop of 252,252,252,251 clock cycles
      // producing 8_000_000 / 2 / 251.75 = 15888.8 Hz rate, why?
      // MAME emulator gives around 15427 Hz which neither make sense to me
      double x3 = SAMPLE_RATE / (8_000_000 / 2 / 251.75);
      byte[] b1 = dacInput.toByteArray();
      byte[] b2 = new byte[(int) (b1.length * x3)];
      for (int i = 0; i < b2.length; i++) {
        b2[i] = b1[(int) (i / x3)];
      }
      dacOutput.add(new ByteArrayInputStream(b2));
    }
    final int[] wav = new int[line.available() / AUDIO_FORMAT.getFrameSize()];
    this.wav.set(wav);
    ym.run();
    for (ByteArrayInputStream output : dacOutput) {
      for (int i = 0; i < wav.length; i++) {
        int b = output.read();
        if (b < 0) {
          break;
        }
        wav[i] += (b - 0x80) << 8;
      }
    }
    if (dacOutput.stream().anyMatch(s -> s.available() <= 0)) {
      dacOutput = dacOutput.stream().filter(s -> s.available() > 0).collect(Collectors.toSet());
    }

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
    if (address == 0xDE00) {
      dacInput.write(data);
      super.writeByte(address, data);
      return;
    }
    if (address == YM_DATA) {
      int ymAddr = super.readByte(YM_ADDR);
      ym.set(ymAddr, (byte) data);
      super.writeByte(address, data);
      stderr.add(String.format("ERR%02X,%02X", 0x1B0 + ymAddr, data));
      return;
    }
    if ((address >= MSM_REGISTER && address < MSM_REGISTER + 0x0E)) {
      // audiocpu.mb@2b1=0
      msm.set(address - MSM_REGISTER, (byte) data);
      super.writeByte(address, data);
      stderr.add(String.format("ERR%02X,%02X", 0x1A0 + address - MSM_REGISTER, data));
      return;
    }
    super.writeByte(address, data);
  }
}
