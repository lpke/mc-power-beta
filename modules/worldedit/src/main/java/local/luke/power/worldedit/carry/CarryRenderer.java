package local.luke.power.worldedit.carry;

import net.minecraft.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import org.lwjgl.opengl.GL11;

/** BTA's centered carry transform and bob, rendered with Beta's chest textures. */
public final class CarryRenderer implements BlockView {
  private static final CarryRenderer VIEW = new CarryRenderer();
  private static final class_13 RENDERER = new class_13();
  private int block, count, metadata;

  public static void firstPerson(Minecraft mc, float delta) {
    GL11.glPushMatrix();
    try {
      float swing = mc.player.method_930(delta);
      GL11.glTranslatef(0, (float) Math.sin(swing * Math.PI * 2 - Math.PI / 2) / 33, 0);
      GL11.glScalef(1, -1, 1);
      GL11.glRotatef(-(float) Math.sin(swing * Math.PI * 2) * 5, 1, 0, 0);
      draw(mc, mc.player, delta);
    } finally {
      GL11.glPopMatrix();
    }
  }

  public static void thirdPerson(Minecraft mc, PlayerEntity player, float delta) {
    GL11.glPushMatrix();
    try {
      float speed = player.field_1048 + (player.field_1049 - player.field_1048) * delta;
      float progress = player.field_1050 - player.field_1049 * (1 - delta);
      GL11.glTranslatef(0, (float) Math.cos(progress / 3) / 26 * speed + .1f, 0);
      draw(mc, player, delta);
    } finally {
      GL11.glPopMatrix();
    }
  }

  private static void draw(Minecraft mc, PlayerEntity player, float delta) {
    VIEW.block = ContainerCarry.carriedBlock();
    VIEW.count = ContainerCarry.carriedCount();
    VIEW.metadata = ContainerCarry.carriedMetadata();
    if (VIEW.block == 0) return;
    GL11.glPushAttrib(
        GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, mc.textureManager.getTextureId("/terrain.png"));
      GL11.glScalef(.55f, -.55f, .55f);
      GL11.glTranslatef(-VIEW.count * .5f, -1.25f, -1.25f);
      Block block = Block.BLOCKS[VIEW.block];
      float light = player.method_1394(delta);
      for (int x = 0; x < VIEW.count; x++) {
        for (int face = 0; face < 6; face++) {
          if (face == 4 && x > 0 || face == 5 && x < VIEW.count - 1) continue;
          int texture = block.method_1626(VIEW, x, 0, 0, face);
          Tessellator t = Tessellator.INSTANCE;
          t.startQuads();
          float shade = face == 0 ? .5f : face == 1 ? 1 : face < 4 ? .8f : .6f;
          t.color(light * shade, light * shade, light * shade);
          switch (face) {
            case 0 -> {
              t.normal(0, -1, 0);
              RENDERER.method_46(block, x, 0, 0, texture);
            }
            case 1 -> {
              t.normal(0, 1, 0);
              RENDERER.method_55(block, x, 0, 0, texture);
            }
            case 2 -> {
              t.normal(0, 0, -1);
              RENDERER.method_61(block, x, 0, 0, texture);
            }
            case 3 -> {
              t.normal(0, 0, 1);
              RENDERER.method_65(block, x, 0, 0, texture);
            }
            case 4 -> {
              t.normal(-1, 0, 0);
              RENDERER.method_67(block, x, 0, 0, texture);
            }
            case 5 -> {
              t.normal(1, 0, 0);
              RENDERER.method_69(block, x, 0, 0, texture);
            }
          }
          t.draw();
        }
      }
    } finally {
      GL11.glPopMatrix();
      GL11.glPopAttrib();
    }
  }

  public int getBlockId(int x, int y, int z) {
    return x >= 0 && x < count && y == 0 && z == 0 ? block : 0;
  }

  public int method_1778(int x, int y, int z) {
    return getBlockId(x, y, z) == 0 ? 0 : metadata;
  }

  public BlockEntity method_1777(int x, int y, int z) {
    return null;
  }

  public float method_1784(int x, int y, int z, int light) {
    return 1;
  }

  public float method_1782(int x, int y, int z) {
    return 1;
  }

  public Material method_1779(int x, int y, int z) {
    return getBlockId(x, y, z) == 0 ? Material.AIR : Block.BLOCKS[block].field_1900;
  }

  public boolean method_1783(int x, int y, int z) {
    return getBlockId(x, y, z) != 0;
  }

  public boolean method_1780(int x, int y, int z) {
    return getBlockId(x, y, z) != 0;
  }

  public class_519 method_1781() {
    return null;
  }
}
