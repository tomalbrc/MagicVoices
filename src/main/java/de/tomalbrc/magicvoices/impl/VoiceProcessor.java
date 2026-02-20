package de.tomalbrc.magicvoices.impl;

import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.github.jaredmdobson.OpusDecoder;
import io.github.jaredmdobson.OpusException;
import org.vosk.Recognizer;

public final class VoiceProcessor {
    private final OpusDecoder decoder;
    private final Recognizer recognizer;
    private final PlayerRef playerRef;
    private final int decimationFactor;
    private final int frameLength;

    private final short[] decodeBuffer;
    private final short[] ringBuffer;
    private final short[] frameBuffer;

    private int ringWrite = 0;

    public long lastTime = 0;

    public VoiceProcessor(Recognizer recognizer, PlayerRef playerRef) throws OpusException {
        this.recognizer = recognizer;
        recognizer.setWords(true);
        recognizer.setPartialWords(false);

        this.playerRef = playerRef;

        int targetSampleRate = 16000;
        this.frameLength = 320;

        final int decoderSampleRate = 48000;
        this.decimationFactor = decoderSampleRate / targetSampleRate;

        this.decodeBuffer = new short[5760];
        int ringCapacity = frameLength * 10;
        this.ringBuffer = new short[ringCapacity];
        this.frameBuffer = new short[frameLength];

        this.decoder = new OpusDecoder(decoderSampleRate, 1);
    }

    public String process(byte[] opusData) throws OpusException {
        int decodedSamples = decoder.decode(
                opusData, 0, opusData.length,
                decodeBuffer, 0, decodeBuffer.length, false
        );

        if (decodedSamples <= 0)
            return null;

        for (int i = 0; i + (decimationFactor - 1) < decodedSamples; i += decimationFactor) {
            if (ringWrite >= ringBuffer.length) {
                int keep = ringBuffer.length / 2;
                System.arraycopy(ringBuffer, ringWrite - keep, ringBuffer, 0, keep);
                ringWrite = keep;
            }
            ringBuffer[ringWrite++] = decodeBuffer[i];
        }

        while (ringWrite >= frameLength) {
            System.arraycopy(ringBuffer, 0, frameBuffer, 0, frameLength);

            byte[] frameBytes = shortsToLittleEndianBytes(frameBuffer, frameLength);
            boolean isFinal = recognizer.acceptWaveForm(frameBytes, frameBytes.length);

            int remaining = ringWrite - frameLength;
            if (remaining > 0) {
                System.arraycopy(ringBuffer, frameLength, ringBuffer, 0, remaining);
            }
            ringWrite = remaining;

            if (!isFinal) {
                String json = recognizer.getResult();
                if (json != null && !json.isEmpty()) {
                    var j = JsonParser.parseString(json);
                    var word = j.getAsJsonObject().get("text");
                    return word.getAsString();
                }
            }
        }

        return null;
    }

    private static byte[] shortsToLittleEndianBytes(short[] samples, int len) {
        byte[] out = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            short s = samples[i];
            out[i * 2] = (byte) (s & 0xff);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }

    public String flush() {
        String json = recognizer.getFinalResult();
        if (json != null) {
            var j = JsonParser.parseString(json);
            if (j.getAsJsonObject().has("text")) {
                String text = j.getAsJsonObject().get("text").getAsString();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }

        return null;
    }

    public PlayerRef getPlayerRef() {
        return playerRef;
    }
}