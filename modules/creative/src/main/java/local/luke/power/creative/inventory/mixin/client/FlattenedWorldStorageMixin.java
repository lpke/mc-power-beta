package local.luke.power.creative.inventory.mixin.client;

import net.minecraft.level.LevelProperties;
import net.minecraft.level.storage.LevelMetadata;
import net.minecraft.util.io.CompoundTag;
import net.modificationstation.stationapi.impl.world.storage.FlattenedWorldStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mixin(FlattenedWorldStorage.class)
public class FlattenedWorldStorageMixin {
	@SuppressWarnings("rawtypes")
	@Inject(method = "getMetadata", at = @At(
		value = "INVOKE",
		target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z",
		shift = Shift.AFTER
	), locals = LocalCapture.CAPTURE_FAILSOFT)
	private void creative_setMetadata(CallbackInfoReturnable<List> info, ArrayList worlds, File[] var2, int var3, int var4, File worldPath, String worldFolder, LevelProperties data, CompoundTag worldTag, boolean requiresUpdating, String worldName) {
		LevelMetadata meta = (LevelMetadata) worlds.get(worlds.size() - 1);
    var rules = (local.luke.power.rules.WorldRules) data;
    ((local.luke.power.creative.inventory.interfaces.CreativeLevel) meta).creative_setCreative(
        rules.power$rulesProblem().isEmpty()
            && rules.power$rules().enabled(local.luke.power.rules.GameRule.ALLOW_CREATIVE_MODE));
	}
}
