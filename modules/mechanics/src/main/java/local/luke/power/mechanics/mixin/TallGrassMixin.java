package local.luke.power.mechanics.mixin;

import local.luke.power.mechanics.Config;
import local.luke.power.mechanics.ModHelperStationAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TallPlantBlock.class)
class TallGrassMixin extends PlantBlock {

    public TallGrassMixin(int i, int j) {
        super(i, j);
    }

    @Override
    public void onPlaced(World arg, int i, int j, int k) {
        arg.setBlockMeta(i, j, k, 1);
    }

    @Override
    public void afterBreak(World world, PlayerEntity playerEntity, int x, int y, int z, int meta) {
        if (world.isRemote) return;
        int dropId = -1;
        ItemStack held = playerEntity.inventory.getSelectedItem();
        if (held != null && held.itemId == Item.SHEARS.id) {
            if (meta == 2 && Config.config.FLORA_CONFIG.enableShearsCollectFern)
                dropId = ModHelperStationAPI.identifierToItemId("power_creative_inventory:fern");
            else if (meta == 1 && Config.config.FLORA_CONFIG.enableShearsCollectTallGrass)
                dropId = ModHelperStationAPI.identifierToItemId("power_creative_inventory:tall_grass");
            if (dropId > 0) held.damage(1, playerEntity);
        }

        playerEntity.increaseStat(Stats.MINE_BLOCK[this.id], 1);

        if (dropId > 0) {
            this.dropStack(world, x, y, z, new ItemStack(dropId, 1, 0));
        } else if (FabricLoader.getInstance().isModLoaded("wolves")) {
            this.dropWithChance(world, x, y, z, world.getBlockState(x, y, z), meta, 1.0F);
        } else {
            this.dropStacks(world, x, y, z, meta);
        }
    }
}

