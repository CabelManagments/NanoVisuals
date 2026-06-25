package ru.nanovisuals.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous client-side sound playback.
 *
 * Files are decoded ONCE at init into raw PCM byte[] + AudioFormat. Each
 * play() acquires a channel permit and writes the pre-decoded buffer to a
 * fresh SourceDataLine on a worker thread, so audio never blocks the render
 * or tick threads even during PvP bursts.
 *
 * Protection layers (in order):
 *   1. Per-sound rate limit (drops floods to ~20/s of the same clip).
 *   2. Channel semaphore (caps concurrent live lines; excess is dropped, not
 *      queued — better to skip a hit sound than to lag the audio mixer).
 *   3. Bounded executor with a daemon thread factory so JVM shutdown isn't
 *      blocked by lingering playback.
 */
public final class SoundManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("NanoVisuals/Sound");
    private static final SoundManager INSTANCE = new SoundManager();

    public static SoundManager getInstance() {
        return INSTANCE;
    }

    public enum Sound {
        HIT("hit.wav"),
        KILL("kill.wav"),
        ENABLE("enable.wav"),
        DISABLE("disable.wav");

        final String resource;

        Sound(String resource) {
            this.resource = resource;
        }
    }

    private static final String BASE = "/assets/nanovisuals/sounds/";
    private static final int CHANNELS = 8;
    private static final long PER_SOUND_MIN_INTERVAL_MS = 45L;

    private final Map<Sound, byte[]> pcm = new EnumMap<>(Sound.class);
    private final Map<Sound, AudioFormat> formats = new EnumMap<>(Sound.class);
    private final Map<Sound, AtomicLong> lastPlay = new EnumMap<>(Sound.class);

    private final Semaphore channels = new Semaphore(CHANNELS);
    private final ExecutorService executor = Executors.newFixedThreadPool(CHANNELS, r -> {
        Thread t = new Thread(r, "NanoVisuals-Sound");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    /** Volume scalar in [0..1]. Maps to MASTER_GAIN dB at play time. */
    private volatile float masterVolume = 0.6f;
    private volatile boolean ready;

    private SoundManager() {
        for (Sound s : Sound.values()) lastPlay.put(s, new AtomicLong(0L));
    }

    public void init() {
        for (Sound s : Sound.values()) {
            try {
                load(s);
            } catch (Throwable t) {
                LOGGER.error("Failed to load sound {}: {}", s, t.toString(), t);
            }
        }
        ready = !pcm.isEmpty();
        LOGGER.info("SoundManager ready: {} clips loaded", pcm.size());
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float v) {
        this.masterVolume = Math.max(0f, Math.min(1f, v));
    }

    public void play(Sound sound) {
        if (!ready || sound == null) return;
        if (masterVolume <= 0.001f) return;

        long now = System.currentTimeMillis();
        AtomicLong last = lastPlay.get(sound);
        long prev = last.get();
        if (now - prev < PER_SOUND_MIN_INTERVAL_MS) return;
        if (!last.compareAndSet(prev, now)) return;

        if (!channels.tryAcquire()) return;

        try {
            executor.submit(() -> {
                try {
                    playInternal(sound);
                } catch (Throwable t) {
                    LOGGER.debug("Sound playback error for {}: {}", sound, t.toString());
                } finally {
                    channels.release();
                }
            });
        } catch (Throwable t) {
            channels.release();
        }
    }

    private void load(Sound sound) throws IOException, javax.sound.sampled.UnsupportedAudioFileException {
        String path = BASE + sound.resource;
        try (InputStream raw = SoundManager.class.getResourceAsStream(path)) {
            if (raw == null) throw new IOException("Resource not found: " + path);
            try (BufferedInputStream buf = new BufferedInputStream(raw);
                 AudioInputStream ais = AudioSystem.getAudioInputStream(buf)) {
                AudioFormat fmt = ais.getFormat();
                byte[] data = ais.readAllBytes();
                pcm.put(sound, data);
                formats.put(sound, fmt);
                LOGGER.info("Loaded sound {} ({} bytes, {} Hz, {} ch)",
                        sound.resource, data.length,
                        (int) fmt.getSampleRate(), fmt.getChannels());
            }
        }
    }

    private void playInternal(Sound sound) throws Exception {
        AudioFormat fmt = formats.get(sound);
        byte[] data = pcm.get(sound);
        if (fmt == null || data == null) return;

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        try {
            line.open(fmt);
            applyVolume(line, masterVolume);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } finally {
            try { line.close(); } catch (Throwable ignored) {}
        }
    }

    private static void applyVolume(SourceDataLine line, float volume) {
        if (!line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        float v = Math.max(0.0001f, Math.min(1f, volume));
        float db = (float) (20.0 * Math.log10(v));
        db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
        gain.setValue(db);
    }
}
