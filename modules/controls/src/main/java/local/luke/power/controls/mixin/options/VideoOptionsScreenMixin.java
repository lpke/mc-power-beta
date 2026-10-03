package local.luke.power.controls.mixin.options;

import local.luke.power.controls.ControlFeatures;
import local.luke.power.controls.util.ModOptions;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.option.Option;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(VideoOptionsScreen.class)
public class VideoOptionsScreenMixin {
    @Shadow
    private static Option[] VIDEO_OPTIONS;

    @Unique
    private static final ArrayList<Option> addedOptions = new ArrayList<>();

    static {
        addedOptions.add(ModOptions.fogDensityOption);

        addedOptions.add(ModOptions.cloudsOption);

        addedOptions.add(ModOptions.cloudHeightOption);

        addedOptions.add(ModOptions.brightnessOption);

        VIDEO_OPTIONS = Arrays.copyOf(VIDEO_OPTIONS, VIDEO_OPTIONS.length + addedOptions.size());
        for (int i = 0; i < addedOptions.size(); i++) {
            VideoOptionsScreenMixin.VIDEO_OPTIONS[VideoOptionsScreenMixin.VIDEO_OPTIONS.length - (addedOptions.size() - i)] = addedOptions.get(i);
        }

        VideoOptionsScreenMixin.VIDEO_OPTIONS[3] = ModOptions.fpsLimitOption;

        VideoOptionsScreenMixin.VIDEO_OPTIONS[1] = ModOptions.renderDistanceOption;

        VideoOptionsScreenMixin.VIDEO_OPTIONS[6] = ModOptions.guiScaleOption;
    }
}
