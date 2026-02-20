package de.tomalbrc.itemsfloat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemPhysicsSystem;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Set;

public class FloatingItemSystem extends EntityTickingSystem<EntityStore> {
    private final Query<EntityStore> query;

    public FloatingItemSystem() {
        this.query = Query.and(TransformComponent.getComponentType(), Velocity.getComponentType(), BoundingBox.getComponentType(), ItemComponent.getComponentType());
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    public boolean isInFluid(@Nonnull Store<EntityStore> store, int x, int y, int z) {
        World world = store.getExternalData().getWorld();
        int fluidId = 0;
        ChunkStore chunkStore = world.getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        Ref<ChunkStore> columnRef = chunkStore.getChunkReference(chunkIndex);
        if (columnRef != null && columnRef.isValid()) {
            ChunkColumn column = chunkStore.getStore().getComponent(columnRef, ChunkColumn.getComponentType());
            if (column != null) {
                int sectionY = y >> 5;
                Ref<ChunkStore>[] sections = column.getSections();
                if (sectionY >= 0 && sectionY < sections.length) {
                    Ref<ChunkStore> sectionRef = sections[sectionY];
                    if (sectionRef != null && sectionRef.isValid()) {
                        FluidSection fluidSection = chunkStore.getStore().getComponent(sectionRef, FluidSection.getComponentType());
                        if (fluidSection != null) {
                            fluidId = fluidSection.getFluidId(x & 31, y & 31, z & 31);
                        }
                    }
                }
            }
        }

        return fluidId != 0;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d pos = transform.getPosition();
            double unfilteredY = pos.getY();
            int x = (int)Math.floor(pos.getX());
            int y = (int)Math.floor(unfilteredY);
            int z = (int)Math.floor(pos.getZ());
            var inWater = this.isInFluid(store, x, y, z);
            var blockAboveIsInWater = this.isInFluid(store, x, y + 1, z);
            if (inWater) {
                var v = 1.0 - ((pos.getY() +0.4) - (double)y);

                var velocity = chunk.getComponent(index, Velocity.getComponentType());
                if (velocity != null && (blockAboveIsInWater || v > 0)) {
                    var s = 1.1;
                    Vector3d jumpVector = new Vector3d(0, blockAboveIsInWater ? s : s*v, 0).addScaled(velocity.getVelocity(), 0.5);

                    velocity.addInstruction(jumpVector, null, ChangeVelocityType.Set);
                }
            }
        }
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.BEFORE, ItemPhysicsSystem.class));
    }
}
