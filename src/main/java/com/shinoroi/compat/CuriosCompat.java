package com.shinoroi.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Compile-time-optional Curios API bridge.
 *
 * No Curios classes are referenced directly — only {@link IItemHandlerModifiable}
 * (from NeoForge, always on the classpath) is used after the initial reflection
 * entry point.  This means the mod builds and runs whether or not Curios is
 * installed; when it is installed the slots are automatically saved/restored.
 *
 * Call these methods only inside a {@code ModList.get().isLoaded("curios")} guard
 * (see {@link com.shinoroi.network.ToggleFightModePacket}).
 *
 * Reflection path:
 *   CuriosApi.getCuriosInventory(player)          → Optional<ICuriosItemHandler>
 *   ICuriosItemHandler.getEquippedCurios()         → IItemHandlerModifiable   (NeoForge)
 *   IItemHandlerModifiable.getSlots / get/setStack → standard NeoForge API
 */
public final class CuriosCompat {

    private CuriosCompat() {}

    /**
     * Saves every equipped Curios item into {@code snapshot} (copies), then
     * clears those slots.
     */
    public static void saveAndClear(ServerPlayer player, List<ItemStack> snapshot) {
        withEquipped(player, equipped -> {
            for (int i = 0; i < equipped.getSlots(); i++) {
                snapshot.add(equipped.getStackInSlot(i).copy());
                equipped.setStackInSlot(i, ItemStack.EMPTY);
            }
        });
    }

    /**
     * Restores Curios items from {@code snapshot} beginning at {@code startIdx}.
     */
    public static void restore(ServerPlayer player, List<ItemStack> snapshot, int startIdx) {
        withEquipped(player, equipped -> {
            int idx = startIdx;
            for (int i = 0; i < equipped.getSlots() && idx < snapshot.size(); i++, idx++) {
                equipped.setStackInSlot(i, snapshot.get(idx).copy());
            }
        });
    }

    // ── Internal reflection helper ────────────────────────────────────────────

    @FunctionalInterface
    private interface EquippedConsumer {
        void accept(IItemHandlerModifiable equipped);
    }

    /**
     * Obtains the player's equipped-curios {@link IItemHandlerModifiable} via
     * reflection and passes it to {@code action}.  Silently no-ops on any error
     * (Curios not loaded, API version mismatch, etc.).
     */
    @SuppressWarnings("unchecked")
    private static void withEquipped(ServerPlayer player, EquippedConsumer action) {
        try {
            Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInventory = apiClass.getMethod(
                "getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);

            Optional<?> opt = (Optional<?>) getCuriosInventory.invoke(null, player);
            opt.ifPresent(handler -> {
                try {
                    Method getEquipped = handler.getClass().getMethod("getEquippedCurios");
                    // getEquippedCurios() returns IItemHandlerModifiable — a NeoForge type
                    // we already have on the classpath, so the cast is safe.
                    IItemHandlerModifiable equipped =
                        (IItemHandlerModifiable) getEquipped.invoke(handler);
                    action.accept(equipped);
                } catch (Exception ignored) { /* API version mismatch */ }
            });
        } catch (Exception ignored) { /* Curios not on classpath */ }
    }
}
