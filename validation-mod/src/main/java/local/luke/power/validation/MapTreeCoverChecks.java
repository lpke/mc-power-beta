package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.util.*;
import local.luke.power.map.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;

/** Native roof classification and layer sampling without rendering or loading terrain. */
final class MapTreeCoverChecks {
  private static void plane(Minecraft mc,int x,int z,int y,int id,int metadata) {
    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)
      mc.world.method_154(x+dx,y,z+dz,id,metadata);
  }

  static void run(Minecraft mc)throws Exception {
    check(mc.world.dimension.id!=-1,"tree-cover fixture requires the Overworld");
    int x=((int)Math.floor(mc.player.x)>>4)*16+4,z=((int)Math.floor(mc.player.z)>>4)*16+4;
    var chunk=MapSampler.loaded(mc.world,x>>4,z>>4);
    check(chunk!=null,"tree-cover fixture chunk is not loaded");
    var original=MapConfig.copy();boolean fancy=!Block.LEAVES.method_1620();
    var blocks=new ArrayList<int[]>();
    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int y=80;y<128;y++) {
      check(mc.world.method_1777(x+dx,y,z+dz)==null,"tree-cover fixture contains saved block-entity data");
      blocks.add(new int[]{x+dx,y,z+dz,mc.world.getBlockId(x+dx,y,z+dz),mc.world.method_1778(x+dx,y,z+dz)});
    }
    try {
      for(int y=80;y<128;y++)plane(mc,x,z,y,y==89?1:0,0);
      var settings=MapConfig.copy();settings.caveMode=1;settings.caveAbove=settings.caveBelow=10;
      settings.roofSize=3;MapConfig.preview(settings);
      test("automatic maps stay on the surface beneath all native tree leaves in fast and fancy graphics",()->{
        check(MapSampler.layer(mc,x+.5,90,z+.5)==-1,"open sky became a cave");
        for(boolean graphics:new boolean[]{false,true}) {
          Block.LEAVES.method_991(graphics);
          check(Block.LEAVES.method_1623(),"fixture no longer exercises full-cube native leaves");
          for(int variant=0;variant<3;variant++) {
            for(int y=108;y<=110;y++)plane(mc,x,z,y,18,variant);
            for(int width:new int[]{1,3,5}) {
              settings.roofSize=width;MapConfig.preview(settings);
              check(MapSampler.layer(mc,x+.5,90,z+.5)==-1,
                  "tree became cave: graphics="+graphics+" variant="+variant+" width="+width);
            }
          }
        }
        settings.roofSize=3;MapConfig.preview(settings);
        mc.world.method_154(x,108,z,17,0);
        check(MapSampler.layer(mc,x+.5,90,z+.5)==-1,"isolated tree branch completed a leaf roof");
        mc.world.method_154(x,108,z,18,0);
      });
      test("leaf cover never hides real stone or wooden roofs and explicit map modes retain control",()->{
        settings.roofSize=3;settings.caveMode=1;MapConfig.preview(settings);
        int expected=MapSampler.window(settings,90).layer();
        for(int height:new int[]{104,116})for(int block:new int[]{1,5,17}) {
          plane(mc,x,z,height,block,0);
          check(MapSampler.layer(mc,x+.5,90,z+.5)==expected,"solid roof lost beneath/above foliage");
          mc.world.method_154(x+1,height,z+1,0,0);
          check(MapSampler.layer(mc,x+.5,90,z+.5)==-1,"leaves filled a missing solid-roof column");
          plane(mc,x,z,height,0,0);
        }
        settings.caveMode=2;settings.caveTop=72;MapConfig.preview(settings);
        check(MapSampler.layer(mc,x+.5,90,z+.5)==9,"manual cave layer followed tree cover");
        plane(mc,x,z,116,1,0);settings.caveMode=0;MapConfig.preview(settings);
        check(MapSampler.layer(mc,x+.5,90,z+.5)==-1,"forced surface entered cave under stone");
        plane(mc,x,z,116,0,0);settings.caveMode=1;MapConfig.preview(settings);
      });
      test("native surface/cave/surface transitions keep canopy and cave colors in separate tiles",()->{
        var sampler=new MapSampler(new MapPalette());
        var sample=MapSampler.class.getDeclaredMethod("sample",net.minecraft.world.World.class,
            net.minecraft.class_43.class,MapTile.class,int.class,int.class,CaveWindow.class);
        sample.setAccessible(true);var window=MapSampler.window(settings,90);
        var surface=new MapTile(MapMath.key(mc.world.dimension.id,-1,0,x,z));
        var cave=new MapTile(MapMath.key(mc.world.dimension.id,window.layer(),0,x,z,window.context()));
        sample.invoke(sampler,mc.world,chunk,surface,x,z,null);int index=MapMath.index(x,z);
        check(surface.block(index)==18&&surface.height(index)==110,"tree canopy disappeared from surface sampling");
        byte[] surfaceBefore=MapCodec.encode(surface);
        plane(mc,x,z,116,1,0);
        check(MapSampler.layer(mc,x+.5,90,z+.5)==window.layer(),"cave transition missing");
        sample.invoke(sampler,mc.world,chunk,cave,x,z,window);
        check(cave.block(index)==1&&cave.height(index)==89,"true cave floor lost");
        check(Arrays.equals(surfaceBefore,MapCodec.encode(surface)),"cave sampling overwrote surface tile");
        plane(mc,x,z,116,0,0);
        check(MapSampler.layer(mc,x+.5,90,z+.5)==-1,"removed roof retained cave mode under trees");
        sample.invoke(sampler,mc.world,chunk,surface,x,z,null);
        check(surface.block(index)==18&&cave.block(index)==1,"returning to canopy reused cave terrain");
        settings.heightShading=settings.slopeShading=false;settings.caveLighting=true;
        int surfaceColor=surface.color(index,settings),caveColor=cave.color(index,settings);
        settings.caveLighting=false;
        check(surface.color(index,settings)==surfaceColor,"cave lighting darkened outdoor tree terrain");
        check(cave.color(index,settings)!=caveColor,"cave lighting stopped shading the real cave");
      });
      test("automatic roof classification never requests unloaded chunks",()->{
        boolean checking=Release252Checks.checkingUnloaded;int requests=Release252Checks.chunkRequests;
        Release252Checks.chunkRequests=0;Release252Checks.checkingUnloaded=true;
        try {
          check(MapSampler.layer(mc,31_999_900,90,31_999_900)==-1,"unloaded terrain became a cave");
          check(Release252Checks.chunkRequests==0,"roof classification requested a native chunk");
        } finally {Release252Checks.checkingUnloaded=checking;Release252Checks.chunkRequests=requests;}
      });
    } finally {
      Block.LEAVES.method_991(fancy);MapConfig.preview(original);
      for(int[] block:blocks)mc.world.method_154(block[0],block[1],block[2],block[3],block[4]);
    }
  }
}
