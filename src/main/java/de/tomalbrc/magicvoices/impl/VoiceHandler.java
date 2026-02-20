package de.tomalbrc.magicvoices.impl;

import com.google.gson.JsonArray;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.voice.VoiceData;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.concurrent.ThreadUtil;
import de.tomalbrc.magicvoices.MagicVoicesPlugin;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class VoiceHandler {
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newSingleThreadScheduledExecutor(ThreadUtil.daemon("MagicVoices"));

    private static final Map<UUID, VoiceProcessor> map = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 50;

    static Model voskModel;

    public static void reset() {
        map.clear();
    }

    public static void init() {
        try {
            voskModel = new Model(MagicVoicesPlugin.get().getDataDirectory().resolve(MagicVoicesPlugin.get().itemConfig().voskModelPath).toAbsolutePath().toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Vosk model", e);
        }

        SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();

                for (var entry : map.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    long lastTime = entry.getValue().lastTime;

                    if (now - lastTime > TIMEOUT_MS) {
                        VoiceProcessor processor = map.get(playerUuid);

                        if (processor != null) {
                            String finalWord = processor.flush();
                            if (finalWord != null && !finalWord.isBlank()) {
                                dispatchEvent(processor.getPlayerRef(), finalWord);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                HytaleLogger.forEnclosingClass().at(Level.SEVERE).log("Failed to tick voice processor", e);
            }
        }, 0, 60, TimeUnit.MILLISECONDS);
    }

    public static void analyze(PlayerRef playerRef, VoiceData data) {
        SCHEDULED_EXECUTOR.execute(() -> {
            try {
                var processor = map.computeIfAbsent(playerRef.getUuid(), _ -> {
                    try {
                        JsonArray array = new JsonArray();
                        MagicVoicesPlugin.get().itemConfig().byWord.keySet().forEach(array::add);
                        var recognizer = new Recognizer(voskModel, 16000.0f, array.toString());
                        return new VoiceProcessor(recognizer, playerRef);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                processor.lastTime = System.currentTimeMillis();

                String spell = processor.process(data.opusData);
                if (spell != null && !spell.isBlank()) {
                    playerRef.sendMessage(Message.raw(spell));
                    dispatchEvent(playerRef, spell);
                }

            } catch (Exception e) {
                HytaleLogger.forEnclosingClass().at(Level.SEVERE).log("Voice analysis error", e);
            }
        });
    }

    private static void dispatchEvent(PlayerRef playerRef, String spell) {
        var dispatcher = HytaleServer.get().getEventBus().dispatchForAsync(SpokenWordEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new SpokenWordEvent(playerRef, spell));
        }
    }

    public static void remove(UUID uuid) {
        map.remove(uuid);
    }
}
