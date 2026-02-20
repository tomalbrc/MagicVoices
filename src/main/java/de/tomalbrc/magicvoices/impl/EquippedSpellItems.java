package de.tomalbrc.magicvoices.impl;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.tomalbrc.magicvoices.MagicVoicesPlugin;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.HashMap;
import java.util.Map;

public class EquippedSpellItems implements Component<EntityStore> {
    public Map<String, ItemConfig.Spell> byItemId = new HashMap<>();

    @NullableDecl
    @Override
    public Component<EntityStore> clone() {
        var e = new EquippedSpellItems();;
        e.byItemId = new HashMap<>(this.byItemId);
        return e;
    }

    public static ComponentType<EntityStore, EquippedSpellItems> getComponentType() {
        return MagicVoicesPlugin.get().equippedSpellItemsComponentType();
    }
}