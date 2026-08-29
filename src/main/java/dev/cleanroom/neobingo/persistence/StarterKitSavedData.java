package dev.cleanroom.neobingo.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** 保存大厅中由管理员直接编辑的 36 格初始物资背包。 */
public final class StarterKitSavedData extends SavedData {
    public static final int SIZE = 36;
    private static final String DATA_NAME = "neo_bingo_starter_kit";
    private static final Factory<StarterKitSavedData> FACTORY =
            new Factory<>(StarterKitSavedData::new, StarterKitSavedData::load);

    private final SimpleContainer inventory = createInventory();

    public static StarterKitSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    public void clear() {
        inventory.clearContent();
        setDirty();
    }

    /** 每次开局取得独立副本，避免玩家修改背包时影响下一局的模板。 */
    public List<ItemStack> snapshot() {
        List<ItemStack> result = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) result.add(inventory.getItem(slot).copy());
        return List.copyOf(result);
    }

    /** 将旧版“物品 ID → 数量”配置一次性迁入可视化背包。 */
    public void importLegacy(Map<String, Integer> legacyItems) {
        legacyItems.forEach((id, count) -> BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.parse(id))
                .ifPresent(item -> inventory.addItem(new ItemStack(item, count))));
        if (!legacyItems.isEmpty()) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag items = new ListTag();
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", slot);
            entry.put("stack", stack.save(registries));
            items.add(entry);
        }
        tag.put("items", items);
        return tag;
    }

    private static StarterKitSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        StarterKitSavedData data = new StarterKitSavedData();
        ListTag items = tag.getList("items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag entry = items.getCompound(index);
            int slot = entry.getInt("slot");
            if (slot >= 0 && slot < SIZE) {
                data.inventory.setItem(slot,
                        ItemStack.parseOptional(registries, entry.getCompound("stack")));
            }
        }
        return data;
    }

    private SimpleContainer createInventory() {
        return new SimpleContainer(SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                StarterKitSavedData.this.setDirty();
            }
        };
    }
}
