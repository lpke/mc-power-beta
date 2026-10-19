package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import local.luke.power.rules.*;
import local.luke.power.world.*;
import net.minecraft.client.Minecraft;

/** Uses an external restart to verify both global presets and native world metadata. */
final class RuleRestartChecks {
  private static final Path RECORD=Path.of("power-beta-rules-restart.json");
  static void run(Minecraft mc,String action)throws Exception {
    if(action.equals("prepare")) {
      var record=new JsonObject();
      record.add("oldSurvival",RulePresets.survival().document());
      record.add("oldCheats",RulePresets.cheats().document());
      var rules=new RuleSet();
      for(var rule:GameRule.values()) {
        if(rule.integer())rules.set(rule,73);else rules.set(rule,rule.ordinal()%2==0);
      }
      rules.set(GameRule.ALLOW_CREATIVE_MODE,true);
      WorldRuleChanges.apply(mc,rules);CreativeVehicleChecks.mode(mc,"CREATIVE");
      ((WorldDifficulty)mc.world.method_262()).power$difficulty(3);
      var survival=rules.copy();survival.set(GameRule.PLAYERS_SLEEPING_PERCENTAGE,17);
      var cheats=RuleSet.cheats();cheats.set(GameRule.EXPLOSIONS_DESTROY_ITEMS,false);
      RulePresets.save(survival,cheats);
      record.add("world",rules.document());record.add("survival",survival.document());record.add("cheats",cheats.document());
      record.addProperty("folder",WorldRuleChanges.directory(mc).getFileName().toString());
      var crafting=new JsonArray();
      for(int i=0;i<4;i++) {
        var stack=DeathChecks.tagged(35,201+i,i,"crafting-restart-"+i);
        mc.player.playerContainer.method_2084(i+1).setStack(stack);
        crafting.add(local.luke.power.death.DeathItems.encode(stack));
      }
      record.add("crafting",crafting);
      mc.world.method_195(true,null);WorldRuleChanges.save(mc);
      Files.writeString(RECORD,record.toString());mc.setWorld(null);
      log("RESTART PREPARED");return;
    }
    if(!action.equals("verify"))throw new IllegalArgumentException(action);
    failures=0;var record=JsonParser.parseString(Files.readString(RECORD)).getAsJsonObject();
    try {
      test("global rule presets survive a fresh process independently",()->{
        check(RulePresets.survival().equals(read(record,"survival")),"Survival preset changed");
        check(RulePresets.cheats().equals(read(record,"cheats")),"Cheat preset changed");
      });
      test("all world rules, difficulty and player mode survive a fresh process",()->{
        mc.setWorld(null);mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);
        String folder=record.get("folder").getAsString();mc.method_2120(folder,folder,17320261003L);
        check(WorldRules.of(mc.world.method_262()).equals(read(record,"world")),"world rules changed");
        check(((WorldDifficulty)mc.world.method_262()).power$difficulty()==3,"difficulty changed");
        check(mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString().equals("CREATIVE"),"mode changed");
        for(int i=0;i<4;i++)check(record.getAsJsonArray("crafting").get(i).getAsString().equals(local.luke.power.death.DeathItems.encode(mc.player.playerContainer.method_2084(i+1).getStack())),"crafting input lost on restart "+i);
        mc.method_2139();
        for(int i=0;i<4;i++)check(record.getAsJsonArray("crafting").get(i).getAsString().equals(local.luke.power.death.DeathItems.encode(mc.player.playerContainer.method_2084(i+1).getStack())),"crafting input lost across dimension "+i);
        check(WorldRules.of(mc.world.method_262()).equals(read(record,"world")),"dimension rules changed");
        check(((WorldDifficulty)mc.world.method_262()).power$difficulty()==3,"dimension difficulty changed");
        mc.method_2139();
      });
      test("created Custom world keeps its own rules after later preset edits and restart",()->{
        String folder=Files.readString(Path.of("power-beta-custom-rules-world.txt"));
        mc.setWorld(null);mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);mc.method_2120(folder,folder,17320261015L);
        var rules=WorldRules.of(mc.world.method_262());
        check(!rules.enabled(GameRule.FIRE_SPREAD)&&rules.enabled(GameRule.ALLOW_CREATIVE_MODE),"Custom rules changed");
        check(rules.number(GameRule.PLAYERS_SLEEPING_PERCENTAGE)==100,"Custom followed a changed preset");
        check(((WorldDifficulty)mc.world.method_262()).power$difficulty()==2,"Custom difficulty changed");
      });
    } finally {RulePresets.save(read(record,"oldSurvival"),read(record,"oldCheats"));}
    log("RULE RESTART FAILURES "+failures);
  }
  private static RuleSet read(JsonObject record,String key){return RuleSet.read(record.getAsJsonObject(key),new RuleSet());}
}
