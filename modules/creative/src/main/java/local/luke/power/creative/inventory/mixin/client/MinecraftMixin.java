package local.luke.power.creative.inventory.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ClientInteractionManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.living.player.AbstractClientPlayer;
import net.minecraft.inventory.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.level.Level;
import net.minecraft.util.hit.HitResult;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.block.BlockState;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import local.luke.power.creative.inventory.CreativeInventory;
import local.luke.power.creative.inventory.api.BlockSelectAPI;
import local.luke.power.creative.inventory.registry.TabRegistry;
import local.luke.power.creative.inventory.registry.TabRegistryEvent;
import local.luke.power.creative.inventory.util.SlotUpdatePacket;

@Mixin(Minecraft.class)
public class MinecraftMixin {
  @Shadow public AbstractClientPlayer player;
  @Shadow public HitResult hitResult;
  @Shadow public Level level;
  @Shadow private int attackCooldown;
  @Shadow private int ticksPlayed;
  @Unique private final local.luke.power.creative.CreativeBreakCooldown power$breakCooldown = new local.luke.power.creative.CreativeBreakCooldown();
  @Unique private boolean power$brokeBlock;

  @Inject(method = "tick", at = @At("HEAD"))
  private void power$breakContext(CallbackInfo ci) {
    power$breakCooldown.context(level, player, ticksPlayed);
  }

  @Inject(method = "pickupHitBlock", at = @At("HEAD"), cancellable = true)
  private void creative_setMouseButtonItem(CallbackInfo info) {
    if (this.player == null
        || this.level == null
        || !this.player.creative_isCreative()
        || this.hitResult == null) return;
    if (this.hitResult.type != net.minecraft.util.hit.HitType.BLOCK) {
      info.cancel();
      return;
    }

    BlockState state = level.getBlockState(this.hitResult.x, this.hitResult.y, this.hitResult.z);
    int meta = level.getBlockMeta(this.hitResult.x, this.hitResult.y, this.hitResult.z);
    ItemStack stack = BlockSelectAPI.convert(state, meta);

    if (stack == null) return;

    PlayerInventory inventory = this.player.inventory;

    info.cancel();

    int selectedSlot = inventory.selectedHotbarSlot;
    boolean selectEmpty = true;

    for (byte slot = 0; slot < 9; slot++) {
      ItemStack itemInv = inventory.getItem(slot);
      if (itemInv == null) {
        if (selectEmpty) {
          selectedSlot = slot;
          selectEmpty = false;
        }
      } else if (itemInv.itemId == stack.itemId && itemInv.getDamage() == stack.getDamage()) {
        inventory.selectedHotbarSlot = slot;
        return;
      }
    }

    inventory.selectedHotbarSlot = selectedSlot;
    inventory.setItem(selectedSlot, stack);
    PacketHelper.send(new SlotUpdatePacket(selectedSlot, stack));
  }

  @WrapOperation(method = "tick", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/Minecraft;processAttack(I)V", ordinal = 0))
  private void power$creativePress(Minecraft mc, int button, Operation<Void> original) {
    int previous = attackCooldown;
    boolean restart = button == 0 && local.luke.power.creative.ClientRuntime.active(mc)
        && player.creative_isCreative() && hitResult != null
        && hitResult.type == net.minecraft.util.hit.HitType.BLOCK
        && power$breakCooldown.restart(level, player, ticksPlayed, previous);
    power$brokeBlock = false;
    if (restart) attackCooldown = 0;
    original.call(mc, button);
    // A canceled or unsupported interaction does not consume an existing delay.
    if (restart && !power$brokeBlock && attackCooldown == 0) attackCooldown = previous;
  }

  @WrapOperation(method = "processAttack", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/ClientInteractionManager;playerDigBlock(IIII)V"))
  private void creative_blockBreakDelay(ClientInteractionManager manager, int x, int y, int z, int face, Operation<Void> original) {
    boolean creative = level != null && !level.isRemote && player != null && player.creative_isCreative();
    BlockState before = creative ? level.getBlockState(x, y, z) : null;
    original.call(manager, x, y, z, face);
    if (level != null && level.isRemote && player != null && player.creative_isCreative()) {
      attackCooldown = 5;
      return;
    }
    if (!creative || before == level.getBlockState(x, y, z)) return;
    power$brokeBlock = true;
    attackCooldown = 5;
    power$breakCooldown.broken(level, player, ticksPlayed);
  }

  @Inject(
      method = "run",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/Minecraft;init()V",
              shift = Shift.AFTER))
  private void creative_onGameInit(CallbackInfo info) {
    CreativeInventory.LOGGER.info("Register creative tabs");
    StationAPI.EVENT_BUS.post(new TabRegistryEvent());
    TabRegistry.sort();
  }
}
