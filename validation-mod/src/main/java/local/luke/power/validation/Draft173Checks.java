package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

final class Draft173Checks {
  private static void key(Screen s,int key){((ScreenInput)s).power$key('\0',key);}
  private static void click(Screen s,int x,int y){((ScreenInput)s).power$click(x,y,0);}
  private static boolean prompted(Screen s)throws Exception{return (boolean)call(field(s,"exitPrompt"),"active");}
  private static void choice(Screen s,int index){int w=Math.min(76,Math.max(10,(s.width-32)/3));click(s,(s.width-(w*3+8))/2+index*(w+4)+4,s.height/2+14);}
  private static void remove(BlockRulesScreen s)throws Exception{click(s,(int)call(s,"right")-20,68);}
  private static BlockRulesScreen rules(Minecraft mc,PowerOptionsScreen parent)throws Exception{
    BlockRulesScreen s=(BlockRulesScreen)ItemEditors.screen(parent,find(parent.session(),"veinMining.tools"));mc.setScreen(s);return s;
  }
  static void run(Minecraft mc)throws Exception{
    failures=0;boolean auto=MenuPreferences.current().autoApply;MenuPreferences.update(p->p.autoApply=false);
    var parent=new PowerOptionsScreen(null);mc.setScreen(parent);
    byte[] config=Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
    var inventory=mc.player.inventory.main.clone();var setting=find(parent.session(),"veinMining.tools");JsonElement before=setting.value.deepCopy();
    try {
      test("unchanged rule and picker exits need no confirmation",()->{
        var rules=rules(mc,parent);key(rules,Keyboard.KEY_ESCAPE);check(mc.currentScreen==parent,"unchanged rules warned");
        var picker=new ItemPickerScreen(parent,"Tools",ItemCatalogue.Domain.ITEM,List.of("278"),true,v->{throw new AssertionError("unexpected save");});mc.setScreen(picker);
        key(picker,Keyboard.KEY_ESCAPE);check(mc.currentScreen==parent,"unchanged selection warned");
      });
      test("rule Escape/Cancel warns; modal blocks editing and preserves scroll on resize",()->{
        for(int width:new int[]{320,640}){
          var rules=rules(mc,parent);rules.init(mc,width,240);remove(rules);JsonElement draft=((JsonObject)field(rules,"draft")).deepCopy();
          call(rules,"leave");check(prompted(rules) && mc.currentScreen==rules,"changed rules exited silently");
          int scroll=(int)field(rules,"scroll");remove(rules);key(rules,Keyboard.KEY_RETURN);
          check(draft.equals(field(rules,"draft")) && setting.value.equals(before),"modal edited/saved underlying rules");
          rules.init(mc,width,240);rules.render(-1,-1,1);check(prompted(rules) && (int)field(rules,"scroll")==scroll,"resize lost prompt or scroll");
          choice(rules,2);check(!prompted(rules) && mc.currentScreen==rules && draft.equals(field(rules,"draft")),"Cancel discarded draft");
          click(rules,(int)call(rules,"right")-34,rules.height-20);check(prompted(rules),"Cancel button skipped warning");
          key(rules,Keyboard.KEY_ESCAPE);check(!prompted(rules) && mc.currentScreen==rules,"Escape closed editor behind dialog");
          key(rules,Keyboard.KEY_ESCAPE);choice(rules,1);check(mc.currentScreen==parent && setting.value.equals(before),"Discard changed setting");
          check(GL11.glGetError()==0,"dialog rendering error");
        }
      });
      test("saving rule confirmation updates parent draft; Options Cancel restores it",()->{
        var rules=rules(mc,parent);remove(rules);JsonElement draft=((JsonObject)field(rules,"draft")).deepCopy();
        key(rules,Keyboard.KEY_ESCAPE);choice(rules,0);
        check(mc.currentScreen==parent && setting.value.equals(draft) && !setting.value.equals(before),"Save lost draft");
        parent.session().discard();check(setting.value.equals(before),"Options Cancel lost original rules");
      });
      test("nested required-tool picker has independent Save/Discard/Cancel",()->{
        var rules=rules(mc,parent);JsonElement originalDraft=((JsonObject)field(rules,"draft")).deepCopy();
        click(rules,(int)call(rules,"toolsX")+5,70);var picker=(ItemPickerScreen)mc.currentScreen;
        click(picker,(int)call(picker,"left")+85,picker.height-20); // Clear tools.
        key(picker,Keyboard.KEY_ESCAPE);check(prompted(picker),"changed required tools exited silently");
        choice(picker,2);check(mc.currentScreen==picker && !prompted(picker),"nested Cancel did not stay");
        key(picker,Keyboard.KEY_ESCAPE);choice(picker,1);
        check(mc.currentScreen==rules && originalDraft.equals(field(rules,"draft")),"nested Discard changed rules");
        click(rules,(int)call(rules,"toolsX")+5,70);picker=(ItemPickerScreen)mc.currentScreen;
        click(picker,(int)call(picker,"left")+85,picker.height-20);key(picker,Keyboard.KEY_ESCAPE);choice(picker,0);
        check(mc.currentScreen==rules && !originalDraft.equals(field(rules,"draft")) && setting.value.equals(before),"nested Save bypassed rule draft");
        key(rules,Keyboard.KEY_ESCAPE);check(prompted(rules),"saved child did not dirty rules");choice(rules,1);
      });
      test("selection toggled back to its original value exits directly",()->{
        var accepted=new AtomicReference<List<String>>();
        var picker=new ItemPickerScreen(parent,"Tools",ItemCatalogue.Domain.ITEM,List.of("278"),true,accepted::set);mc.setScreen(picker);
        ((TextInput)field(picker,"search")).setText("278");call(picker,"filter");
        click(picker,(int)call(picker,"left")+35,75);click(picker,(int)call(picker,"left")+35,75);
        key(picker,Keyboard.KEY_ESCAPE);check(mc.currentScreen==parent && accepted.get()==null,"reverted selection warned or saved");
      });
      test("failed Save leaves the selection editable with its error",()->{
        var picker=new ItemPickerScreen(parent,"Tools",ItemCatalogue.Domain.ITEM,List.of("278"),true,v->{throw new IllegalArgumentException("Rejected test selection");});mc.setScreen(picker);
        click(picker,(int)call(picker,"left")+85,picker.height-20);key(picker,Keyboard.KEY_ESCAPE);choice(picker,0);
        check(mc.currentScreen==picker && !prompted(picker) && field(picker,"error").equals("Rejected test selection"),"failed save lost editor/error");
        key(picker,Keyboard.KEY_ESCAPE);choice(picker,1);
      });
      check(Arrays.equals(config,Files.readAllBytes(local.luke.power.storage.PowerConfig.path())),"draft dialogs wrote config");
      check(Arrays.equals(inventory,mc.player.inventory.main),"draft dialogs touched inventory");
    } finally {parent.session().discard();mc.setScreen(null);MenuPreferences.update(p->p.autoApply=auto);}
    log("RELEASE 173 EDITOR FAILURES "+failures);
  }
}
