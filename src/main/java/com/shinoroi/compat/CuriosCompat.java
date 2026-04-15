package com.shinoroi.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.List;

/**
 * Compatibility bridge for Curios API (optional dependency).
 *
 * This class is ONLY referenced inside an {@code if (ModList.get().isLoaded("curios"))}
 * guard in {@link com.shinoroi.network.ToggleFightModePacket}, ensuring the JVM
 * never loads it — and never throws NoClassDefFoundError — when Curios is absent.
 *
 * Curios slots are appended to the end of the {@code storedInventory} list after
 * the vanilla slots (main 36 + offhand 1 + armor 4).  The index passed to
 * {@link #restore} must be the offset at which Curios items begin in that list.
 */
public final class CuriosCompat {

    private CuriosCompat() {}

    /**
     * Saves every equipped Curios item into {@code snapshot} (copies), then
     * clears those slots.
     */
    public static void saveAndClear(ServerPlayer player, List<ItemStack> snapshot) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            for (int i = 0; i < equipped.getSlots(); i++) {
                snapshot.add(equipped.getStackInSlot(i).copy());
                equipped.setStackInSlot(i, ItemStack.EMPTY);
            }
        });
    }

    /**
     * Restores Curios items from {@code snapshot} starting at {@code startIdx},
     * in the same order they were saved by {@link #saveAndClear}.
     */
    public static void restore(ServerPlayer player, List<ItemStack> snapshot, int startIdx) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            int idx = startIdx;
            for (int i = 0; i < equipped.getSlots() && idx < snapshot.size(); i++, idx++) {
                equipped.setStackInSlot(i, snapshot.get(idx).copy());
            }
        });
    }
}
