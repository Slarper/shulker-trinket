package slarper.shulkertrinket.keybind;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class ShulkerTrinketInventory implements Inventory, NamedScreenHandlerFactory {

    protected DefaultedList<ItemStack> items;

    private final ItemStack stack;

    public ShulkerTrinketInventory(ItemStack stack) {
        this.stack = stack;
        this.items = DefaultedList.ofSize(27, ItemStack.EMPTY);
    }
    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {

            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.items, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.items.set(slot, stack == null ? ItemStack.EMPTY : stack);

        if (stack != null && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public Text getDisplayName() {
        return this.stack.getName();
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ShulkerBoxScreenHandler(syncId, inv, this);
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    @Override
    public void onOpen(PlayerEntity player) {
        if (!player.isSpectator()) {
            NbtCompound tag = this.stack.getSubNbt("BlockEntityTag");

            if (tag != null) {
                if (tag.contains("LootTable", 8)) {
                    Identifier lootTable = new Identifier(tag.getString("LootTable"));
                    long lootSeed = tag.getLong("LootTableSeed");
                    this.checkLootInteraction(player, lootTable, lootSeed);
                } else {
                    this.readFromTag(tag);
                }
            }


            if (player instanceof ServerPlayerEntity serverPlayerEntity) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeInt(player.getId());
                ServerPlayNetworking.send(serverPlayerEntity, ShulkerTrinketPackets.OPENING_BOX, buf);
                PlayerLookup.tracking(player)
                        .forEach(player1 -> ServerPlayNetworking.send(player1, ShulkerTrinketPackets.OPENING_BOX, buf));
            }
            player.world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SHULKER_BOX_OPEN,
                    SoundCategory.BLOCKS, 0.5F, player.world.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    public void checkLootInteraction(PlayerEntity player, Identifier lootTableId, long lootSeed) {

        if (lootTableId != null && player.world.getServer() != null) {
            LootTable lootTable = player.world.getServer().getLootManager().getTable(lootTableId);

            if (player instanceof ServerPlayerEntity) {
                Criteria.PLAYER_GENERATES_CONTAINER_LOOT.trigger((ServerPlayerEntity) player, lootTableId);
            }
            LootContext.Builder builder = (new LootContext.Builder((ServerWorld) player.world))
                    .parameter(LootContextParameters.ORIGIN, Vec3d.ofCenter(player.getBlockPos()))
                    .random(lootSeed);
            builder.luck(player.getLuck()).parameter(LootContextParameters.THIS_ENTITY, player);
            lootTable.supplyInventory(this, builder.build(LootContextTypes.CHEST));
        }
    }
    public void readFromTag(NbtCompound tag) {
        this.items = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);

        if (tag.contains("Items", 9)) {
            Inventories.readNbt(tag, this.items);
        }
    }
    public void writeToTag(NbtCompound tag) {
        Inventories.writeNbt(tag, this.items, true);
    }

    @Override
    public void onClose(PlayerEntity player) {

        if (!player.isSpectator()) {
            NbtCompound tag = this.stack.getSubNbt("BlockEntityTag");
            if (tag != null) {
                tag.remove("LootTable");
                tag.remove("LootTableSeed");
                this.writeToTag(tag);
            }

            if (player instanceof ServerPlayerEntity serverPlayerEntity) {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeInt(player.getId());
                ServerPlayNetworking.send(serverPlayerEntity, ShulkerTrinketPackets.CLOSING_BOX, buf);
                PlayerLookup.tracking(player)
                        .forEach(player1 -> ServerPlayNetworking.send(player1, ShulkerTrinketPackets.CLOSING_BOX, buf));
            }
            player.world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SHULKER_BOX_CLOSE,
                    SoundCategory.BLOCKS, 0.5F, player.world.random.nextFloat() * 0.1F + 0.9F);
        }
    }
}
