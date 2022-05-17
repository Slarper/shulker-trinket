package slarper.shulkertrinket.keybind;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

public class ShulkerNetworking implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(
                ClientNetworking.OPEN_SHULKER_BOX,
                (server, player, handler, buf, responseSender)-> server.execute(() -> openShulkerBox(player)
                )
        );
    }

    public static void openShulkerBox(ServerPlayerEntity player){
        if(TrinketsApi.getTrinketComponent(player).isPresent()) {
            ItemStack stack = TrinketsApi.getTrinketComponent(player).get().getInventory().get("chest").get("back").getStack(0);
            if (stack.getItem() instanceof BlockItem){
                if(((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
                    player.openHandledScreen(new ShulkerInventory(stack));
                }
            }
        }
    }
}
