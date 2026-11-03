package local.luke.power.validation.mixin;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PlayerEntityRenderer.class)
public interface CapeRenderAccess {
  @Invoker("method_827") void power$renderCape(PlayerEntity player,float delta);
  @Accessor("bipedModel") BipedEntityModel power$capeModel();
}
