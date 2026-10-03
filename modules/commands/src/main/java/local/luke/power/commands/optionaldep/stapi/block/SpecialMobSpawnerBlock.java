package local.luke.power.commands.optionaldep.stapi.block;

import local.luke.power.commands.optionaldep.stapi.item.MobSpawnerBlockItem;
import net.minecraft.block.SpawnerBlock;
import net.modificationstation.stationapi.api.block.HasCustomBlockItemFactory;

@HasCustomBlockItemFactory(MobSpawnerBlockItem.class)
public class SpecialMobSpawnerBlock extends SpawnerBlock {
    public SpecialMobSpawnerBlock(int i, int j) {
        super(i, j);
    }
}
