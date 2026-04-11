package com.shinoroi.mixin;

import com.shinoroi.core.ModAttachments;
import com.shinoroi.data.PlayerData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents players in fight mode from picking up item entities.
 *
 * We intercept ItemEntity.playerTouch() rather than relying on
 * EntityItemPickupEvent (removed in NeoForge 1.21.x). The injection
 * is cancellable so the item stays in the world untouched.
 */
@Mixin(ItemEntity.class)
public class MixinItemEntity {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void shinoroi$blockPickupInFightMode(Player player, CallbackInfo ci) {
        // playerTouch is called server-side; guard just in case
        if (player.level().isClientSide()) return;

        PlayerData data = player.getData(ModAttachments.PLAYER_DATA.get());
        if (data.isFightModeActive()) {
            ci.cancel(); // item stays on the ground
        }
    }
}
