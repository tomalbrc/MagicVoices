package de.tomalbrc.magicvoices;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionChainData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.stream.StreamType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.io.stream.StreamManager;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import de.tomalbrc.magicvoices.impl.*;
import de.tomalbrc.magicvoices.impl.system.SpellItemChangeSystem;
import de.tomalbrc.magicvoices.impl.system.SpellItemHotbarEventSystem;

import javax.annotation.Nonnull;

public class MagicVoicesPlugin extends JavaPlugin {
    static MagicVoicesPlugin instance;

    ComponentType<EntityStore, EquippedSpellItems> equippedSpellItemsComponentType;
    Config<ItemConfig> itemConfig = withConfig("ItemConfig", ItemConfig.CODEC);

    public MagicVoicesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static MagicVoicesPlugin get() {
        return instance;
    }

    public ComponentType<EntityStore, EquippedSpellItems> equippedSpellItemsComponentType() {
        return equippedSpellItemsComponentType;
    }

    public ItemConfig itemConfig() {
        return itemConfig.get();
    }

    @Override
    protected void setup() {
        StreamManager.getInstance().unregisterHandler(StreamType.Voice); // idk if this is needed tbh
        StreamManager.getInstance().registerHandler(StreamType.Voice, VoiceStreamHandler::new);

        this.getCommandRegistry().registerCommand(new ReloadCommand());

        itemConfig.load().join();
        itemConfig.save();

        VoiceHandler.init();

        equippedSpellItemsComponentType = this.getEntityStoreRegistry().registerComponent(EquippedSpellItems.class, () -> null);
        this.getEntityStoreRegistry().registerSystem(new SpellItemHotbarEventSystem());
        this.getEntityStoreRegistry().registerSystem(new SpellItemChangeSystem());

        this.getEventRegistry().registerGlobal(
                PlayerDisconnectEvent.class,
                playerDisconnectEvent -> VoiceHandler.remove(playerDisconnectEvent.getPlayerRef().getUuid())
        );

        this.getEventRegistry().registerGlobal(SpokenWordEvent.class, event -> {
            var wid = event.getPlayerRef().getWorldUuid();
            if (wid != null) {
                var world = Universe.get().getWorld(wid);
                assert world != null;

                var ref = world.getEntityRef(event.getPlayerRef().getUuid());

                if (ref != null) {
                    world.execute(() -> {
                        if (!ref.isValid())
                            return;

                        handleSpokenWordEvent(event, ref);
                    });
                }
            }
        });
    }

    private static void handleSpokenWordEvent(SpokenWordEvent event, Ref<EntityStore> ref) {
        var store = ref.getStore();
        var hotbar = ref.getStore().getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        var equipped = ref.getStore().getComponent(ref, EquippedSpellItems.getComponentType());

        if (hotbar != null && equipped != null) {
            var itemStack = hotbar.getActiveItem();
            if (itemStack != null) {
                var id = itemStack.getItemId();
                var spell = MagicVoicesPlugin.get().itemConfig().byWord.get(event.getWord());
                if (spell != null && spell.itemId.equals(id)) {

                    ComponentType<EntityStore, InteractionManager> managerType = InteractionModule.get().getInteractionManagerComponent();
                    RootInteraction root = RootInteraction.getAssetMap().getAsset(spell.interactionId);
                    InteractionManager manager = store.getComponent(ref, managerType);
                    if (manager != null && root != null) {
                        InteractionContext context = InteractionContext.forInteraction(
                                manager,
                                ref,
                                InteractionType.Secondary,
                                -1,
                                store
                        );

                        var chain = new InteractionChain(InteractionType.Secondary, context, new InteractionChainData(), root, null, false);
                        manager.queueExecuteChain(chain);
                    }
                }
            }
        }
    }

    public void reload() {
        this.itemConfig.load();
    }
}