package local.luke.power.gameplay;

import local.luke.power.input.DetachedCamera;
import local.luke.power.input.PlayerMode;
import local.luke.power.rules.GameRule;
import local.luke.power.rules.WorldRules;
import local.luke.power.sprint.SprintPlayer;
import net.minecraft.block.Material;
import net.minecraft.class_385;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.Box;

/** Java 1.21.1's local-player swimming and shared collision-safe crouching/crawling poses. */
public final class ModernSwimming {
  private ModernSwimming() {}

  public static boolean compact(Entity entity) {
    return entity instanceof SwimPlayer player && player.power$swim().compact;
  }

  public static boolean crouched(Entity entity) {
    return entity instanceof SwimPlayer player && player.power$swim().crouched;
  }

  public static boolean lowPose(Entity entity) { return compact(entity) || crouched(entity); }

  public static boolean swimming(Entity entity) {
    return entity instanceof SwimPlayer player && player.power$swim().swimming
        && ModernWater.eligible(entity) && permitted((ClientPlayerEntity) entity);
  }

  public static boolean permitted(ClientPlayerEntity player) {
    return player.world != null && !player.world.isRemote
        && WorldRules.enabled(player.world.method_262(), GameRule.ALLOW_SPRINTING);
  }

  /** Starting requires submerged eyes; an existing swim may continue at the surface. */
  public static boolean canSprint(ClientPlayerEntity player) {
    return ModernWater.eligible(player) && player.method_1334()
        && (swimming(player) || eyesInWater(player));
  }

  public static boolean eyesInWater(ClientPlayerEntity player) {
    return waterAt(player, player.boundingBox.minY + (compact(player) ? SwimState.EYE_HEIGHT : crouched(player) ? 1.27 : 1.62));
  }

  public static void preparePose(ClientPlayerEntity player) {
    SwimState state = ((SwimPlayer)player).power$swim();
    // Resolve obstructions before Beta's upright push-out checks and sprint
    // eligibility. Resize once, retaining the native feet and input magnitudes.
    updatePose(player, state);
    state.inputPosePrepared = true;
  }

  public static void prepareInput(ClientPlayerEntity player) {
    SwimState state = ((SwimPlayer)player).power$swim();
    if (!state.inputPosePrepared) preparePose(player);
    var input=player.field_161;
    // Undo only the real keyboard adapter's crouch scaling, including auto-walk's
    // matching .3 factor. Other input producers already supply their own magnitudes.
    if (input instanceof net.minecraft.class_413 && ModernWater.eligible(player)
        && player.method_1334() && input.field_2536) {
      input.field_2532 /= .3f;
      input.field_2533 /= .3f;
    }
    if (lowPose(player) && !player.method_1334()
        && !(input instanceof net.minecraft.class_413 && input.field_2536)) {
      input.field_2532 *= .3f;
      input.field_2533 *= .3f;
    }
    if (loweredCamera(((SwimPlayer)player).power$swim())) player.field_1640=0;
  }

  private static boolean waterAt(ClientPlayerEntity player, double height) {
    if (player.world == null || !Double.isFinite(height) || !Double.isFinite(player.x)
        || !Double.isFinite(player.z)) return false;
    int x = (int) Math.floor(player.x), y = (int) Math.floor(height), z = (int) Math.floor(player.z);
    if (y < 0 || y >= 128 || !player.world.method_239(x, y, z)
        || player.world.method_1779(x, y, z) != Material.WATER) return false;
    double surface = y < 127 && player.world.method_1779(x, y + 1, z) == Material.WATER
        ? y + 1 : y + 1 - class_385.method_1218(player.world.method_1778(x, y, z));
    return height < surface;
  }

  /** Once per movement tick, after the shared sprint controller resolves input/stamina. */
  public static void tick(ClientPlayerEntity player) {
    SwimState state = ((SwimPlayer) player).power$swim();
    if (player.dead || player.health <= 0) {
      state.swimming = state.compact = state.crouched = false;
      state.inputPosePrepared = false;
      state.previousAmount = state.amount = 0;
      return;
    }
    boolean wet = player.method_1334(), wasSwimming = state.swimming;
    boolean sprint = player instanceof SprintPlayer sprinter && sprinter.power$sprint().sprinting;
    state.swimming = ModernWater.eligible(player) && permitted(player) && sprint && wet
        && (state.swimming || eyesInWater(player) && waterAt(player, player.boundingBox.minY + .001));
    if (!state.inputPosePrepared || wasSwimming != state.swimming) updatePose(player, state);
    state.inputPosePrepared = false;
    state.animate();
    state.cameraTick(loweredCamera(state) ? player.field_1631 : 1.62f);
  }

  private static boolean loweredCamera(SwimState state) {
    return state.compact || state.crouched && (state.lowCeiling
        || GameplayConfig.current().modernCrawling && GameplayConfig.current().crouchCameraHeight);
  }

  public static void cancel(ClientPlayerEntity player) {
    SwimState state = ((SwimPlayer) player).power$swim();
    state.swimming = false;
    if (!player.dead && player.health > 0) updatePose(player, state);
  }

  /** Rebase before native death changes its eye offset, preserving death-record feet. */
  public static void died(ClientPlayerEntity player) {
    if(player.world==null || player.world.isRemote)return;
    SwimState state=((SwimPlayer)player).power$swim();
    if(lowPose(player)) {
      double feet=player.boundingBox.minY,offset=1.62f-player.field_1631;
      player.field_1631=1.62f;
      player.prevY+=offset;player.field_1638+=offset;player.field_1640=0;
      player.method_1340(player.x,feet+player.field_1631,player.z);
    }
    state.swimming=state.compact=state.crouched=false;
    state.inputPosePrepared=false;
    state.previousAmount=state.amount=0;state.waterDepth=0;state.waterJumpTicks=0;
    state.resetCamera(1.62f);
  }

  private static void updatePose(ClientPlayerEntity player, SwimState state) {
    if (player.world == null || player.world.isRemote || player.dead || player.health <= 0) return;
    if (player.method_943()) {
      // Native bed entry has already placed and resized the player.
      state.swimming = state.compact = state.crouched = false;
      state.previousAmount = state.amount = 0;
      state.resetCamera(1.62f);
      return;
    }
    // Native mounting owns its pose even if an enclosing vehicle collides with
    // a standing test. It must never initiate crawling.
    if (player.field_1595 != null) {
      if (state.compact || state.crouched) resize(player, false, false);
      state.swimming = state.compact = state.crouched = false;
      return;
    }
    boolean crawling = GameplayConfig.current().modernCrawling && ModernWater.localMovement(player);
    boolean small = state.swimming, crouched = false;
    if (!small && (state.compact || state.crouched || crawling)) {
      // Modern Player.updatePlayerPose first proves the compact box fits. A
      // wall through the feet must not manufacture a new crawl, nor force an
      // unsafe standing box when terrain is unavailable.
      if (crawling && !canFit(player, SwimState.HEIGHT)) return;
      boolean sneak = crawling && player.field_161 != null && player.field_161.field_2536;
      state.lowCeiling = !canFit(player, 1.8);
      if (sneak || state.lowCeiling) {
        crouched = canFit(player, 1.5);
        small = !crouched;
      }
    } else state.lowCeiling = false;
    if (small != state.compact || crouched != state.crouched) {
      state.compact = small;
      state.crouched = crouched;
      resize(player, small, crouched);
    }
    if (loweredCamera(state)) {
      // Compact poses own their camera. Ordinary crouching retains Beta's
      // native sneak offset when the extra camera lowering is disabled.
      player.field_1640 = 0;
    }
  }

  private static boolean canFit(ClientPlayerEntity player,double height) {
    var box = player.boundingBox;
    if (!Double.isFinite(box.minX) || !Double.isFinite(box.minY) || !Double.isFinite(box.minZ)
        || !Double.isFinite(box.maxX) || !Double.isFinite(box.maxZ)) return false;
    int x0 = (int) Math.floor(box.minX), x1 = (int) Math.floor(box.maxX);
    int y0 = Math.max(0, (int) Math.floor(box.minY)-1), y1 = Math.min(127, (int) Math.floor(box.minY + height));
    int z0 = (int) Math.floor(box.minZ), z1 = (int) Math.floor(box.maxZ);
    // Native collision includes neighbor-shaped blocks and a .25-expanded entity
    // query with a further two-block chunk margin. Guard the whole footprint.
    if (x1 - x0 > 2 || z1 - z0 > 2 || !player.world.method_155(x0-3, y0, z0-3, x1+3, y1, z1+3)) return false;
    Box standing = Box.getOrCreate(box.minX + 1e-7, box.minY + 1e-7, box.minZ + 1e-7,
        box.maxX - 1e-7, box.minY + height - 1e-7, box.maxZ - 1e-7);
    return player.world.method_190(player, standing).isEmpty();
  }

  private static void resize(ClientPlayerEntity player, boolean compact,boolean crouched) {
    double feet = player.boundingBox.minY;
    float previousOffset=player.field_1631;
    player.spacingXZ = SwimState.WIDTH;
    player.spacingY = compact ? SwimState.HEIGHT : crouched ? 1.5f : 1.8f;
    player.field_1631 = compact ? SwimState.EYE_HEIGHT : crouched ? 1.27f : 1.62f;
    // Native previous positions contain the eye origin. Keep their feet fixed as
    // that origin changes; the camera owns its separate modern half-response.
    double offset=player.field_1631-previousOffset;
    player.prevY+=offset;
    player.field_1638+=offset;
    if (loweredCamera(((SwimPlayer)player).power$swim())) player.field_1640 = 0;
    // Native positioning adds sneak/step smoothing back to the collision box.
    // Subtract it here so retaining Beta's camera motion never moves the feet.
    player.method_1340(player.x, feet + player.field_1631 - player.field_1640, player.z);
  }

  public static void steer(ClientPlayerEntity player) {
    if (!swimming(player)) return;
    player.velocityY = SwimState.steer(player.velocityY, player.pitch,
        player.field_161 != null && player.field_161.field_2535,
        waterAt(player, player.boundingBox.minY + .9));
  }

  public static float amount(Entity entity, float delta) {
    if (!(entity instanceof SwimPlayer player) || entity.dead
        || entity instanceof PlayerMode mode && (mode.power$flying() || mode.power$spectator())
        || DetachedCamera.isActive() && !compact(entity)) return 0;
    return player.power$swim().amount(delta);
  }

  /** The native camera subtracts eyeOffset-1.62; replace only its render offset. */
  public static float cameraOffset(Entity entity,float delta,float nativeOffset) {
    if (!ownsCameraHeight(entity)) return nativeOffset;
    return 1.62f+nativeOffset-((SwimPlayer)entity).power$swim().cameraEye(delta);
  }

  /** Match the camera's pose height without applying bobbing, perspective or free look. */
  public static double interactionEyeOffset(Object actor,float delta,double nativeFallback) {
    if (!(actor instanceof Entity entity) || !ownsCameraHeight(entity)) return nativeFallback;
    return ((SwimPlayer)entity).power$swim().cameraEye(delta)-entity.field_1631;
  }

  private static boolean ownsCameraHeight(Entity entity) {
    if (!(entity instanceof SwimPlayer owner) || !ModernWater.localMovement(entity)) return false;
    SwimState state=owner.power$swim();
    return lowPose(entity) || Math.abs(state.previousEyeHeight-1.62f)>=1e-6f
        || Math.abs(state.eyeHeight-1.62f)>=1e-6f;
  }
}
