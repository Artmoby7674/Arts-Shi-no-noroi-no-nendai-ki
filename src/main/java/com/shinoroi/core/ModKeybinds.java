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

    /** Z — technique slot 1 */
    public static final KeyMapping TECHNIQUE_1 = new KeyMapping(
        "key.shinoroi.technique_1",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z,
        CATEGORY
    );

    /** X — technique slot 2 */
    public static final KeyMapping TECHNIQUE_2 = new KeyMapping(
        "key.shinoroi.technique_2",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        CATEGORY
    );

    /** C — technique slot 3 */
    public static final KeyMapping TECHNIQUE_3 = new KeyMapping(
        "key.shinoroi.technique_3",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_C,
        CATEGORY
    );

    /** V — domain expansion / ultimate */
    public static final KeyMapping DOMAIN_EXPANSION = new KeyMapping(
        "key.shinoroi.domain_expansion",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        CATEGORY
    );
}
