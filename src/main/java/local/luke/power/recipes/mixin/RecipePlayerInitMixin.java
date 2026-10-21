package local.luke.power.recipes.mixin;

import local.luke.power.recipes.*;
import net.minecraft.class_585;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native player inventory overrides init without invoking ContainerScreen.init. */
@Mixin(class_585.class)
public abstract class RecipePlayerInitMixin {
  @Inject(method="init",at=@At("TAIL"))
  private void power$initBook(CallbackInfo ci){var book=((RecipeBookHost)this).power$recipeBook();if(book!=null)book.tick();}
}
