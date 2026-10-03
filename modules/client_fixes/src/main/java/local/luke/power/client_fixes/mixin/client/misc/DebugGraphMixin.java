/*
 * Copyright (C) 2022-2025 js6pak
 *
 * This file is part of MojangFixStationAPI.
 *
 * MojangFixStationAPI is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, version 3.
 *
 * MojangFixStationAPI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with MojangFixStationAPI. If not, see <https://www.gnu.org/licenses/>.
 */

package local.luke.power.client_fixes.mixin.client.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.Config;
import local.luke.power.client_fixes.ModHelper;
import local.luke.power.client_fixes.client.ClientFixesClientMod;

@Mixin(Minecraft.class)
public abstract class DebugGraphMixin {

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z", ordinal = 0, shift = At.Shift.BEFORE))
    private void onF3(CallbackInfo ci) {
        if (Config.config.enableDebugGraphModernToggle) {
            ModHelper.ModHelperFields.isDebugGraphOn = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWorldRemote()Z", ordinal = 0))
    private void onKey(CallbackInfo ci) {
        if (!Config.config.enableDebugGraphModernToggle) {
            if (Keyboard.getEventKey() == local.luke.power.input.Bindings.eventCode(ClientFixesClientMod.DEBUG_GRAPH_KEYBIND)) {
                ModHelper.ModHelperFields.isDebugGraphOn = !ModHelper.ModHelperFields.isDebugGraphOn;
            }
        }
    }

    @Redirect(method = "run", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z", opcode = Opcodes.GETFIELD))
    private boolean getShowDebugInfo(GameOptions gameSettings) {
        return gameSettings.debugHud && ModHelper.ModHelperFields.isDebugGraphOn;
    }
}
