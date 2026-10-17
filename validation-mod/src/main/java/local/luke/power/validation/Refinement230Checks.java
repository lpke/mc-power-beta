package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.config.*;
import local.luke.power.config.backend.LogoBackend;
import local.luke.power.input.PanoramaRenderer;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import org.lwjgl.input.Keyboard;

final class Refinement230Checks {
  private static final Path REPORT=Path.of("power-beta-data/reports/refinements-230");
  private static Object get(Object target,String name)throws Exception{return Editor230Checks.get(target,name);}
  private static void open(Minecraft mc,Screen screen){mc.setScreen(screen);screen.init(mc,640,420);}
  private static void click(Screen screen,int x,int y,int button){((ScreenInput)(Object)screen).power$click(x,y,button);}
  private static Setting setting(ConfigSession s,String id){return s.settings().stream().filter(v->v.id.equals(id)).findFirst().orElseThrow();}
  private static ConfigSession logo()throws Exception{var session=new ConfigSession();LogoBackend.register(session);return session;}
  private static JsonObject design(ConfigSession session){var out=new JsonObject();for(Setting s:session.settings())if(TitleTiles.DESIGN_KEYS.contains(s.id))out.add(s.id,s.value.deepCopy());return out;}
  static void run(Minecraft mc)throws Exception {
    failures=0;Files.createDirectories(REPORT);var original=PowerConfig.document();var world=mc.world;
    try {
      test("native logo clicks cycle the base, both presets and every official style in both directions",()->{
        var session=logo();JsonObject base=design(session);var presets=new JsonArray();
        for(int i=0;i<2;i++){
          JsonObject preset=new JsonObject(),value=base.deepCopy();preset.addProperty("id",UUID.randomUUID().toString());preset.addProperty("name","Cycle "+i);
          value.addProperty("logo.logo.shadow",i==0);value.getAsJsonArray("logo.logo.rotation").set(0,new JsonPrimitive(25+i));
          preset.add("values",value);presets.add(preset);
        }
        setting(session,"logo.presets").value=presets;setting(session,"logo.style").value=new JsonPrimitive(0);
        setting(session,"logo.selectedPreset").value=new JsonPrimitive("");setting(session,"logo.retroBase").value=new JsonObject();session.save(Path.of(".").toAbsolutePath());
        mc.world=null;var title=new TitleScreen();open(mc,title);
        for(int index=1;index<=7;index++){
          click(title,320,60,0);var current=logo();int expected=index<3||index==7?0:index-2;
          check(setting(current,"logo.style").value.getAsInt()==expected,"forward style "+index);
          if(index<3)check(setting(current,"logo.selectedPreset").value.equals(presets.get(index-1).getAsJsonObject().get("id")),"forward preset "+index);
        }
        check(design(logo()).equals(base),"cycling lost the custom retro base");
        click(title,320,60,1);check(setting(logo(),"logo.style").value.getAsInt()==4,"backward wrap did not reach Modern");
        for(int index=0;index<6;index++)click(title,320,60,1);
        check(design(logo()).equals(base)&&setting(logo(),"logo.style").value.getAsInt()==0,"reverse cycle lost the base");
        int before=setting(logo(),"logo.style").value.getAsInt();click(title,4,4,0);check(setting(logo(),"logo.style").value.getAsInt()==before,"blank title space changed style");
        check(TitlePreview.renderableBlock.test(1)&&TitlePreview.renderableBlock.test(17),"stone/log rejected");
        check(!TitlePreview.renderableBlock.test(8)&&!TitlePreview.renderableBlock.test(9)&&!TitlePreview.renderableBlock.test(10),"liquids allowed");
        var options=new PowerOptionsScreen(null);open(mc,options);var editor=new TitleLetteringScreen(options);open(mc,editor);
        int tile=((Number)Editor230Checks.invoke(editor,"tileSize",new Class<?>[0])).intValue();
        int gx=((Number)Editor230Checks.invoke(editor,"gridLeft",new Class<?>[0])).intValue(),gy=((Number)Editor230Checks.invoke(editor,"gridTop",new Class<?>[0])).intValue();
        click(editor,gx+tile/2,gy+tile/2,0);Editor230Checks.invoke(editor,"stopGesture",new Class<?>[0]);
        SettingsDraft local=(SettingsDraft)get(editor,"draft");JsonObject edited=new JsonObject();for(Setting s:local.settings())if(TitleTiles.DESIGN_KEYS.contains(s.id))edited.add(s.id,s.value.deepCopy());
        Editor230Checks.invoke(editor,"save",new Class<?>[0]);setting(options.session(),"logo.style").value=new JsonPrimitive(1);options.session().save(Path.of(".").toAbsolutePath());
        open(mc,title);for(int i=0;i<4;i++)click(title,320,60,0);
        check(design(logo()).equals(edited),"a later Options style change lost edited retro lettering");
        SettingsSnapshot.render(mc,title,640,420,REPORT.resolve("logo-cycle.png"));
      });
      mc.world=world;
      test("custom version text edits inline and remains local until Done",()->{
        var options=new PowerOptionsScreen(null);open(mc,options);
        var hud=new HudEditorScreen(options,"power_controls:userinterface.versionTextConfig.customVersionText");open(mc,hud);
        List<?> controls=(List<?>)Editor230Checks.invoke(hud,"controls",new Class<?>[0]);
        Setting value=(Setting)controls.stream().filter(v->((Setting)v).id.endsWith(".customVersionText")).findFirst().orElseThrow();
        int index=controls.indexOf(value);var scrollField=hud.getClass().getSuperclass().getDeclaredField("scroll");scrollField.setAccessible(true);scrollField.set(hud,index*23d);
        SettingsSnapshot.render(mc,hud,640,420,REPORT.resolve("version-before.png"));
        int x=((Number)Editor230Checks.invoke(hud,"controlX",new Class<?>[0])).intValue();
        int y=80+index*23-((Number)get(hud,"scroll")).intValue()+7;click(hud,x+12,y,0);check(mc.currentScreen==hud,"text edit opened another screen");
        ((TextInput)get(get(hud,"inline"),"input")).setText("Inline version preview");((ScreenInput)(Object)hud).power$key('\0',Keyboard.KEY_RETURN);
        SettingsDraft draft=(SettingsDraft)get(hud,"draft");check(draft.get(value.id).value.getAsString().equals("Inline version preview"),"inline text not accepted");
        check(!setting(options.session(),value.id).value.getAsString().equals("Inline version preview"),"inline text leaked to parent");
      });
      test("menu transitions share one clock and retain original dirt list headers and footers",()->{
        var session=SettingsRegistry.open(mc);String prefix="power_controls:userinterface.panoramaConfig.";
        setting(session,prefix+"onlyTitleScreen").value=new JsonPrimitive(false);setting(session,prefix+"panoramaFolder").value=new JsonPrimitive("b1_7_3");session.preview(true);
        mc.world=null;var screen=new net.minecraft.client.gui.screen.world.SelectWorldScreen(new TitleScreen());open(mc,screen);
        Path before=REPORT.resolve("dirt-worlds.png"),after=REPORT.resolve("panorama-worlds.png");SettingsSnapshot.render(mc,screen,640,420,before);
        setting(session,prefix+"panoramaFolder").value=new JsonPrimitive("b1_8_1");session.preview(true);
        float start=PanoramaRenderer.animationTicks();open(mc,new TitleScreen());float title=PanoramaRenderer.animationTicks();open(mc,screen);
        float menu=PanoramaRenderer.animationTicks();check(title>=start&&menu>=title&&menu-start<100,"menu clock restarted or jumped");
        SettingsSnapshot.render(mc,screen,640,420,after);var a=ImageIO.read(before.toFile());var b=ImageIO.read(after.toFile());
        int same=0;for(int y:new int[]{8,16,a.getHeight()-8,a.getHeight()-16})for(int x=8;x<48;x++)if(a.getRGB(x,y)==b.getRGB(x,y))same++;
        check(same==160,"native dirt header/footer changed: "+same+" of 160 pixels");
        session.discard();
      });
    }finally{mc.world=world;mc.setScreen(null);PowerConfig.write(original);new LogoBackend().apply(Map.of());}
    log("REFINEMENT 2.3 FAILURES "+failures);
  }
}
