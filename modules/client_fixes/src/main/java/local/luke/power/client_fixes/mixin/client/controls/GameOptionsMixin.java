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

package local.luke.power.client_fixes.mixin.client.controls;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.client_fixes.Config;
import local.luke.power.client_fixes.client.ClientFixesClientMod;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Shadow
    public KeyBinding[] allKeys;

    @Inject(method = {"<init>()V", "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V"}, at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        ArrayList<KeyBinding> newKeys = new ArrayList<>(Arrays.asList(allKeys));


        if (Config.config.enableDebugGraphChanges) {
            newKeys.add(ClientFixesClientMod.DEBUG_GRAPH_KEYBIND);
        }

        allKeys = newKeys.toArray(new KeyBinding[0]);
    }
}
