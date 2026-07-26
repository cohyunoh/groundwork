package cnnr.groundwork.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The Lens's real logic lives entirely in {@code Groundwork}'s server-side
 * {@code UseBlockCallback}/{@code UseItemCallback} handlers. This override exists only so the
 * CLIENT's local interaction prediction sees the block-click as "consumed": the default
 * {@link Item#useOn} returns PASS, and since ordinary blocks don't consume the Lens's click either,
 * the client would otherwise conclude the click did nothing and automatically also send a
 * follow-up "use item" packet (vanilla's PASS-fallback behavior) — firing our air-use toggle a
 * second time on every block click. Returning a consuming result here suppresses that duplicate.
 */
public class SurveyorsLensItem extends Item {
    public SurveyorsLensItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.SUCCESS_SERVER;
    }
}
