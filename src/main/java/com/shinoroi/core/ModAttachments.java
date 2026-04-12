package com.shinoroi.core;

import com.shinoroi.ShinoRoi;
import com.shinoroi.data.PlayerData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ShinoRoi.MODID);

    /** All player state: energy, fight mode, rank, cooldowns, unlocked techniques. */
    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA =
        ATTACHMENT_TYPES.register("player_data", () ->
            AttachmentType.builder(PlayerData::new)
                .serialize(PlayerData.MAP_CODEC)
                .build()
        );
}
