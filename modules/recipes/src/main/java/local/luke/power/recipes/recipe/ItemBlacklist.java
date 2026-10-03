package local.luke.power.recipes.recipe;

import local.luke.power.recipes.config.AMIConfig;
import local.luke.power.recipes.util.RecipeBrowser;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemBlacklist implements local.luke.power.recipes.api.ItemBlacklist {
    @Nonnull
    private final Set<String> itemBlacklist = new HashSet<>();

    @Override
    public void addItemToBlacklist(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            RecipeBrowser.LOGGER.error("Null itemStack", new NullPointerException());
            return;
        }
        String uid = RecipeBrowser.getStackHelper().getUniqueIdentifierForStack(itemStack);
        itemBlacklist.add(uid);

        RecipeBrowser.resetItemFilter();
    }

    @Override
    public void removeItemFromBlacklist(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            RecipeBrowser.LOGGER.error("Null itemStack", new NullPointerException());
            return;
        }
        String uid = RecipeBrowser.getStackHelper().getUniqueIdentifierForStack(itemStack);
        itemBlacklist.remove(uid);

        RecipeBrowser.resetItemFilter();
    }

    @Override
    public boolean isItemBlacklisted(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            RecipeBrowser.LOGGER.error("Null itemStack", new NullPointerException());
            return false;
        }
        List<String> uids = RecipeBrowser.getStackHelper().getUniqueIdentifiersWithWildcard(itemStack);
        return uids.stream().anyMatch(uid -> itemBlacklist.contains(uid) || AMIConfig.INSTANCE.itemBlacklist.contains(uid));
    }

    @Override
    public boolean isItemAPIBlacklisted(@Nullable ItemStack itemStack) {
        if (itemStack == null) {
            RecipeBrowser.LOGGER.error("Null itemStack", new NullPointerException());
            return false;
        }
        List<String> uids = RecipeBrowser.getStackHelper().getUniqueIdentifiersWithWildcard(itemStack);
        return uids.stream().anyMatch(itemBlacklist::contains);
    }

    public static void reset() {
        RecipeBrowser.getHelpers().getItemBlacklist().itemBlacklist.clear();
    }
}
