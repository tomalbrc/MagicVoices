package de.tomalbrc.magicvoices.impl.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.tomalbrc.magicvoices.impl.EquippedSpellItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellItemChangeSystem extends RefChangeSystem<EntityStore, EquippedSpellItems> {
    public static Set<UUID> PLAYERS = ConcurrentHashMap.newKeySet();

    final Query<EntityStore> query;

    public SpellItemChangeSystem() {
        this.query = EquippedSpellItems.getComponentType();
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, EquippedSpellItems> componentType() {
        return EquippedSpellItems.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull EquippedSpellItems component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PLAYERS.add(Objects.requireNonNull(store.getComponent(ref, UUIDComponent.getComponentType())).getUuid());
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable EquippedSpellItems component, @Nonnull EquippedSpellItems t1, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull EquippedSpellItems component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PLAYERS.remove(Objects.requireNonNull(store.getComponent(ref, UUIDComponent.getComponentType())).getUuid());
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }
}
