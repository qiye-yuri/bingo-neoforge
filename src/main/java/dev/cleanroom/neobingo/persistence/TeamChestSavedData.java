package dev.cleanroom.neobingo.persistence;

import dev.cleanroom.neobingo.domain.TeamId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** 在服务器存档中维护每支队伍的共享箱，并在新对局开始时统一清空。 */
public final class TeamChestSavedData extends SavedData {
    private static final String DATA_NAME = "neo_bingo_team_chests";
    private static final Factory<TeamChestSavedData> FACTORY =
            new Factory<>(TeamChestSavedData::new, TeamChestSavedData::load);

    private final Map<TeamId, SimpleContainer> inventories = new HashMap<>();

    public static TeamChestSavedData get(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public SimpleContainer inventory(TeamId team, int rows) {
        int size = Math.clamp(rows, 1, 6) * 9;
        SimpleContainer existing = inventories.get(team);
        if (existing != null && existing.getContainerSize() == size) return existing;

        SimpleContainer resized = create(size);
        if (existing != null) {
            for (int slot = 0; slot < Math.min(existing.getContainerSize(), size); slot++) {
                resized.setItem(slot, existing.getItem(slot));
            }
        }
        inventories.put(team, resized);
        setDirty();
        return resized;
    }

    public void clearAll() {
        inventories.values().forEach(SimpleContainer::clearContent);
        inventories.clear();
        setDirty();
    }

    private SimpleContainer create(int size) {
        return new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                TeamChestSavedData.this.setDirty();
            }
        };
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag teams = new ListTag();
        inventories.forEach((team, inventory) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("team", team.value());
            entry.putInt("size", inventory.getContainerSize());
            ListTag items = new ListTag();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty()) continue;
                CompoundTag item = new CompoundTag();
                item.putInt("slot", slot);
                item.put("stack", stack.save(registries));
                items.add(item);
            }
            entry.put("items", items);
            teams.add(entry);
        });
        tag.put("teams", teams);
        return tag;
    }

    private static TeamChestSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TeamChestSavedData data = new TeamChestSavedData();
        ListTag teams = tag.getList("teams", Tag.TAG_COMPOUND);
        for (int index = 0; index < teams.size(); index++) {
            CompoundTag entry = teams.getCompound(index);
            int size = Math.clamp(entry.getInt("size"), 9, 54);
            size -= size % 9;
            SimpleContainer inventory = data.create(size == 0 ? 27 : size);
            ListTag items = entry.getList("items", Tag.TAG_COMPOUND);
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                CompoundTag item = items.getCompound(itemIndex);
                int slot = item.getInt("slot");
                if (slot >= 0 && slot < inventory.getContainerSize()) {
                    inventory.setItem(slot, ItemStack.parseOptional(registries, item.getCompound("stack")));
                }
            }
            data.inventories.put(new TeamId(entry.getString("team")), inventory);
        }
        return data;
    }
}
