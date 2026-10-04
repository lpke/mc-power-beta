package local.luke.power.creative.inventory.mixin.client;

import net.minecraft.level.storage.LevelMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import local.luke.power.creative.inventory.interfaces.CreativeLevel;

@Mixin(LevelMetadata.class)
public class LevelMetadataMixin implements CreativeLevel, local.luke.power.permissions.CheatWorld {
  @Unique private boolean power$cheats;
  public boolean power$cheatsEnabled() { return power$cheats; }
  public void power$cheatsEnabled(boolean enabled) { power$cheats = enabled; }
	@Unique private boolean creative_isCreative;
	
	@Override
	public boolean creative_isCreative() {
		return creative_isCreative;
	}
	
	@Override
	public void creative_setCreative(boolean creative) {
		creative_isCreative = creative;
	}
}
