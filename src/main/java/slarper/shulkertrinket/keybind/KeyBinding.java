package slarper.shulkertrinket.keybind;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import static slarper.shulkertrinket.keybind.ShulkerTrinketPackets.OPEN_SHULKER_BOX;

public class KeyBinding implements ClientModInitializer {

    private static net.minecraft.client.option.KeyBinding openShulkerBox;


    @Override
    public void onInitializeClient() {
        openShulkerBox = KeyBindingHelper.registerKeyBinding(new net.minecraft.client.option.KeyBinding(
                "key.shulkertrinket.open_shulker_box", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_B, // The keycode of the key
                "category.shulkertrinket.keybinding" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openShulkerBox.wasPressed()) {
                ClientPlayNetworking.send(OPEN_SHULKER_BOX, PacketByteBufs.create());
            }
        });
    }
}
