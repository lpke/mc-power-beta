package local.luke.power.bow;

import local.luke.power.inventory.HandUse;
import local.luke.power.offhand.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Official draw transforms and pull frames, using Beta's fixed-function item renderer. */
public final class ModernBowRenderer {
  private ModernBowRenderer() {}
  public static boolean firstPerson(Minecraft mc, class_556 renderer, float delta, float mainEquip) {
    if (!ModernBow.enabled() || mc.player == null) return false;
    HandUse.Hand hand=ModernBow.hand(mc.player);if(hand==null)return false;
    ItemStack stack=Offhand.stack(mc.player,hand);if(stack==null)return false;
    int side=hand==HandUse.Hand.MAIN?1:-1;
    float equip=side==1?mainEquip:OffhandRenderer.equip(mc.player,delta);
    float elapsed=Math.max(0,ModernBow.ticks(mc.player,stack)-1+delta),power=BowPhysics.power(elapsed);
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_LIGHTING_BIT|GL11.GL_TEXTURE_BIT|GL11.GL_POLYGON_BIT);
    GL11.glPushMatrix();
    try {
      GL11.glPushMatrix();
      GL11.glRotatef(mc.player.prevPitch+(mc.player.pitch-mc.player.prevPitch)*delta,1,0,0);
      GL11.glRotatef(mc.player.prevYaw+(mc.player.yaw-mc.player.prevYaw)*delta,0,1,0);
      class_583.method_1930();GL11.glPopMatrix();
      GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
      float light=mc.player.method_1394(delta);GL11.glColor4f(light,light,light,1);
      GL11.glTranslatef(side*.56f,-.52f-(1-equip)*.6f,-.72f);
      GL11.glTranslatef(side*-.2785682f,.18344387f,.15731531f);
      GL11.glRotatef(-13.935f,1,0,0);GL11.glRotatef(side*35.3f,0,1,0);GL11.glRotatef(side*-9.785f,0,0,1);
      if(power>.1f)GL11.glTranslatef(0,(float)Math.sin((elapsed-.1f)*1.3f)*(power-.1f)*.004f,0);
      GL11.glTranslatef(0,0,power*.04f);GL11.glScalef(1,1,1+power*.2f);GL11.glRotatef(-side*45,0,1,0);
      HeldItemTransforms.render(mc.player,renderer,stack,true,side);
    }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
    return true;
  }
  /** Centered generated-model geometry, with the same 1/16 extrusion as the native sprite. */
  public static void sprite(PlayerEntity player, ItemStack stack) {
    int texture=BowPhysics.texture(ModernBow.ticks(player,stack));
    Minecraft mc=(Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    mc.textureManager.bindTexture(mc.textureManager.getTextureId("/assets/powerbeta/textures/bow/pulling_"+texture+".png"));
    var t=Tessellator.INSTANCE;float d=1f/32;
    t.startQuads();
    t.normal(0,0,1);quad(t,-.5f,-.5f,d,.5f,.5f,d,0,1,1,0,false);
    t.normal(0,0,-1);quad(t,-.5f,-.5f,-d,.5f,.5f,-d,0,1,1,0,true);
    for(int i=0;i<16;i++){
      float a=i/16f-.5f,b=(i+1)/16f-.5f,u=(i+.5f)/16;
      t.normal(-1,0,0);t.vertex(a,-.5,d,u,1);t.vertex(a,.5,d,u,0);t.vertex(a,.5,-d,u,0);t.vertex(a,-.5,-d,u,1);
      t.normal(1,0,0);t.vertex(b,-.5,-d,u,1);t.vertex(b,.5,-d,u,0);t.vertex(b,.5,d,u,0);t.vertex(b,-.5,d,u,1);
      t.normal(0,-1,0);t.vertex(-.5,a,-d,0,1-u);t.vertex(.5,a,-d,1,1-u);t.vertex(.5,a,d,1,1-u);t.vertex(-.5,a,d,0,1-u);
      t.normal(0,1,0);t.vertex(-.5,b,d,0,1-u);t.vertex(.5,b,d,1,1-u);t.vertex(.5,b,-d,1,1-u);t.vertex(-.5,b,-d,0,1-u);
    }
    t.draw();
  }
  private static void quad(Tessellator t,float x0,float y0,float z0,float x1,float y1,float z1,float u0,float v0,float u1,float v1,boolean reverse){
    if(reverse){t.vertex(x0,y0,z0,u0,v0);t.vertex(x0,y1,z1,u0,v1);t.vertex(x1,y1,z1,u1,v1);t.vertex(x1,y0,z0,u1,v0);}
    else{t.vertex(x0,y0,z0,u0,v0);t.vertex(x1,y0,z0,u1,v0);t.vertex(x1,y1,z1,u1,v1);t.vertex(x0,y1,z1,u0,v1);}
  }
}
