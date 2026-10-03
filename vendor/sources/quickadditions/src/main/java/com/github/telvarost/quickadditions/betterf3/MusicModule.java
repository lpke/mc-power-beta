package com.github.telvarost.quickadditions.betterf3;

import com.github.telvarost.quickadditions.ModHelper;
import com.github.telvarost.quickadditions.events.init.ModuleListener;
import net.minecraft.client.Minecraft;
import net.modificationstation.stationapi.api.util.Identifier;
import ralf2oo2.betterf3.modules.BaseModule;
import ralf2oo2.betterf3.utils.DebugLine;

public class MusicModule extends BaseModule {

    public MusicModule(Identifier id) {
        super(id);
        this.defaultNameColor = 0x00AA00;
        this.defaultValueColor = 0x55FFFF;

        this.nameColor = defaultNameColor;
        this.valueColor = defaultValueColor;

        lines.add(new DebugLine("music").withNamespace(ModuleListener.NAMESPACE));
        //lines.get(0).inReducedDebug = true;
    }

    @Override
    public void update(Minecraft minecraft) {
        lines.get(0).setValue(ModHelper.ModHelperFields.currentBGM);
    }
}
