package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.rules.GameRule;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.chat.CommandCatalog;
import local.luke.power.worldedit.core.*;
import net.minecraft.class_104;
import net.minecraft.class_206;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.Display;

/** Native spawner payloads and isolated command history in owned, already-loaded air cells. */
final class Spawner471Checks {
  private Spawner471Checks() {}
  static void run(Minecraft mc)throws Exception {
    check(mc.world!=null && !mc.world.isRemote && mc.player!=null && Display.isActive()
        && mc.options.soundVolume==0 && mc.options.musicVolume==0 && !WorldEditor.editing(),
        "focus a muted disposable singleplayer world with no pending edit");
    int x=((int)Math.floor(mc.player.x)>>4)*16+6,z=((int)Math.floor(mc.player.z)>>4)*16+6;
    Pos first=new Pos(x,112,z),second=first.add(1,0,0);
    check(mc.world.method_155(x-2,0,z-2,x+4,127,z+2),"spawner fixture requires already loaded neighboring terrain");
    for(Pos pos:List.of(first,second))check(mc.world.getBlockId(pos.x(),pos.y(),pos.z())==0
        && mc.world.method_1777(pos.x(),pos.y(),pos.z())==null,"spawner fixture cells must be air");
    var world=new MinecraftWorld(mc.world);var originalFirst=world.get(first);var originalSecond=world.get(second);
    var rules=AccessRules.copy(mc);var screen=mc.currentScreen;
    var editorField=WorldEditor.class.getDeclaredField("editor");editorField.setAccessible(true);Object oldEditor=editorField.get(null);
    var worldField=WorldEditor.class.getDeclaredField("world");worldField.setAccessible(true);Object oldWorld=worldField.get(null);
    var editor=new Editor(world,new BlockParser(id->id>=0 && id<Block.BLOCKS.length && (id==0 || Block.BLOCKS[id]!=null)),line->mc.inGameHud.addChatMessage(line));
    failures=0;
    try {
      editorField.set(null,editor);worldField.set(null,mc.world);mc.setScreen(null);AccessRules.set(mc,GameRule.ALLOW_WORLD_EDITING,true);
      editor.select(Region.between(first,second));
      test("SPAWNER471 supported values resolve to native living mobs and autocomplete",()->{
        for(String name:SpawnerMobs.names())check(class_206.method_732(SpawnerMobs.registryName(name),mc.world) instanceof LivingEntity,
            "spawner value is not a registered native living mob: "+name);
        check(CommandCatalog.complete("//set spawner[mob=zo").contains("//set spawner[mob=zombie]"),"missing mob completion");
        check(CommandCatalog.complete("//set spawner[mob=minecraft:sq").contains("//set spawner[mob=minecraft:squid]"),"missing namespace completion");
      });
      test("SPAWNER471 complete native payload survives set, undo and redo",()->{
        var cow=editor.blocks.block("spawner[mob=cow]");world.set(first,world.prepare(first,cow,world.get(first)));
        var before=(class_104)mc.world.method_1777(first.x(),first.y(),first.z());before.field_2681=321;before.markDirty();
        BlockValue previous=world.get(first);
        command(mc,editor,"//set spawner[mob=zombie]");
        expect(mc,first,"Zombie",321);expect(mc,second,"Zombie",20);
        BlockValue afterFirst=world.get(first),afterSecond=world.get(second);
        command(mc,editor,"//undo");check(previous.same(world.get(first)) && originalSecond.same(world.get(second)),"undo changed native spawner data");
        command(mc,editor,"//redo");check(afterFirst.same(world.get(first)) && afterSecond.same(world.get(second)),"redo changed native spawner data");
        command(mc,editor,"//replace spawner[mob=zombie] spawner[mob=minecraft:skeleton]");
        expect(mc,first,"Skeleton",321);expect(mc,second,"Skeleton",20);
        command(mc,editor,"//undo");check(afterFirst.same(world.get(first)) && afterSecond.same(world.get(second)),"mob-mask undo lost complete data");
        var override=BlockStates.apply(afterFirst,Map.of("mob","pig"));
        editor.engine.submit(List.of(first),pos->override);drain(editor);
        expect(mc,first,"Pig",321);
        command(mc,editor,"//undo");check(afterFirst.same(world.get(first)),"valued-pattern mob override lost copied payload/history");
      });
      test("SPAWNER471 invalid names and mixed patterns fail atomically",()->{
        BlockValue beforeFirst=world.get(first),beforeSecond=world.get(second);int history=editor.engine.undoSize();
        for(String pattern:List.of("spawner[mob=all]","spawner[mob=player]","spawner[mob=@e]","spawner[mob=boat]",
            "spawner[mob=arrow]","spawner[mob=creaking]","spawner[mob=cow],spawner[mob=boat]")) {
          var response=ChatChecks.record(()->check(WorldEditor.command(mc,"//set "+pattern),"command was not owned"));
          check(response.stream().anyMatch(line->line.contains("Unsupported Beta spawner mob")),"missing invalid-mob feedback: "+response);
          check(!editor.engine.busy() && editor.engine.undoSize()==history
              && beforeFirst.same(world.get(first)) && beforeSecond.same(world.get(second)),"invalid argument changed world/history: "+pattern);
        }
      });
      test("SPAWNER471 world-edit permission remains authoritative",()->{
        BlockValue beforeFirst=world.get(first),beforeSecond=world.get(second);int history=editor.engine.undoSize();
        AccessRules.set(mc,GameRule.ALLOW_WORLD_EDITING,false);
        try {
          var response=ChatChecks.record(()->WorldEditor.command(mc,"//set spawner[mob=pig]"));
          check(response.stream().anyMatch(line->line.contains("allow_world_editing")),"missing permission denial");
          check(!editor.engine.busy() && editor.engine.undoSize()==history
              && beforeFirst.same(world.get(first)) && beforeSecond.same(world.get(second)),"denied spawner command edited the world");
        } finally {AccessRules.set(mc,GameRule.ALLOW_WORLD_EDITING,true);}
      });
    } finally {
      editor.engine.finishRollback();world.set(first,originalFirst);world.set(second,originalSecond);
      editorField.set(null,oldEditor);worldField.set(null,oldWorld);AccessRules.restore(mc,rules);mc.setScreen(screen);
    }
    log("SPAWNER471 checks complete failures="+failures);check(failures==0,"Spawner471 failures="+failures);
  }
  private static void command(Minecraft mc,Editor editor,String text) {
    check(WorldEditor.command(mc,text),"command was not owned");drain(editor);
  }
  private static void drain(Editor editor) {
    int ticks=0;
    while(editor.engine.busy() && ticks++<100)editor.engine.tick(1000);
    check(!editor.engine.busy(),"spawner edit did not finish");
  }
  private static void expect(Minecraft mc,Pos pos,String mob,int delay) {
    check(mc.world.getBlockId(pos.x(),pos.y(),pos.z())==52,"missing spawner at "+pos);
    check(mc.world.method_1777(pos.x(),pos.y(),pos.z()) instanceof class_104,"missing native spawner entity");
    var spawner=(class_104)mc.world.method_1777(pos.x(),pos.y(),pos.z());
    check(spawner.method_2037().equals(mob) && spawner.field_2681==delay,"wrong mob/delay at "+pos);
  }
}
