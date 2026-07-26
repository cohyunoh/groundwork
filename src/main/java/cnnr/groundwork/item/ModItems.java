package cnnr.groundwork.item;

import cnnr.groundwork.Groundwork;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public final class ModItems {
    private ModItems() {}

    public static Item SURVEYORS_LENS;

    public static void register() {
        ResourceKey<Item> lensKey = ResourceKey.create(Registries.ITEM, Groundwork.id("surveyors_lens"));
        SURVEYORS_LENS = new SurveyorsLensItem(new Item.Properties().setId(lensKey).stacksTo(1));
        Registry.register(BuiltInRegistries.ITEM, lensKey, SURVEYORS_LENS);

        // CreativeModeTabs.TOOLS_AND_UTILITIES is private in 26.2; reconstruct the same key vanilla builds it
        // from (confirmed via CreativeModeTabs.createKey's bytecode: Registries.CREATIVE_MODE_TAB + this path).
        ResourceKey<CreativeModeTab> toolsAndUtilities =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));
        CreativeModeTabEvents.modifyOutputEvent(toolsAndUtilities)
            .register(output -> output.accept(SURVEYORS_LENS));
    }
}
