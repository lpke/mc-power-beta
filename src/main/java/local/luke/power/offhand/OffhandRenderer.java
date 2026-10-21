package local.luke.power.offhand;

import com.matthewperiut.retroauth.skin.data.ModelPartData;
import com.matthewperiut.retroauth.skin.data.PlayerEntitySkinData;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Render state belongs to the physical player and never changes gameplay inventory. */
public final class OffhandRenderer {
  private static Object owner, world;
  private static ItemStack rendered;
  private static float equip, previousEquip, swing, previousSwing;
  private static int swingTicks;
  private static boolean changed;
  private static final ModelPart[][] arms = new ModelPart[3][];
  private static int skinTexture = -1, skinLayout;
  private OffhandRenderer() {}
  public static void changed() { changed = true; }
  private static void reset(Object nextOwner, Object nextWorld) {
    owner = nextOwner; world = nextWorld; rendered = null;
    equip = previousEquip = swing = previousSwing = 0; swingTicks = 0; changed = false;
  }
  public static void swing(PlayerEntity player) {
    if (owner != player || world != player.world) reset(player, player.world);
    swingTicks = 6; previousSwing = swing = 0;
  }
  public static float swing(PlayerEntity player, float delta) {
    if (owner != player || world != player.world) return 0;
    float difference = swing - previousSwing; if (difference < 0) difference++;
    return previousSwing + difference * delta;
  }
  public static float equip(PlayerEntity player, float delta) {
    return owner == player && world == player.world ? previousEquip + (equip - previousEquip) * delta : 0;
  }
  public static void tick(Minecraft mc) {
    if (owner != mc.player || world != mc.world) reset(mc.player, mc.world);
    previousEquip = equip; previousSwing = swing;
    if (swingTicks > 0) { swing = (6 - --swingTicks) / 6f; } else swing = 0;
    ItemStack actual = mc.player != null && Offhand.enabled(mc.player) ? Offhand.get(mc.player.inventory) : null;
    float target = !changed && rendered == actual ? 1 : 0;
    equip += Math.max(-.4f, Math.min(.4f, target - equip));
    if (equip < .1f) { rendered = actual; changed = false; }
  }
  public static boolean splitMap(Minecraft mc, ItemStack main) {
    return Offhand.playable(mc) && owner == mc.player && world == mc.world && rendered != null
        && main != null && main.itemId == Item.field_457.id;
  }
  public static void firstPerson(Minecraft mc, class_556 renderer, class_321 maps, float delta, ItemStack mainMap, float mainEquip) {
    if (!Offhand.playable(mc)) return;
    ItemStack off = owner == mc.player && world == mc.world ? rendered : null;
    if (off == null && mainMap == null) return;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_TEXTURE_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glPushMatrix();
      GL11.glRotatef(mc.player.prevPitch + (mc.player.pitch - mc.player.prevPitch) * delta, 1, 0, 0);
      GL11.glRotatef(mc.player.prevYaw + (mc.player.yaw - mc.player.prevYaw) * delta, 0, 1, 0);
      class_583.method_1930(); GL11.glPopMatrix();
      GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
      float brightness = mc.world.method_1782((int)Math.floor(mc.player.x), (int)Math.floor(mc.player.y), (int)Math.floor(mc.player.z));
      if (mainMap != null) draw(mc, renderer, maps, mainMap,
          1, mc.player.method_930(delta), mainEquip, brightness);
      if (off != null) draw(mc, renderer, maps, off, -1, swing(mc.player, delta),
          previousEquip + (equip - previousEquip) * delta, brightness);
    } finally { GL11.glPopMatrix(); GL11.glPopAttrib(); }
  }
  private static void draw(Minecraft mc, class_556 renderer, class_321 maps, ItemStack item,
      int side, float swing, float equip, float brightness) {
    if (item == null) return;
    GL11.glPushMatrix();
    try {
      if (item.itemId == Item.field_457.id) {
        GL11.glColor4f(brightness, brightness, brightness, 1);
        map(mc, maps, item, side, swing, equip);
        return;
      }
      HeldItemTransforms.firstPersonHand(side, swing, equip);
      int tint = Item.ITEMS[item.itemId].method_440(item.getDamage());
      GL11.glColor4f(brightness * (tint >> 16 & 255) / 255f,
          brightness * (tint >> 8 & 255) / 255f, brightness * (tint & 255) / 255f, 1);
      HeldItemTransforms.render(mc.player, renderer, item, true, side);
    } finally { GL11.glPopMatrix(); }
  }
  /** Modern one-handed map placement, with Beta's native map pixels and markers. */
  private static void map(Minecraft mc, class_321 maps, ItemStack item, int side, float swing, float equip) {
    float root = (float)Math.sin(Math.sqrt(swing) * Math.PI);
    float wave = (float)Math.sin(Math.sqrt(swing) * Math.PI * 2);
    float arc = (float)Math.sin(swing * Math.PI), lowered = 1 - equip;
    GL11.glTranslatef(side * .125f, -.125f, 0);
    GL11.glPushMatrix();
    try {
      GL11.glRotatef(side * 10, 0, 0, 1);
      GL11.glTranslatef(side * (.64f - root * .3f), -.6f + wave * .4f - lowered * .6f, -.72f - arc * .4f);
      GL11.glRotatef(side * 45, 0, 1, 0);
      GL11.glRotatef(side * root * 70, 0, 1, 0);
      GL11.glRotatef(side * -20 * (float)Math.sin(swing * swing * Math.PI), 0, 0, 1);
      GL11.glTranslatef(-side, 3.6f, 3.5f);
      GL11.glRotatef(side * 120, 0, 0, 1); GL11.glRotatef(200, 1, 0, 0);
      GL11.glRotatef(side * -135, 0, 1, 0); GL11.glTranslatef(side * 5.6f, 0, 0);
      arm(mc, side);
    } finally { GL11.glPopMatrix(); }
    GL11.glTranslatef(side * .51f, -.08f - lowered * 1.2f, -.75f);
    GL11.glTranslatef(side * root * -.5f, wave * .4f - root * .3f, arc * -.3f);
    GL11.glRotatef(root * -45, 1, 0, 0); GL11.glRotatef(side * root * -30, 0, 1, 0);
    // Native map quads face -Z; both rotations keep their front and text facing the camera.
    GL11.glRotatef(180, 0, 1, 0); GL11.glRotatef(180, 0, 0, 1);
    GL11.glScalef(.38f, .38f, .38f); GL11.glTranslatef(-.5f, -.5f, 0);
    GL11.glScalef(1f/128, 1f/128, 1f/128);
    mc.textureManager.bindTexture(mc.textureManager.getTextureId("/misc/mapbg.png"));
    var tessellator = net.minecraft.client.render.Tessellator.INSTANCE;
    GL11.glNormal3f(0,0,-1); tessellator.startQuads();
    tessellator.vertex(-7,135,0,0,1); tessellator.vertex(135,135,0,1,1);
    tessellator.vertex(135,-7,0,1,0); tessellator.vertex(-7,-7,0,0,0); tessellator.draw();
    maps.method_1046(mc.player, mc.textureManager, Item.field_457.method_1730(item, mc.world));
  }
  private static void arm(Minecraft mc, int side) {
    int texture = mc.textureManager.method_1093(mc.player.skinUrl, mc.player.method_1314());
    mc.textureManager.bindTexture(texture);
    if (skinTexture != texture) {
      int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH);
      int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT);
      skinLayout = h == w ? 1 : 0; skinTexture = texture;
    }
    boolean slim = skinLayout == 1 && mc.player instanceof PlayerEntitySkinData skin && "slim".equals(skin.getTextureModel());
    int layout = slim ? 2 : skinLayout;
    if (arms[layout] == null) {
      int width = slim ? 3 : 4;
      ModelPart left = new ModelPart(skinLayout == 1 ? 32 : 40, skinLayout == 1 ? 48 : 16);
      ModelPart right = new ModelPart(40,16);
      ((ModelPartData)(Object)left).setTextureHeight(skinLayout == 1 ? 64 : 32);
      ((ModelPartData)(Object)right).setTextureHeight(skinLayout == 1 ? 64 : 32);
      left.mirror = skinLayout == 0; left.addCuboid(-1,-2,-2,width,12,4,0); right.addCuboid(-width+1,-2,-2,width,12,4,0);
      ModelPart sleeve = new ModelPart(48,48), rightSleeve = new ModelPart(40,32);
      ((ModelPartData)(Object)sleeve).setTextureHeight(64); ((ModelPartData)(Object)rightSleeve).setTextureHeight(64);
      sleeve.addCuboid(-1,-2,-2,width,12,4,.25f); rightSleeve.addCuboid(-width+1,-2,-2,width,12,4,.25f);
      left.setPivot(5, 2, 0); right.setPivot(-5, 2, 0);
      sleeve.setPivot(5, 2, 0); rightSleeve.setPivot(-5, 2, 0);
      left.roll = sleeve.roll = -.1f; right.roll = rightSleeve.roll = .1f;
      arms[layout] = new ModelPart[]{left,right,sleeve,rightSleeve};
    }
    arms[layout][side < 0 ? 0 : 1].render(1f/16);
    if (skinLayout == 1) arms[layout][side < 0 ? 2 : 3].render(1f/16);
  }
}
