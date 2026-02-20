package de.tomalbrc.magicvoices.impl;

import com.hypixel.hytale.event.IAsyncEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class SpokenWordEvent implements IAsyncEvent<Void> {
    private final PlayerRef playerRef;
    private final String word;

    public SpokenWordEvent(@Nonnull PlayerRef playerRef, String word) {
        this.playerRef = playerRef;
        this.word = word;
    }

    public PlayerRef getPlayerRef() {
        return this.playerRef;
    }

    public String getWord() {
        return this.word;
    }
}
