package cnnr.groundwork.block;

import cnnr.groundwork.Groundwork;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    private ModBlocks() {}

    public static WispBlock WISP_BLOCK;

    public static void register() {
        ResourceKey<Block> wispKey = ResourceKey.create(Registries.BLOCK, Groundwork.id("wisp"));
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
            .setId(wispKey)
            .noCollision()
            .noOcclusion()
            .instabreak()
            .noLootTable()
            .pushReaction(PushReaction.IGNORE);
        WISP_BLOCK = new WispBlock(props);
        Registry.register(BuiltInRegistries.BLOCK, wispKey, WISP_BLOCK);
        // No BlockItem: wisps are placed via the Surveyor's Lens, not from the inventory.
    }
}
