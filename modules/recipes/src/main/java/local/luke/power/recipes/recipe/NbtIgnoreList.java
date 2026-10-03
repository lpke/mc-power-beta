package local.luke.power.recipes.recipe;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import local.luke.power.recipes.api.AMINbt;
import local.luke.power.recipes.config.AMIConfig;
import local.luke.power.recipes.util.RecipeBrowser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NbtIgnoreList implements local.luke.power.recipes.api.NbtIgnoreList {
    private final Set<String> nbtTagNameBlacklist = new HashSet<>();
    private final HashMultimap<Item, String> itemNbtTagNameBlacklist = HashMultimap.create();

    @Override
    public void ignoreNbtTagNames(@Nullable Item item, String... nbtTagNames) {
        if (item == null) {
            RecipeBrowser.LOGGER.error("Null item", new NullPointerException());
            return;
        }
        Collection<String> ignoredNbtTagNames = itemNbtTagNameBlacklist.get(item);
        Collections.addAll(ignoredNbtTagNames, nbtTagNames);
    }

    @Override
    public boolean isNbtTagIgnored(@Nullable String nbtTagName) {
        if (nbtTagName == null) {
            RecipeBrowser.LOGGER.error("Null nbtTagName", new NullPointerException());
            return false;
        }
        return AMIConfig.INSTANCE.nbtBlacklist.contains(nbtTagName) || nbtTagNameBlacklist.contains(nbtTagName);
    }

    @Nullable
    @Override
    public NbtCompound getNbt(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            RecipeBrowser.LOGGER.error("Null itemStack", new NullPointerException());
            return null;
        }

        NbtCompound nbtTagCompound = itemStack.getStationNbt();
        if (nbtTagCompound == null) {
            return null;
        }

        Set<String> keys = ((AMINbt) nbtTagCompound).always_More_Items$getKeySet();

        Set<String> allIgnoredKeysForItem = itemNbtTagNameBlacklist.get(itemStack.getItem());

        Set<String> ignoredKeysConfig = Sets.intersection(keys, new HashSet<>(AMIConfig.INSTANCE.nbtBlacklist));
        Set<String> ignoredKeysApi = Sets.intersection(keys, nbtTagNameBlacklist);
        Set<String> ignoredKeysApiForItem = Sets.intersection(keys, allIgnoredKeysForItem);

        Set<String> ignoredKeys = Sets.union(ignoredKeysConfig, ignoredKeysApi);
        ignoredKeys = Sets.union(ignoredKeys, ignoredKeysApiForItem);

        if (ignoredKeys.isEmpty()) {
            return nbtTagCompound;
        }

        NbtCompound nbtTagCompoundCopy = nbtTagCompound.copy();
        for (String ignoredKey : ignoredKeys) {
            ((AMINbt) nbtTagCompoundCopy).always_More_Items$removeTag(ignoredKey);
        }
        return nbtTagCompoundCopy;
    }
}
