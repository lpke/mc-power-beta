package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.world.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_180;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.*;
import org.lwjgl.input.Keyboard;

final class CreationRuleChecks {
  static void run(Minecraft mc) throws Exception {
    failures=0;mc.setWorld(null);
    RuleSet survival=RulePresets.survival(),cheats=RulePresets.cheats();
    boolean auto=MenuPreferences.current().autoApply;
    try {
      MenuPreferences.update(v->v.autoApply=true);
      test("main-menu templates save independently and existing creation drafts keep their original copy",()->{
        var frozen=new CreationRules(survival,cheats);
        var options=new PowerOptionsScreen(null);mc.setScreen(options);
        check(options.visiblePages().contains("Game rules"),"main-menu templates unavailable");
        check(options.session().settings().stream().filter(s->s.backend.equals("rulePresets")).count()==62,"template rule count");
        var fire=WorldRulesChecks.find(options.session(),"rules.survival.fire_spread");
        fire.parse(Boolean.toString(!survival.enabled(GameRule.FIRE_SPREAD)));options.changed(fire);
        check(RulePresets.survival().enabled(GameRule.FIRE_SPREAD)!=survival.enabled(GameRule.FIRE_SPREAD),"auto-apply did not persist preset");
        check(RulePresets.cheats().equals(cheats),"Survival edit changed Cheat defaults");
        check(frozen.rules().equals(survival),"preset edit changed an existing creation draft");
        for(int width:new int[]{320,640}) {
          options.init(mc,width,width==320?240:420);field(options,"page","Game rules");search(options,"");
          SettingsSnapshot.render(mc,options,width,width==320?240:420,Path.of("power-beta-data/reports/presets-"+width+".png"));
        }
        RulePresets.save(survival,cheats);
      });
      test("creation preserves text and Custom rules across preset cycles, editor visits and resizing",()->{
        var creation=new class_180(null);mc.setScreen(creation);creation.init(mc,320,240);
        ((TextFieldWidget)field(creation,"field_632")).setText("Custom rules fixture");
        ((TextFieldWidget)field(creation,"field_633")).setText("17320261015");
        press(creation,2);press(creation,2);
        var draft=(CreationRules)field(creation,"power$draft");
        check(draft.preset()==CreationRules.Preset.CUSTOM,"preset cycle mismatch");
        check(((TextFieldWidget)field(creation,"field_632")).getText().equals("Custom rules fixture"),"preset cycle lost world name");
        byte[] before=Files.readAllBytes(PowerConfig.path());
        press(creation,4);var editor=(PowerOptionsScreen)mc.currentScreen;
        check(editor.session().settings().size()==31,"creation editor exposes unrelated settings");
        var fire=WorldRulesChecks.find(editor.session(),"rules.creation.fire_spread");
        fire.parse("false");editor.changed(fire);
        check(!draft.custom().enabled(GameRule.FIRE_SPREAD),"auto-apply missed future-world draft");
        check(Arrays.equals(before,Files.readAllBytes(PowerConfig.path())),"creation edit wrote global settings");
        check(RulePresets.survival().equals(survival)&&RulePresets.cheats().equals(cheats),"creation edit changed saved presets");
        search(editor,"doFireTick");
        check(!((List<?>)field(editor,"rows")).isEmpty(),"creation aliases not searchable");
        invoke(editor,"chooseRulePreset",true);check(draft.custom().equals(cheats),"copy Cheat defaults incomplete");
        invoke(editor,"chooseRulePreset",false);check(draft.custom().equals(survival),"copy Survival defaults incomplete");
        fire=WorldRulesChecks.find(editor.session(),"rules.creation.fire_spread");fire.parse("false");editor.changed(fire);
        mc.setScreen(creation);creation.init(mc,640,420);
        for(int i=0;i<3;i++)press(creation,2);
        check(draft.preset()==CreationRules.Preset.CUSTOM&&!draft.rules().enabled(GameRule.FIRE_SPREAD),"cycling erased Custom rules");
        check(((TextFieldWidget)field(creation,"field_633")).getText().equals("17320261015"),"resize lost seed");
        press(creation,5);check(!draft.problem().isEmpty(),"forbidden starting mode accepted");
        creation.render(-1,-1,0);check(!button(creation,0).active,"Create enabled for forbidden mode");
        press(creation,0);check(mc.world==null,"invalid draft created a world");
        for(int width:new int[]{320,640}) {
          creation.init(mc,width,width==320?240:420);
          SettingsSnapshot.render(mc,creation,width,width==320?240:420,Path.of("power-beta-data/reports/create-custom-"+width+".png"));
        }
        press(creation,4);editor=(PowerOptionsScreen)mc.currentScreen;
        var permission=WorldRulesChecks.find(editor.session(),"rules.creation.allow_creative_mode");permission.parse("true");editor.changed(permission);
        mc.setScreen(creation);check(draft.problem().isEmpty(),"valid mode/rule combination rejected");
        creation.render(-1,-1,0);check(button(creation,0).active,"Create remained disabled");
        String folder=(String)field(creation,"field_634");RuleSet expected=draft.rules();
        press(creation,0);check(mc.world!=null&&WorldRules.of(mc.world.method_262()).equals(expected),"created rules differ from draft");
        check(mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString().equals("CREATIVE"),"Custom starting mode lost");
        mc.world.method_195(true,null);WorldRuleFiles.verifySaved(WorldRuleChanges.directory(mc),expected,2);
        mc.setWorld(null);mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);mc.method_2120(folder,folder,17320261015L);
        check(WorldRules.of(mc.world.method_262()).equals(expected),"reloaded rules changed");
        Files.writeString(Path.of("power-beta-custom-rules-world.txt"),folder);
        mc.setWorld(null);
      });
      test("manual creation drafts cancel without touching templates or creating worlds",()->{
        MenuPreferences.update(v->v.autoApply=false);
        var draft=new CreationRules(survival,cheats);draft.cycle(-1);
        var parent=new class_180(null);mc.setScreen(parent);PowerOptionsScreen.openCreationRules(mc,parent,draft);
        var editor=(PowerOptionsScreen)mc.currentScreen;var fire=WorldRulesChecks.find(editor.session(),"rules.creation.fire_spread");
        boolean before=fire.value.getAsBoolean();fire.parse(Boolean.toString(!before));editor.changed(fire);
        check(draft.custom().enabled(GameRule.FIRE_SPREAD)==before,"manual edit committed early");
        ((ScreenInput)(Object)editor).power$key('\0',Keyboard.KEY_ESCAPE);
        check((boolean)field(editor,"confirmClose")&&mc.currentScreen==editor,"dirty exit skipped confirmation");
        editor.session().discard();check(draft.custom().enabled(GameRule.FIRE_SPREAD)==before,"Cancel changed creation rules");
        check(mc.world==null&&RulePresets.survival().equals(survival)&&RulePresets.cheats().equals(cheats),"Cancel touched saved data");
        mc.setScreen(null);
      });
    } finally {RulePresets.save(survival,cheats);MenuPreferences.update(v->v.autoApply=auto);}
    log("CREATION RULES FAILURES "+failures);
  }
  private static ButtonWidget button(class_180 screen,int id){return ((ScreenInput)screen).power$buttons().stream().filter(b->b.id==id).findFirst().orElseThrow();}
  private static void press(class_180 screen,int id){var button=button(screen,id);((ScreenInput)screen).power$click(button.x+5,button.y+5,0);}
  private static void invoke(PowerOptionsScreen screen,String name,boolean value)throws Exception{var method=PowerOptionsScreen.class.getDeclaredMethod(name,boolean.class);method.setAccessible(true);method.invoke(screen,value);}
}
