package com.shinoroi.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {

    public static final String CATEGORY = "key.categories.shinoroi";

    /** R — toggle fight mode on/off */
    public static final KeyMapping TOGGLE_FIGHT_MODE = new KeyMapping(
        "key.shinoroi.toggle_fight_mode",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    /** K — open the skill tree screen */
    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(
        "key.shinoroi.open_skill_tree",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        CATEGORY
    );
}
