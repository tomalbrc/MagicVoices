package de.tomalbrc.magicvoices.impl;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import de.tomalbrc.magicvoices.MagicVoicesPlugin;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ReloadCommand extends AbstractAsyncCommand {
    public ReloadCommand() {
        super("magicvoices", "magicvoices.command");
        this.setPermissionGroup(GameMode.Creative);
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        MagicVoicesPlugin.get().reload();
        VoiceHandler.reset();
        return CompletableFuture.completedFuture(null);
    }
}
