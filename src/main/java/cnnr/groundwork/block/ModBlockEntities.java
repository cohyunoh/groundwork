package cnnr.groundwork.block;

import cnnr.groundwork.Groundwork;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static BlockEntityType<WispBlockEntity> WISP;

    public static void register() {
        WISP = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Groundwork.id("wisp"),
            FabricBlockEntityTypeBuilder.create(WispBlockEntity::new, ModBlocks.WISP_BLOCK).build());
    }
}
