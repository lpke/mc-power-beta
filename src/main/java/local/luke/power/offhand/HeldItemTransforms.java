package local.luke.power.offhand;

import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.client.StationRenderAPI;
import net.modificationstation.stationapi.api.client.render.RendererAccess;
import net.modificationstation.stationapi.api.client.render.model.VanillaBakedModel;
import net.modificationstation.stationapi.api.client.render.model.json.ModelTransformation;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases;
import net.modificationstation.stationapi.api.item.BlockItemForm;
import org.lwjgl.opengl.GL11;

/** Modern 26.3 item display frames, adapted to Beta's native geometry and renderer hooks. */
public final class HeldItemTransforms {
  enum Kind { GENERATED, HANDHELD, BOW, ROD, BLOCK }
  interface Pose {
    void translate(float x, float y, float z);
    void rotate(float degrees, float x, float y, float z);
    void scale(float x, float y, float z);
  }
  private static final Pose GL = new Pose() {
    public void translate(float x, float y, float z) { GL11.glTranslatef(x, y, z); }
    public void rotate(float angle, float x, float y, float z) { GL11.glRotatef(angle, x, y, z); }
    public void scale(float x, float y, float z) { GL11.glScalef(x, y, z); }
  };
  private record Display(float x, float y, float z, float tx, float ty, float tz, float scale) {
    void apply(Pose pose, int side) {
      // ItemTransform.apply mirrors translation X and rotation Y/Z for the left hand.
      pose.translate(side * tx / 16, ty / 16, tz / 16);
      pose.rotate(x, 1, 0, 0); pose.rotate(side * y, 0, 1, 0); pose.rotate(side * z, 0, 0, 1);
      pose.scale(scale, scale, scale);
    }
  }
  // Values from the official item/generated, handheld, bow, handheld_rod and block/block models.
  private static final Display GENERATED_FIRST = new Display(0, -90, 25, 1.13f, 3.2f, 1.13f, .68f);
  private static final Display HANDHELD_FIRST_LEFT = new Display(0, 90, -25, 1.13f, 3.2f, 1.13f, .68f);
  private static final Display ROD_FIRST_LEFT = new Display(0, -90, -25, 0, 1.6f, .8f, .68f);
  private static final Display ROD_FIRST_RIGHT = new Display(0, 90, 25, 0, 1.6f, .8f, .68f);
  private static final Display BLOCK_FIRST_LEFT = new Display(0, 225, 0, 0, 0, 0, .4f);
  private static final Display BLOCK_FIRST_RIGHT = new Display(0, 45, 0, 0, 0, 0, .4f);
  private static final Display GENERATED_THIRD = new Display(0, 0, 0, 0, 3, 1, .55f);
  private static final Display HANDHELD_THIRD_LEFT = new Display(0, 90, -55, 0, 4, .5f, .85f);
  private static final Display HANDHELD_THIRD_RIGHT = new Display(0, -90, 55, 0, 4, .5f, .85f);
  private static final Display BOW_THIRD_LEFT = new Display(-80, -280, 40, -1, -2, 2.5f, .9f);
  private static final Display BOW_THIRD_RIGHT = new Display(-80, 260, -40, -1, -2, 2.5f, .9f);
  private static final Display ROD_THIRD_LEFT = new Display(0, -90, -55, 0, 4, 2.5f, .85f);
  private static final Display ROD_THIRD_RIGHT = new Display(0, 90, 55, 0, 4, 2.5f, .85f);
  private static final Display BLOCK_THIRD = new Display(75, 45, 0, 0, 2.5f, 0, .375f);
  private HeldItemTransforms() {}

  static Kind kind(ItemStack stack) {
    var item = stack.getItem();
    Block block = item instanceof BlockItemForm form ? form.getBlock()
        : stack.itemId >= 0 && stack.itemId < 256 ? Block.BLOCKS[stack.itemId] : null;
    if (block != null && class_13.method_42(block.method_1621())) return Kind.BLOCK;
    if (item instanceof class_367) return Kind.BOW;
    if (item instanceof class_607) return Kind.ROD;
    // Beta marks sugar as full-3D too; its modern model inherits generated, unlike sticks/bones.
    return item.method_448() && item != Item.field_425 ? Kind.HANDHELD : Kind.GENERATED;
  }

  public static void firstPersonHand(int side, float swing, float equip) { firstPersonHand(GL, side, swing, equip); }
  static void firstPersonHand(Pose pose, int side, float swing, float equip) {
    float root = (float)Math.sin(Math.sqrt(swing) * Math.PI);
    pose.translate(side * (.56f - root * .4f),
        -.52f - (1 - equip) * .6f + (float)Math.sin(Math.sqrt(swing) * Math.PI * 2) * .2f,
        -.72f - (float)Math.sin(swing * Math.PI) * .2f);
    pose.rotate(side * (45 - 20 * (float)Math.sin(swing * swing * Math.PI)), 0, 1, 0);
    pose.rotate(side * -20 * root, 0, 0, 1); pose.rotate(-80 * root, 1, 0, 0);
    pose.rotate(side * -45, 0, 1, 0);
  }

  /** Call after the native arm's transform, matching ItemInHandLayer's adult hand frame. */
  public static void thirdPersonHand(int side) { thirdPersonHand(GL, side); }
  static void thirdPersonHand(Pose pose, int side) {
    pose.rotate(-90, 1, 0, 0); pose.rotate(180, 0, 1, 0);
    pose.translate(side / 16f, 2 / 16f, -10 / 16f);
  }

  static void display(Pose pose, Kind kind, boolean firstPerson, int side) {
    Display transform = firstPerson ? switch (kind) {
      case BLOCK -> side < 0 ? BLOCK_FIRST_LEFT : BLOCK_FIRST_RIGHT;
      case ROD -> side < 0 ? ROD_FIRST_LEFT : ROD_FIRST_RIGHT;
      case HANDHELD, BOW -> side < 0 ? HANDHELD_FIRST_LEFT : GENERATED_FIRST;
      case GENERATED -> GENERATED_FIRST;
    } : switch (kind) {
      case BLOCK -> BLOCK_THIRD;
      case ROD -> side < 0 ? ROD_THIRD_LEFT : ROD_THIRD_RIGHT;
      case BOW -> side < 0 ? BOW_THIRD_LEFT : BOW_THIRD_RIGHT;
      case HANDHELD -> side < 0 ? HANDHELD_THIRD_LEFT : HANDHELD_THIRD_RIGHT;
      case GENERATED -> GENERATED_THIRD;
    };
    transform.apply(pose, side);
  }

  static void normalizeNative(Pose pose, Kind kind) {
    if (kind == Kind.BLOCK) return; // Native block inventory geometry is already centered.
    // Native sprite U runs opposite model X. Modern handheld models use U as X;
    // turn the centered sprite to that face before applying their display frame.
    if (kind == Kind.HANDHELD || kind == Kind.BOW || kind == Kind.ROD) pose.rotate(180, 0, 1, 0);
    // Center the 0..1 sprite and its -1/16..0 extrusion, then undo class_556's baked hand pose.
    pose.translate(-.5f, -.5f, 1 / 32f);
    pose.translate(.9375f, .0625f, 0);
    pose.rotate(-335, 0, 0, 1); pose.rotate(-50, 0, 1, 0);
    pose.scale(1 / 1.5f, 1 / 1.5f, 1 / 1.5f); pose.translate(0, .3f, 0);
  }

  public record Tip(double x, double y, double z) {}
  /** The center of the native rod's top-right tip pixel, transformed by the actual hand/display frames. */
  public static Tip firstPersonRodTip(int side, float swing, float equip) {
    var pose = new TipPose();
    firstPersonHand(pose, side, swing, equip); display(pose, Kind.ROD, true, side);
    // Native texture pixel (14.5, 1.5), centered and turned to the modern rod face above.
    return pose.point(6.5 / 16, 6.5 / 16, -1 / 32.0);
  }
  private static final class TipPose implements Pose {
    private final double[] matrix = {1,0,0,0, 0,1,0,0, 0,0,1,0};
    public void translate(float x, float y, float z) {
      for (int row = 0; row < 3; row++) {
        int i = row * 4; matrix[i+3] += matrix[i]*x + matrix[i+1]*y + matrix[i+2]*z;
      }
    }
    public void scale(float x, float y, float z) {
      for (int row = 0; row < 3; row++) { int i = row*4; matrix[i] *= x; matrix[i+1] *= y; matrix[i+2] *= z; }
    }
    public void rotate(float angle, float x, float y, float z) {
      double c = Math.cos(Math.toRadians(angle)), s = Math.sin(Math.toRadians(angle));
      int a = x != 0 ? 1 : y != 0 ? 2 : 0, b = x != 0 ? 2 : y != 0 ? 0 : 1;
      for (int row = 0; row < 3; row++) {
        int i = row*4; double beforeA = matrix[i+a], beforeB = matrix[i+b];
        matrix[i+a] = beforeA*c + beforeB*s; matrix[i+b] = -beforeA*s + beforeB*c;
      }
    }
    Tip point(double x, double y, double z) {
      return new Tip(matrix[0]*x+matrix[1]*y+matrix[2]*z+matrix[3],
          matrix[4]*x+matrix[5]*y+matrix[6]*z+matrix[7], matrix[8]*x+matrix[9]*y+matrix[10]*z+matrix[11]);
    }
  }

  public static void render(PlayerEntity player, class_556 nativeRenderer, ItemStack stack,
      boolean firstPerson, int side) {
    var access = RendererAccess.INSTANCE;
    if (access.hasRenderer()) {
      var renderer = access.getRenderer().bakedModelRenderer();
      var model = renderer.getModel(stack, player.world, player, player.id);
      if (!(model instanceof VanillaBakedModel)) {
        var mode = firstPerson ? side < 0 ? ModelTransformation.Mode.FIRST_PERSON_LEFT_HAND : ModelTransformation.Mode.FIRST_PERSON_RIGHT_HAND
            : side < 0 ? ModelTransformation.Mode.THIRD_PERSON_LEFT_HAND : ModelTransformation.Mode.THIRD_PERSON_RIGHT_HAND;
        StationRenderAPI.getBakedModelManager().getAtlas(Atlases.GAME_ATLAS_TEXTURE).bindTexture();
        Tessellator.INSTANCE.startQuads();
        try { renderer.renderItem(stack, mode, player.method_1394(1), model); }
        finally { Tessellator.INSTANCE.draw(); }
        return;
      }
    }
    Kind kind = kind(stack);
    display(GL, kind, firstPerson, side); normalizeNative(GL, kind);
    if (!firstPerson && player.fishHook != null && stack.getItem() instanceof class_607)
      stack = new ItemStack(Item.field_377);
    nativeRenderer.method_1862(player, stack);
  }
}
