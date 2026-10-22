package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.input.RenderedView;
import local.luke.power.render.*;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_56;
import net.minecraft.class_66;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Reads the real terrain VBO. Runs only in a disposable world on the render thread. */
final class WaterTerrainChecks {
  static void run(Minecraft mc) throws Exception {
    failures=0;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);FreecamPreviewChecks.active(false);
    var original=VisualConfig.copy();int sky=mc.world.field_202;
    RenderControls.reset(mc);
    int lists=GL11.glGenLists(3);
    class_66 section=new class_66(mc.world,new ArrayList<>(),32,96,32,16,lists);
    try {
      mc.world.field_202=0;
      var s=VisualConfig.copy();s.underwaterDarkness=0;s.underwaterDarknessOutside=100;VisualConfig.preview(s);
      // Open daylight above water, with visible floor/wall faces and known native darkness.
      for(int x=31;x<=48;x++)for(int z=31;z<=48;z++)for(int y=94;y<128;y++) {
        mc.world.method_154(x,y,z,y==96?1:y<=109?9:0,0);
        mc.world.method_205(class_56.SKY,x,y,z,y>109?15:0);
        mc.world.method_205(class_56.BLOCK,x,y,z,0);
      }
      mc.player.method_1340(40.5,105.62,40.5);mc.player.prevY=mc.player.y;
      mc.field_2807=mc.player;RenderedView.clear();CameraEnvironment.updateLighting(mc);
      test("terrain retains native geometry and both light colors in a single VBO",()->{
        long before=WaterTerrain.secondPasses();build(section);
        check(WaterTerrain.secondPasses()==before+1,"water section did not calculate both endpoints");
        check(section.field_223,"day/night changes would leave compensated deep water stale");
        check(WaterTerrain.mismatches()==0,"native geometry changed between lighting calculations");
        int changed=0, vertices=0;
        for(int pass=0;pass<2;pass++)if(!section.field_244[pass]) {
          ByteBuffer data=read(section,pass);vertices+=data.limit()/32;
          for(int p=0;p<data.limit();p+=32) {
            check((data.get(p+23)&255)==255,"native alpha changed");
            for(int c=0;c<3;c++) if(water(data,p,c)>(data.get(p+20+c)&255))changed++;
          }
        }
        check(changed>0,"GPU has no brighter water endpoint");
        log("WATER VBO vertices="+vertices+" bytes="+(vertices*32)+" nativeBytes="+(vertices*28)+" changedChannels="+changed);
      });
      test("1000 actual water crossings and slider previews queue no terrain work",()->{
        Object meshes=field(mc.worldRenderer,"field_1809");
        var pending=(java.util.List<?>)field(mc.worldRenderer,"field_1807");int count=pending.size();
        long builds=WaterTerrain.sections();int nativeBuilds=class_66.field_230;
        ByteBuffer before=read(section,0);long start=System.nanoTime();
        for(int i=0;i<1000;i++) {
          mc.player.y=(i&1)==0?105.62:115.62;RenderedView.clear();CameraEnvironment.updateLighting(mc);
          check(CameraEnvironment.darkness()==((i&1)==0?0:100),"wrong camera darkness");
        }
        log("WATER 1000 crossings ms="+(System.nanoTime()-start)/1_000_000.0);
        var session=SettingsRegistry.open(mc);
        try {
          for(int value:new int[]{100,75,50,25,0}) {
            find(session,"visual.underwaterDarknessOutside").parse(""+value);session.preview(true);
            check(CameraEnvironment.darkness()==value,"slider preview stale");
            check(pending.size()==count,"slider queued terrain");
          }
        } finally {session.discard();}
        check(CameraEnvironment.darkness()==100,"Cancel did not restore uniform value");
        check(field(mc.worldRenderer,"field_1809")==meshes && pending.size()==count,"crossing recreated/queued meshes");
        check(WaterTerrain.sections()==builds && class_66.field_230==nativeBuilds,"crossing rebuilt terrain");
        check(before.equals(read(section,0)),"crossing uploaded different GPU data");
      });
      test("shader uses the selected percentage and keeps attribute state isolated",()->{
        Object shader=Class.forName("net.mine_diver.smoothbeta.client.render.Shaders").getMethod("getTerrainShader").invoke(null);
        int program=(int)shader.getClass().getMethod("getProgramRef").invoke(shader);
        check(GL20.glGetAttribLocation(program,"PowerWaterColor")==5,"extra color attribute not bound");
        int location=GL20.glGetUniformLocation(program,"PowerWaterBrightness");check(location>=0,"water uniform absent");
        for(int value:new int[]{100,75,50,25,0}) {
          s.underwaterDarknessOutside=value;VisualConfig.preview(s);CameraEnvironment.updateLighting(mc);
          shader.getClass().getMethod("bind").invoke(shader);
          var out=BufferUtils.createFloatBuffer(1);GL20.glGetUniform(program,location,out);
          check(Math.abs(out.get(0)-(100-value)/100F)<.00001,"wrong shader percentage");
          shader.getClass().getMethod("unbind").invoke(shader);
        }
        var enabled=BufferUtils.createIntBuffer(4);
        GL20.glGetVertexAttrib(5,GL20.GL_VERTEX_ATTRIB_ARRAY_ENABLED,enabled);
        check(enabled.get(0)==0,"water color attribute leaked out of terrain rendering");
      });
      test("night, roof and terrain edits rebuild endpoints together",()->{
        mc.world.field_202=11;build(section);ByteBuffer night=read(section,0);
        for(int p=0;p<night.limit();p+=32)check(water(night,p,0)<200,"night turned into daylight");
        for(int x=31;x<=48;x++)for(int z=31;z<=48;z++) {
          mc.world.method_154(x,110,z,1,0);
          for(int y=94;y<=110;y++)mc.world.method_205(class_56.SKY,x,y,z,0);
        }
        mc.world.field_202=0;long before=WaterTerrain.secondPasses();build(section);
        check(WaterTerrain.secondPasses()==before,"roofed section unnecessarily calculated a second light color");
        ByteBuffer roof=read(section,0);
        for(int p=0;p<roof.limit();p+=32)for(int c=0;c<3;c++)check((roof.get(p+20+c)&255)==water(roof,p,c),"roof brightened");
        mc.world.method_154(36,96,36,0,0);build(section);
        check(!roof.equals(read(section,0)),"block edit left stale geometry");
      });
      test("torch-lit deep water built at night still follows dawn without extra night work",()->{
        for(int x=31;x<=48;x++)for(int z=31;z<=48;z++)for(int y=78;y<128;y++) {
          mc.world.method_154(x,y,z,y==80?1:y<=109?9:0,0);
          mc.world.method_205(class_56.SKY,x,y,z,y>109?15:0);
          mc.world.method_205(class_56.BLOCK,x,y,z,y<=109?14:0);
        }
        section.method_298(32,80,32);mc.world.field_202=11;
        long passes=WaterTerrain.secondPasses();build(section);
        check(WaterTerrain.secondPasses()==passes,"torch-lit night section did unnecessary second-pass work");
        check(section.field_223,"torchlight masked the section's dependency on daylight");
        mc.world.field_202=0;build(section);
        check(WaterTerrain.secondPasses()==passes+1,"daylight did not recompute water compensation");
      });
      test("first opt-in prepares sections without native skylight once",()->{
        s.underwaterDarkness=s.underwaterDarknessOutside=100;VisualConfig.preview(s);
        WaterTerrain.reset();CameraEnvironment.updateLighting(mc);
        var meshes=(class_66[])field(mc.worldRenderer,"field_1809");
        var pending=(java.util.List<?>)field(mc.worldRenderer,"field_1807");
        class_66 deep=meshes[0];boolean oldSky=deep.field_223;
        pending.remove(deep);deep.field_249=false;deep.field_223=false;
        s.underwaterDarkness=0;VisualConfig.preview(s);CameraEnvironment.updateLighting(mc);
        check(deep.field_249 && pending.contains(deep),"first opt-in missed an unlit section");
        int count=pending.size();CameraEnvironment.updateLighting(mc);
        check(pending.size()==count,"first opt-in repeated queue entries");deep.field_223=oldSky;
      });
      test("roof changes notify water-dependent meshes below zero native skylight",()->{
        var meshes=(class_66[])field(mc.worldRenderer,"field_1809");
        var pending=(java.util.List<?>)field(mc.worldRenderer,"field_1807");
        class_66 deep=java.util.Arrays.stream(meshes).filter(m->m.field_231==32&&m.field_232==80&&m.field_233==32).findFirst().orElseThrow();
        build(deep);check(((WaterTerrainSection)deep).power$waterColumn(),"missing column dependency");
        pending.remove(deep);deep.field_249=false;
        mc.world.method_201(36,120,36,1,0);
        check(deep.field_249&&pending.contains(deep),"new roof did not invalidate deep water");
        build(deep);pending.remove(deep);deep.field_249=false;
        mc.world.method_201(36,120,36,0,0);
        check(deep.field_249&&pending.contains(deep),"removed roof did not invalidate deep water");
        int count=pending.size();long start=System.nanoTime();
        for(int i=0;i<10000;i++)mc.worldRenderer.method_1149(36,120,36);
        log("WATER 10000 column notifications ms="+(System.nanoTime()-start)/1_000_000.0);
        check(pending.size()==count,"column invalidation duplicated work");
      });
      check(GL11.glGetError()==0,"OpenGL error after terrain checks");
    } finally {
      // Release the fixture's VBO ranges without touching live world-renderer sections.
      for(int pass=0;pass<2;pass++) {
        Object buffer=section.getClass().getMethod("smoothbeta_getBuffer",int.class).invoke(section,pass);
        buffer.getClass().getMethod("upload",ByteBuffer.class).invoke(buffer,BufferUtils.createByteBuffer(0));
      }
      section.method_302();GL11.glDeleteLists(lists,3);mc.world.field_202=sky;VisualConfig.preview(original);RenderedView.clear();CameraEnvironment.updateLighting(mc);
    }
    log("WATER TERRAIN FAILURES "+failures);
  }

  private static int water(ByteBuffer data,int vertex,int channel) {
    return data.get(vertex+((data.get(vertex+27)&128)!=0?28:20)+channel)&255;
  }
  private static void build(class_66 section) {
    section.field_249=true;RenderControls.beginRender();
    try {section.method_296();}finally{RenderControls.endRender();}
  }
  static ByteBuffer read(class_66 section,int pass) throws Exception {
    Object buffer=section.getClass().getMethod("smoothbeta_getBuffer",int.class).invoke(section,pass);
    Object pool=field(buffer,"pool"),pos=field(buffer,"poolPos");
    check((int)field(pool,"vertexBytes")==32,"pool stride differs");
    int vertices=(int)pos.getClass().getMethod("getSize").invoke(pos), start=(int)pos.getClass().getMethod("getPosition").invoke(pos);
    var data=BufferUtils.createByteBuffer(vertices*32);int old=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    try {GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,(int)field(pool,"vertexBufferId"));if(vertices>0)GL15.glGetBufferSubData(GL15.GL_ARRAY_BUFFER,(long)start*32,data);}
    finally {GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,old);}
    return data;
  }
}
