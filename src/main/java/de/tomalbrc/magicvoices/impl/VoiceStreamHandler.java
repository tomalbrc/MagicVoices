
package de.tomalbrc.magicvoices.impl;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.NetworkChannel;
import com.hypixel.hytale.protocol.ToServerPacket;
import com.hypixel.hytale.protocol.io.ChannelConnection;
import com.hypixel.hytale.protocol.io.ConnectionHandler;
import com.hypixel.hytale.protocol.packets.stream.StreamType;
import com.hypixel.hytale.protocol.packets.voice.VoiceData;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.modules.voice.VoiceModule;
import com.hypixel.hytale.server.core.modules.voice.VoicePlayerState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import de.tomalbrc.magicvoices.impl.system.SpellItemChangeSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class VoiceStreamHandler implements ConnectionHandler {
    private final PacketHandler packetHandler;
    private final ChannelConnection channel;
    private final VoiceModule voiceModule;
    private final HytaleLogger logger;
    private volatile PlayerRef cachedPlayerRef;
    private volatile boolean loggedFirstPacket = false;
    private volatile boolean loggedFirstVoiceData = false;

    public VoiceStreamHandler(@Nonnull PacketHandler packetHandler, @Nonnull ChannelConnection channel) {
        this.packetHandler = packetHandler;
        this.channel = channel;
        this.voiceModule = VoiceModule.get();
        this.logger = this.voiceModule.getLogger();
    }

    @Override
    public void registered(@Nullable ConnectionHandler oldHandler) {
        this.packetHandler.setChannel(StreamType.Voice, this.channel);

        PacketHandler packetHandler = this.packetHandler;
        if (packetHandler instanceof GamePacketHandler gameHandler) {
            this.cachedPlayerRef = gameHandler.getPlayerRef();
        }

        this.logger.at(Level.FINE).log("[VoiceStream] Voice stream registered for %s (channel active=%s, playerRef=%s)", this.packetHandler.getIdentifier(), this.channel.isActive(), this.cachedPlayerRef != null ? this.cachedPlayerRef.getUsername() : "null");
    }

    @Override
    public void handle(@Nonnull ToServerPacket packet) {
        PlayerRef playerRef;
        if (!this.loggedFirstPacket) {
            this.loggedFirstPacket = true;
            this.logger.at(Level.FINE).log("[VoiceStream] First packet received from %s: %s", (Object)this.packetHandler.getIdentifier(), (Object)packet.getClass().getSimpleName());
        }
        if ((playerRef = this.getPlayerRef()) == null) {
            this.logger.at(Level.WARNING).log("[VoiceStream] No player ref for voice packet from %s", this.packetHandler.getIdentifier());
            return;
        }
        if (packet instanceof VoiceData voiceData) {
            this.handleVoiceData(playerRef, voiceData);
        } else {
            this.logger.at(Level.WARNING).log("[VoiceStream] Unexpected packet type %s from %s", (Object)packet.getClass().getSimpleName(), (Object)this.packetHandler.getIdentifier());
        }
    }

    private void handleVoiceData(@Nonnull PlayerRef playerRef, @Nonnull VoiceData data) {
        if (!this.voiceModule.isVoiceEnabled()) {
            return;
        }

        if (!this.loggedFirstVoiceData) {
            this.loggedFirstVoiceData = true;
            this.logger.at(Level.FINE).log("[VoiceStream] Routing first VoiceData from %s: seq=%d, dataSize=%d", playerRef.getUsername(), data.sequenceNumber, data.opusData != null ? data.opusData.length : 0);
        }

        if (this.voiceModule.isShutdown()) {
            return;
        }

        VoicePlayerState state = this.voiceModule.getPlayerState(playerRef.getUuid());
        if (state == null || state.isRoutingDisabled() || state.isSilenced() || this.voiceModule.isPlayerMuted(playerRef.getUuid())) {
            return;
        }

        if (!state.checkRateLimit(this.voiceModule.getMaxPacketsPerSecond(), this.voiceModule.getBurstCapacity())) {
            if (state.shouldLogRateLimit()) {
                this.logger.at(Level.WARNING).log("[VoiceStream] RATE_LIMITED: player=%s, tokens=%.2f, maxPps=%d, burstCapacity=%d", playerRef.getUsername(), state.getTokenBucket(), this.voiceModule.getMaxPacketsPerSecond(), this.voiceModule.getBurstCapacity());
            }
            return;
        }

        if (data.opusData == null || data.opusData.length == 0) {
            this.logger.at(Level.FINE).log("[VoiceStream] REJECTED_EMPTY: player=%s, seq=%d", (Object)playerRef.getUsername(), data.sequenceNumber);
            return;
        }

        if (data.opusData.length > this.voiceModule.getMaxPacketSize()) {
            this.logger.at(Level.WARNING).log("[VoiceStream] REJECTED_OVERSIZE: player=%s, size=%d, maxSize=%d", playerRef.getUsername(), data.opusData.length, this.voiceModule.getMaxPacketSize());
            return;
        }

        this.voiceModule.getVoiceExecutor(playerRef.getUuid()).execute(() -> {
            try {
                this.voiceModule.getVoiceRouter().routeVoiceFromCache(playerRef, data);
                state.resetConsecutiveErrors();

                if (SpellItemChangeSystem.PLAYERS.contains(playerRef.getUuid()))
                    VoiceHandler.analyze(playerRef, data);
            }
            catch (Exception e) {
                int failures = state.incrementConsecutiveErrors();
                if (failures >= 10) {
                    this.logger.at(Level.WARNING).log("[VoiceStream] Disabled voice routing for %s after %d consecutive errors", (Object)playerRef.getUuid(), failures);
                    state.setRoutingDisabled(true);
                }
                this.logger.at(Level.SEVERE).withCause(e).log("[VoiceStream] Exception in routeVoiceFromCache for %s (failure %d/%d)", playerRef.getUuid(), failures, 10);
            }
        });
    }

    private PlayerRef getPlayerRef() {
        if (this.cachedPlayerRef != null) {
            return this.cachedPlayerRef;
        }

        PacketHandler packetHandler = this.packetHandler;
        if (packetHandler instanceof GamePacketHandler gameHandler) {
            this.cachedPlayerRef = gameHandler.getPlayerRef();
        }
        return this.cachedPlayerRef;
    }

    @Override
    public void closed(@Nullable NetworkChannel networkChannel) {
        this.packetHandler.compareAndSetChannel(StreamType.Voice, this.channel, null);
        this.logger.at(Level.FINE).log("[VoiceStream] Voice stream closed for %s", this.packetHandler.getIdentifier());
    }

    @Override
    public void unregistered(@Nullable ConnectionHandler newHandler) {
        this.packetHandler.compareAndSetChannel(StreamType.Voice, this.channel, null);
    }

    @Override
    public void logCloseMessage() {
    }
}
