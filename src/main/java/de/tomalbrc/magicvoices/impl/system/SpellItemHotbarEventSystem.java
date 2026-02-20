package de.tomalbrc.magicvoices.impl.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.tomalbrc.magicvoices.MagicVoicesPlugin;
import de.tomalbrc.magicvoices.impl.EquippedSpellItems;

import javax.annotation.Nullable;

public class SpellItemHotbarEventSystem extends EntityEventSystem<EntityStore, InventoryChangeEvent> {
    public SpellItemHotbarEventSystem() {
        super(InventoryChangeEvent.class);
    }

    @Override
    public void handle(int i, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer, InventoryChangeEvent ecsEvent) {
        if (ecsEvent.getInventory() instanceof InventoryComponent.Hotbar hotbar && hotbar.getActiveItem() != null) {
            var item = new EquippedSpellItems();
            var spell = MagicVoicesPlugin.get().itemConfig().byItemId.get(hotbar.getActiveItem().getItemId());
            item.byItemId.put(hotbar.getActiveItem().getItemId(), spell);
            commandBuffer.putComponent(archetypeChunk.getReferenceTo(i), EquippedSpellItems.getComponentType(), item);
        }
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), InventoryComponent.Hotbar.getComponentType());
    }
}
