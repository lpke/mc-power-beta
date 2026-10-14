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

final class PickerChecks {
  private static void click(Screen s,int x,int y){((ScreenInput)(Object)s).power$click(x,y,0);}
  private static void key(Screen s,int key){((ScreenInput)(Object)s).power$key((char)0,key);}
  private static void query(ItemPickerScreen s,String query) throws Exception {
    ((TextInput)field(s,"search")).setText(query);call(s,"filter");s.render(-1,-1,1);
  }
  static void scene(Minecraft mc,String action) throws Exception {
    PowerOptionsScreen parent=new PowerOptionsScreen(null);mc.setScreen(parent);
    if(action.equals("rules"))mc.setScreen(ItemEditors.screen(parent,find(parent.session(),"veinMining.tools")));
    else {
      ItemPickerScreen picker=new ItemPickerScreen(parent,"Choose blocks and items",action.equals("states")?ItemCatalogue.Domain.BLOCK:ItemCatalogue.Domain.FILTER,List.of("35:4"),true,v->{});
      mc.setScreen(picker);query(picker,action.equals("states")?"redstone":"wool");
    }
  }
  static void run(Minecraft mc) throws Exception {
    failures=0;boolean auto=MenuPreferences.current().autoApply;MenuPreferences.update(p->p.autoApply=false);
    PowerOptionsScreen parent=new PowerOptionsScreen(null);mc.setScreen(parent);
    byte[] config=Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
    var inventory=mc.player.inventory.main.clone();int[] counts=Arrays.stream(inventory).mapToInt(s->s==null?0:s.count).toArray();
    try {
      test("catalogue includes alternate block IDs, metadata and technical names",()->{
        var blocks=ItemCatalogue.entries(ItemCatalogue.Domain.BLOCK);
        for(int id:new int[]{8,9,10,11,43,51,61,62,63,68,73,74,75,76,93,94})
          check(blocks.stream().anyMatch(e->e.id()==id && e.technical().contains("ID: "+id)),"missing state "+id);
        var items=ItemCatalogue.entries(ItemCatalogue.Domain.FILTER);
        for(int damage=0;damage<16;damage++){
          String value="35:"+damage;check(items.stream().anyMatch(e->e.value().equals(value)),"missing wool "+value);
        }
        check(items.stream().anyMatch(e->e.value().equals("35") && e.name().contains("all variants")),"wildcard not distinguished");
        check(ItemCatalogue.resolve(ItemCatalogue.Domain.BLOCK,"minecraft:stone").id()==1,"registry lookup failed");
        check(ItemCatalogue.resolve(ItemCatalogue.Domain.FILTER,"35:4").damage()==4,"metadata lost");
        for(String invalid:List.of("0","999999","minecraft:missing_block","1:2","-1","")) {
          boolean rejected=false;try{ItemCatalogue.resolve(ItemCatalogue.Domain.BLOCK,invalid);}catch(IllegalArgumentException e){rejected=true;}
          check(rejected,"invalid custom block accepted: "+invalid);
        }
      });
      test("every item-ID setting opens the shared editor",()->{
        int count=0;
        for(Setting setting:parent.session().settings())if(ItemEditors.accepts(setting)){
          Screen screen=ItemEditors.screen(parent,setting);mc.setScreen(screen);screen.render(-1,-1,1);
          check(screen instanceof ItemPickerScreen || screen instanceof BlockRulesScreen,"wrong editor "+setting.id);count++;
        }
        check(count>=6,"some ID editors missing: "+count);
      });
      test("picker selection and custom-ID validation stay local until Done",()->{
        AtomicReference<List<String>> accepted=new AtomicReference<>();
        ItemPickerScreen s=new ItemPickerScreen(parent,"Pick blocks",ItemCatalogue.Domain.BLOCK,List.of("1"),true,accepted::set);mc.setScreen(s);
        query(s,"diamond ore");check(((List<?>)field(s,"shown")).size()==1,"search not specific");
        click(s,(int)call(s,"left")+45,75);check(accepted.get()==null,"selection committed early");
        call(s,"custom");Screen custom=mc.currentScreen;
        ((TextInput)field(custom,"input")).setText("minecraft:missing_block");call(custom,"save");
        check(mc.currentScreen==custom && !field(custom,"error").equals(""),"invalid custom ID closed editor");
        ((TextInput)field(custom,"input")).setText("minecraft:gold_ore");call(custom,"save");
        check(mc.currentScreen==s && accepted.get()==null,"custom ID committed parent");
        call(s,"save");check(accepted.get().containsAll(List.of("1","56","14")),"selection lost "+accepted.get());
        accepted.set(null);s=new ItemPickerScreen(parent,"Pick blocks",ItemCatalogue.Domain.BLOCK,List.of("1"),true,accepted::set);mc.setScreen(s);
        query(s,"diamond ore");click(s,(int)call(s,"left")+45,75);key(s,Keyboard.KEY_ESCAPE);check(accepted.get()==null,"Cancel committed selection");
      });
      test("vein rules and nested tool edits support Done and Cancel independently",()->{
        Setting setting=find(parent.session(),"veinMining.tools");JsonElement before=setting.value.deepCopy();
        BlockRulesScreen rules=(BlockRulesScreen)ItemEditors.screen(parent,setting);mc.setScreen(rules);
        String first=((JsonObject)field(rules,"draft")).keySet().iterator().next();
        click(rules,(int)call(rules,"toolsX")+5,70);check(mc.currentScreen instanceof ItemPickerScreen,"tool picker missing");
        ItemPickerScreen picker=(ItemPickerScreen)mc.currentScreen;click(picker,(int)call(picker,"left")+85,picker.height-20);
        call(picker,"save");check(setting.value.equals(before),"nested tool edit changed setting");
        key(rules,Keyboard.KEY_ESCAPE);check(setting.value.equals(before),"rule Cancel changed setting");
        rules=(BlockRulesScreen)ItemEditors.screen(parent,setting);mc.setScreen(rules);
        click(rules,(int)call(rules,"toolsX")+5,70);picker=(ItemPickerScreen)mc.currentScreen;
        click(picker,(int)call(picker,"left")+85,picker.height-20);query(picker,"diamond pickaxe");click(picker,(int)call(picker,"left")+40,75);call(picker,"save");
        call(rules,"save");check(setting.value.getAsJsonObject().getAsJsonArray(first).size()==1 && setting.value.getAsJsonObject().getAsJsonArray(first).get(0).getAsInt()==278,"required tool not saved");
        parent.session().discard();check(find(SettingsRegistry.open(mc),setting.id).value.equals(before),"Options Cancel did not restore preview");
      });
      test("all native icons render at narrow and wide sizes without GL errors",()->{
        for(ItemCatalogue.Domain domain:List.of(ItemCatalogue.Domain.FILTER,ItemCatalogue.Domain.BLOCK)){
          ItemPickerScreen s=new ItemPickerScreen(parent,"Choose blocks and items",domain,List.of(),true,v->{});mc.setScreen(s);
          for(int width:new int[]{320,640}){
            s.init(mc,width,240);int size=((List<?>)field(s,"shown")).size();
            for(int row=0;row<size;row+=4){field(s,"scroll",row*24);s.render(45,75,1);check(GL11.glGetError()==0,"icon rendering error at "+domain+"/"+row);}
            check((int)call(s,"right")-156>=(int)call(s,"left")+124,"buttons overlap");
          }
        }
        BlockRulesScreen rules=(BlockRulesScreen)ItemEditors.screen(parent,find(parent.session(),"veinMining.tools"));mc.setScreen(rules);rules.init(mc,320,240);rules.render(30,72,1);
        check((int)call(rules,"right")-140>=(int)call(rules,"left")+148,"rule buttons overlap");
      });
      check(Arrays.equals(config,Files.readAllBytes(local.luke.power.storage.PowerConfig.path())),"draft edits wrote config");
      check(local.luke.power.building.hotbar.InventoryRows.identical(inventory,mc.player.inventory.main),"picker changed inventory references");
      for(int i=0;i<inventory.length;i++)check(inventory[i]==null || inventory[i].count==counts[i],"picker changed item count");
    }finally{parent.session().discard();mc.setScreen(null);MenuPreferences.update(p->p.autoApply=auto);}
    log("RELEASE 171 PICKER FAILURES "+failures);
  }
}
