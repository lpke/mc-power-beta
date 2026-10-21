package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.chat.ChatBuffer;
import local.luke.power.death.DeathItems;
import local.luke.power.input.InventoryPanels;
import local.luke.power.rules.GameRule;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.*;
import org.lwjgl.opengl.GL11;

/** Live registry aliases and real chat rendering/completion without executing a command. */
final class Commands403Checks {
  private Commands403Checks() {}
  static void run(Minecraft mc)throws Exception {
    check(mc.world!=null&&mc.player!=null&&org.lwjgl.opengl.Display.isActive(),"focus a disposable loaded world");
    failures=0;var rules=AccessRules.copy(mc);var previous=mc.currentScreen;boolean grabbed=Mouse.isGrabbed(),repeat=Keyboard.areRepeatEventsEnabled();
    String inventory=DeathItems.inventory(DeathItems.snapshot(mc.player.inventory,mc.player.playerContainer));
    ChatScreen chat=new ChatScreen();java.nio.file.Files.createDirectories(Sorting403Checks.REPORT);
    try {
      AccessRules.set(mc,GameRule.ALLOW_ITEM_SPAWNING,true);Mouse.setGrabbed(false);mc.currentScreen=chat;chat.init(mc,640,360);
      test("COMMANDS403 live Wooden Planks display alias keeps the native ID",()->{
        for(String name:List.of("wooden_planks","5")) {
          Object parsed=Class.forName("local.luke.power.commands.util.ItemArgument").getMethod("parse",String.class).invoke(null,name);
          var stack=(ItemStack)parsed.getClass().getMethod("stack",int.class).invoke(parsed,1);
          check(stack.itemId==5&&stack.count==1,"planks alias resolved to another item: "+name);
        }
        var names=Class.forName("local.luke.power.commands.util.ItemArgument");
        var choices=(String[])names.getMethod("suggestNames",String.class).invoke(null,"wooden_pla");
        check(Arrays.asList(choices).contains("nks"),"Wooden Planks is missing from live suggestions");
      });
      test("COMMANDS403 the last partial and fully typed suggestions retain their list",()->{
        for(String token:List.of("wooden_plank","wooden_planks")) {
          set("/give @p "+token);String file="chat-"+token+"-640";
          GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
          try{GL11.glClearColor(.3f,.4f,.5f,1);Sorting403Checks.capture(mc,chat,file,false);}finally{GL11.glPopAttrib();}
          String[] choices=(String[])UiChecks.field(chat,"suggestions");
          check(choices.length==1&&choices[0].equals(token.endsWith("s")?"":"s"),"near-complete chat lost its matching suffix");
          var pixels=javax.imageio.ImageIO.read(Sorting403Checks.REPORT.resolve(file+".png").toFile());
          int x=4+mc.textRenderer.getWidth("> /give @p ")+1;
          check((pixels.getRGB(x*2,(chat.height-24)*2)&0xffffff)==0,"single matching suggestion no longer draws its list row");
        }
      });
      test("COMMANDS403 Tab completes the plain display alias without executing give",()->{
        set("/give @p wooden_pla");Sorting403Checks.capture(mc,chat,"chat-before-tab-640",false);
        ((ScreenInput)(Object)chat).power$key('\0',Keyboard.KEY_TAB);
        var editor=(ChatBuffer)Class.forName("local.luke.power.commands.integration.chat.ChatWidgetAccess").getMethod("editor").invoke(null);
        check(editor.text().startsWith("/give @p wooden_planks"),"Tab did not complete the plain alias");
        check(inventory.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory,mc.player.playerContainer))),"completion executed give or touched native inventory");
      });
    }finally {
      chat.removed();InventoryPanels.remove(chat);AccessRules.restore(mc,rules);mc.currentScreen=previous;Mouse.setGrabbed(grabbed);Keyboard.enableRepeatEvents(repeat);
    }
    log("COMMANDS403 failures="+failures);check(failures==0,"Commands403 failures="+failures);
  }
  private static void set(String text)throws Exception {
    Class.forName("local.luke.power.commands.integration.chat.ChatWidgetAccess").getMethod("setText",String.class).invoke(null,text);
  }
}
