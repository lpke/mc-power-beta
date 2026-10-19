package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.PlayerMode;
import local.luke.power.permissions.*;
import local.luke.power.render.*;
import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

final class Release150Checks {
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("spawn-dimension")) {
      (AccessRules.of(mc.world.method_262())).allAllowed(true);
      CreativeVehicleChecks.mode(mc,"CREATIVE");
      var source=new local.luke.power.commands.util.SharedCommandSource(mc);
      check(local.luke.power.commands.command.vanilla.Teleport.switchDimensions(source,"minecraft:the_nether"),"Nether entry failed");
      check(mc.player.dimensionId==-1,"not in Nether");
      new local.luke.power.commands.command.vanilla.Spawn().command(new local.luke.power.commands.util.SharedCommandSource(mc),new String[]{"spawn"});
      var pos=mc.world.getSpawnPos();
      check(mc.player.dimensionId==0 && mc.player.x==pos.x+.5 && mc.player.z==pos.z+.5,"spawn did not return from Nether");
      log("PASS /spawn returns from Nether to Overworld spawn");return;
    }
    if (action.equals("saved")) {
      var s = GameplayConfig.current();
      check(s.limitTnt && s.mergeItems && !local.luke.power.rules.WorldRules.of(mc.world.method_262()).enabled(local.luke.power.rules.GameRule.EXPLOSIONS_DESTROY_ITEMS) && s.tntPerTick == 2, "new settings lost at restart");
      log("PASS release 1.5 settings survive restart"); return;
    }
    if (action.equals("save")) {
      var s = GameplayConfig.copy(); s.limitTnt = s.mergeItems = true; AccessRules.set(mc, local.luke.power.rules.GameRule.EXPLOSIONS_DESTROY_ITEMS, false); s.tntPerTick = 2;
      GameplayConfig.save(s); return;
    }
    failures = 0;
    var original = GameplayConfig.copy(); var visual = VisualConfig.copy();
    var world = AccessRules.of(mc.world.method_262()); boolean cheats = world.allAllowed();
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    var inventory = Arrays.stream(mc.player.inventory.main).map(s -> ItemStack.clone(s)).toArray(ItemStack[]::new);
    var cursor = ItemStack.clone(mc.player.inventory.getCursorStack());
    double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
    List<Entity> created = new ArrayList<>();
    try {
      mc.setScreen(null); world.allAllowed(true); CreativeVehicleChecks.mode(mc, "CREATIVE");
      mc.player.method_1340(0, 110, 0); mc.world.method_214(0, 0);
      test("new defaults and settings transactions", () -> {
        var defaults = new GameplaySettings(); defaults.validate();
          check(defaults.mergeItems && !defaults.limitTnt && defaults.leavesDropSaplingsInCreative, "gameplay defaults changed");
        var session = SettingsRegistry.open(mc);
        var setting = session.settings().stream().filter(s -> s.id.equals("gameplay.limitTnt")).findFirst().orElseThrow();
        setting.value = new com.google.gson.JsonPrimitive(!original.limitTnt);
        session.preview(); check(GameplayConfig.current().limitTnt == original.limitTnt, "preview changed world mechanics");
        session.discard(); check(GameplayConfig.current().limitTnt == original.limitTnt, "cancel changed mechanics");
        check(session.settings().stream().filter(s -> s.id.startsWith("death.")).allMatch(s -> s.page.equals("Gameplay")), "death options not moved");
        for (String id : List.of("keys.power_beta.entities", "keys.power_beta.fullBright")) {
          var key = session.settings().stream().filter(s -> s.id.equals(id)).findFirst().orElseThrow();
          check(key.defaultValue.getAsInt() == 0, "new key not unbound");
        }
      });
      test("item merging conserves stacks and distinguishes damage and native NBT", () -> {
        var s = GameplayConfig.copy(); s.mergeItems = true; s.mergeThreshold = 16; GameplayConfig.preview(s);
        for (int i = 0; i < 80; i++) created.add(item(mc, i < 64 ? 4 : 5, 1, i >= 72 ? 1 : 0, 2 + i % 2 * .1, 110, 2));
        var special = item(mc, 4, 7, 0, 2,110,2); created.add(special);
        DeathChecks.tag(special.field_564).putString("test", "keep");
        int total = count(created); int before = alive(created);
        s.mergeItems = false; GameplayConfig.preview(s); check(ItemMerging.merge(mc.world, mc) == 0 && alive(created) == before, "off state merged");
        s.mergeItems = true; s.mergeThreshold = 200; GameplayConfig.preview(s); check(ItemMerging.merge(mc.world, mc) == 0, "threshold ignored");
        s.mergeThreshold = 16; GameplayConfig.preview(s);
        for (int i = 0; i < 6; i++) ItemMerging.merge(mc.world, mc);
        check(count(created) == total && alive(created) < before / 2, "merge lost counts or failed: " + alive(created));
        check(!special.dead && special.field_564.count == 7 && DeathChecks.tag(special.field_564).getString("test").equals("keep"), "NBT merged into plain item");
        for (Entity e : created) if (e instanceof class_142 it && !e.dead) check(it.field_564.count <= it.field_564.getMaxCount(), "overfull item");
        mc.world.isRemote = true;
        try { check(ItemMerging.merge(mc.world, mc) == 0, "client changed server items"); }
        finally { mc.world.isRemote = false; }
        created.forEach(Entity::markDead); created.clear();
      });
      test("explosion immunity is scoped and zero-distance impulses remain finite", () -> {
        var s = GameplayConfig.copy(); AccessRules.set(mc, local.luke.power.rules.GameRule.EXPLOSIONS_DESTROY_ITEMS, false); GameplayConfig.preview(s);
        var protectedItem = item(mc,4,64,0,20,110,20); created.add(protectedItem);
        mc.world.method_187(null,20,110,20,4);
        check(!protectedItem.dead && protectedItem.field_564.count == 64, "explosion deleted protected stack");
        check(Double.isFinite(protectedItem.velocityX) && Double.isFinite(protectedItem.velocityY), "center blast generated NaN");
        protectedItem.damage(null,10); check(protectedItem.dead, "ordinary damage was blocked");
        AccessRules.set(mc, local.luke.power.rules.GameRule.EXPLOSIONS_DESTROY_ITEMS, true); GameplayConfig.preview(s);
        var normal = item(mc,4,1,0,20.5,110,20); created.add(normal);
        mc.world.method_187(null,20,110,20,4); check(normal.dead,"off state protected item");
      });
      test("creative TNT has no drop and survival keeps the drop", () -> {
        for (boolean creative : new boolean[]{true,false,true,false}) {
          CreativeVehicleChecks.mode(mc,creative ? "CREATIVE" : "SURVIVAL");
          mc.world.method_201(7,110,7,46,0);
          int drops = itemCount(mc);
          mc.interactionManager.method_1716(7,110,7,1);
          check(itemCount(mc) == drops + (creative ? 0 : 1), "incorrect TNT drop in " + creative);
        }
      });
      test("normal and fast leaf decay restore survival drops after creative", () -> {
        var s=GameplayConfig.copy(); s.leavesDropSaplingsInCreative=false; GameplayConfig.preview(s);
        var features=Class.forName("local.luke.power.controls.ControlFeatures").getField("FEATURES_CONFIG").get(null);
        var fast=features.getClass().getField("fastLeafDecay").get(features);
        var enabled=fast.getClass().getField("enableFastLeafDecay");
        boolean before=(Boolean)enabled.get(fast);
        try {
          for(boolean accelerate:new boolean[]{false,true}) {
            enabled.set(fast,accelerate);
            for(boolean creative:new boolean[]{true,false,true,false}) {
              CreativeVehicleChecks.mode(mc,creative ? "CREATIVE" : "SURVIVAL");
              int drops=itemCount(mc);
              // A deterministic drop RNG checks the actual decay path, including scheduled decay.
              mc.world.field_214.setSeed(0);
              for(int i=0;i<128;i++) {
                mc.world.method_201(8,112,8,18,8);
                Block.LEAVES.method_1602(mc.world,8,112,8,new Random(0));
                check(mc.world.getBlockId(8,112,8)==0,"leaf did not decay");
              }
              check(creative ? itemCount(mc)==drops : itemCount(mc)>drops,"leaf drops wrong after mode switch " + creative);
            }
          }
        } finally { enabled.set(fast,before); }
      });
      test("catalog transfer respects limits NBT cursor and capacity", () -> {
        CreativeVehicleChecks.mode(mc,"CREATIVE");
        var transfer=Class.forName("local.luke.power.creative.InventoryActions").getMethod("catalogueStack",net.minecraft.entity.player.PlayerEntity.class,ItemStack.class);
        Arrays.fill(mc.player.inventory.main,null);
        var held=new ItemStack(264,3,0); mc.player.inventory.setCursorStack(held);
        var template=new ItemStack(4,1,0); DeathChecks.tag(template).putString("test","catalogue");
        transfer.invoke(null,mc.player,template);
        check(mc.player.inventory.main[0].count==64 && DeathChecks.tag(mc.player.inventory.main[0]).getString("test").equals("catalogue"),"stack or NBT lost");
        check(mc.player.inventory.getCursorStack()==held && template.count==1,"cursor or catalog changed");
        transfer.invoke(null,mc.player,new ItemStack(257,1,37));
        check(mc.player.inventory.main[1].count==1 && mc.player.inventory.main[1].getDamage()==37,"tool limit/damage");
        for(int i=0;i<36;i++) mc.player.inventory.main[i]=new ItemStack(4,64,0);
        check(!(Boolean)transfer.invoke(null,mc.player,new ItemStack(4,1,0)),"full inventory accepted");
        mc.player.inventory.main[2].count=60;
        transfer.invoke(null,mc.player,new ItemStack(4,1,0)); check(mc.player.inventory.main[2].count==64,"partial capacity failed");
        CreativeVehicleChecks.mode(mc,"SURVIVAL"); check(!(Boolean)transfer.invoke(null,mc.player,template),"survival spawned items");
      });
      test("entity visibility changes rendering without removing or disabling pickup", () -> {
        var s=VisualConfig.copy(); s.hideEntities=true; s.hideItems=true; VisualConfig.preview(s);
        var item=item(mc,4,1,0,5,110,5); created.add(item);
        check(EntityVisibility.hidden(item) && !EntityVisibility.hidden(mc.player) && !item.dead,"default types wrong");
        s.hidePlayers=true; VisualConfig.preview(s);check(EntityVisibility.hidden(mc.player),"player type ignored");
        s.hideEntities=false; VisualConfig.preview(s);check(!EntityVisibility.hidden(item),"toggle failed to restore");
      });
      test("full bright leaves world light unchanged and resets across modes and cheats", () -> {
        var s=VisualConfig.copy();s.spectatorFullBright=true;VisualConfig.preview(s);
        CreativeVehicleChecks.mode(mc,"SURVIVAL");RenderControls.tick(mc);check(!RenderControls.fullBright(),"survival auto bright");
        int light=mc.world.method_164(class_56.BLOCK,0,20,0); float shade=mc.world.method_1782(0,20,0);
        CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.tick(mc);check(RenderControls.fullBright(),"spectator auto failed");
        check(mc.world.method_1782(0,20,0)==shade,"simulation shading changed");
        RenderControls.beginRender();try { check(mc.world.method_1782(0,20,0)==1,"render darkness remains"); } finally { RenderControls.endRender(); }
        check(mc.world.method_164(class_56.BLOCK,0,20,0)==light,"spawn light changed");
        CreativeVehicleChecks.mode(mc,"CREATIVE");RenderControls.tick(mc);check(!RenderControls.fullBright(),"spectator effect leaked");
        CreativeVehicleChecks.mode(mc,"SPECTATOR");RenderControls.tick(mc);world.allAllowed(false);RenderControls.tick(mc);check(!RenderControls.fullBright(),"cheats disabled kept fullbright");
        world.allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");RenderControls.tick(mc);
      });
      test("spawn command uses world spawn and safe help", () -> {
        var pos=mc.world.getSpawnPos(); var source=new local.luke.power.commands.util.SharedCommandSource(mc);
        var command=new local.luke.power.commands.command.vanilla.Spawn();
        double x=mc.player.x; command.command(source,new String[]{"spawn","help"});check(mc.player.x==x,"spawn help moved player");
        command.command(source,new String[]{"spawn"});
        check(mc.player.x==pos.x+.5 && mc.player.z==pos.z+.5 && Math.abs(mc.player.boundingBox.minY-pos.y)<.01,"wrong spawn position");
        world.allAllowed(false);check(!CommandPermissions.allowed("spawn"),"spawn not cheat-gated");world.allAllowed(true);
      });
    } finally {
      mc.world.isRemote=false; created.forEach(Entity::markDead);
      GameplayConfig.preview(original);VisualConfig.preview(visual);world.allAllowed(cheats);
      CreativeVehicleChecks.mode(mc,mode);RenderControls.tick(mc);
      System.arraycopy(inventory,0,mc.player.inventory.main,0,inventory.length);mc.player.inventory.setCursorStack(cursor);
      mc.player.method_1340(px,py,pz);
    }
    log("RELEASE 150 FAILURES " + failures);
  }
  static class_142 item(Minecraft mc,int id,int count,int damage,double x,double y,double z) {
    var item=new class_142(mc.world,x,y,z,new ItemStack(id,count,damage)); mc.world.method_210(item);return item;
  }
  static int count(List<Entity> entities) {return entities.stream().filter(e->!e.dead && e instanceof class_142).mapToInt(e->((class_142)e).field_564.count).sum();}
  static int alive(List<Entity> entities) {return (int)entities.stream().filter(e->!e.dead).count();}
  static int itemCount(Minecraft mc) {return (int)mc.world.method_291().stream().filter(e->e instanceof class_142 it && !it.dead).count();}
}
