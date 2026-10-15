package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.WebsiteGallery.*;
import java.lang.reflect.*;
import java.util.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;

/** Temporary camera and UI motions for authentic recordings in the disposable gallery world. */
final class WebsiteMotion {
  private static String motion="";
  private static long start;
  private static Object target;
  private static double distance;
  private static float yaw, pitch;
  static void run(Minecraft mc,String action) throws Exception {
    motion="";
    if(action.startsWith("section ")) {
      String[] parts=action.substring(8).split("\\|",2);menu(mc,parts[0],"");
      var screen=(PowerOptionsScreen)mc.currentScreen;
      var ids=screen.session().settings().stream().filter(s->s.page.equals(parts[0])&&Arrays.asList(parts[1].split(";")).contains(s.group)).map(s->s.id).toList();
      Validation.check(!ids.isEmpty(),"no settings in "+action);
      field(screen,"relatedIds",ids);field(screen,"relatedLabel",parts[1].replace(';',','));call(screen,"layout");return;
    }
    if(action.startsWith("page ")) {
      var screen=(PowerOptionsScreen)mc.currentScreen;
      int step=Math.max(24,(int)call(screen,"bottom")-(int)call(screen,"top")-48);
      int maximum=Math.max(0,(int)call(screen,"contentHeight")-((int)call(screen,"bottom")-(int)call(screen,"top")));
      int page=Integer.parseInt(action.substring(5));field(screen,"scroll",(double)Math.min(maximum,page*step));call(screen,"layout");
      Validation.log("GALLERY PAGES "+(int)Math.ceil(maximum/(double)step));Mouse.setCursorPosition(0,0);return;
    }
    if(action.equals("music-all")||action.equals("music-presets")) {
      Release150Gallery.run(mc,"music");var screen=(PowerOptionsScreen)mc.currentScreen;var library=field(screen,"library");
      if(action.endsWith("presets"))call(library,"openPresets");
      else {
        field(library,"folder","");call(library,"rebuild");
        ((Set<?>)field(library,"collapsed")).clear();call(library,"rebuildRows");
      }
      return;
    }
    if(action.startsWith("creative ")) {creative(mc,action.substring(9));return;}
    if(action.equals("scroll")) {
      var screen=(PowerOptionsScreen)mc.currentScreen;
      if((boolean)call(screen,"libraryVisible")) {
        target=field(screen,"library");
        distance=Math.min(900,Math.max(0,(int)field(target,"contentHeight")-((int)call(target,"bottom")-(int)call(target,"listTop"))));
        motion="library";
      } else {
        target=screen;distance=Math.min(900,Math.max(0,(int)call(screen,"contentHeight")-((int)call(screen,"bottom")-(int)call(screen,"top"))));
        motion="menu";
      }
      Validation.log("GALLERY SCROLL "+motion+" "+distance);start=System.nanoTime();return;
    }
    if(action.equals("pan")) {
      yaw=mc.player.yaw;pitch=mc.player.pitch;motion="pan";start=System.nanoTime();return;
    }
    if(action.equals("freecam")) {
      WebsiteGallery.run(mc,"castle");camera(mc,0,4,-20,0,0);
      var settings=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);

      var controller=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
      controller.getClass().getMethod("startFromView",Minecraft.class).invoke(controller,mc);
      controller.getClass().getMethod("setActive",boolean.class).invoke(controller,true);
      target=controller;motion="freecam";start=System.nanoTime();return;
    }
    if(!action.equals("stop"))throw new IllegalArgumentException(action);
  }
  static void tick(Minecraft mc) {
    if(motion.isEmpty())return;
    if(mc.world==null||mc.player==null){motion="";return;}
    try {
      double seconds=(System.nanoTime()-start)/1e9;
      if(motion.equals("pan")) {
        double phase=Math.min(1,seconds/8)*Math.PI*2;
        mc.player.prevYaw=mc.player.yaw;mc.player.prevPitch=mc.player.pitch;
        mc.player.yaw=yaw+(float)(Math.sin(phase)*8);mc.player.pitch=pitch+(float)(Math.sin(phase)*2);
        if(seconds>8){mc.player.yaw=mc.player.prevYaw=yaw;mc.player.pitch=mc.player.prevPitch=pitch;motion="";}
      } else if(motion.equals("menu")||motion.equals("library")) {
        if(mc.currentScreen==null){motion="";return;}
        double t=seconds<2?seconds/2:Math.max(0,1-(seconds-2)/2);
        int scroll=(int)Math.round(distance*t);
        if(motion.equals("menu"))field(target,"scroll",(double)scroll);
        else {Method m=target.getClass().getDeclaredMethod("scroll",int.class);m.setAccessible(true);m.invoke(target,scroll);}
        if(distance==0 && motion.equals("menu")) {
          var screen=(PowerOptionsScreen)target;
          var rows=(List<?>)field(screen,"rows");
          var settings=new ArrayList<Object>();
          for(Object row:rows)if(call(row,"setting")!=null)settings.add(row);
          if(!settings.isEmpty() && seconds<4) {
            Object row=settings.get(Math.min(settings.size()-1,(int)(seconds)));
            int py=(int)call(screen,"top")+(int)call(row,"y")+10;
            int px=screen.width/2;
            Mouse.setCursorPosition(px*mc.displayWidth/screen.width,mc.displayHeight-py*mc.displayHeight/screen.height-1);
          } else Mouse.setCursorPosition(0,0);
        }
        if(seconds>4)motion="";
      } else if(motion.equals("freecam")) {
        double t=Math.min(1,seconds/9),u=.5-.5*Math.cos(Math.PI*t);
        // Move the real detached camera out of the gate and above the building.
        double px=x+33*u,py=y+4+23*u,pz=z-20-28*u;
        target.getClass().getMethod("setCameraPosition",double.class,double.class,double.class).invoke(target,px,py,pz);
        float yaw=(float)Math.toDegrees(Math.atan2(-(x-px),z-pz));
        float pitch=(float)Math.toDegrees(Math.atan2(py-(y+9),Math.hypot(x-px,z-pz)));
        target.getClass().getMethod("setCameraRotation",float.class,float.class,float.class).invoke(target,pitch,yaw+180,0f);
        if(seconds>10)motion="";
      }
    }catch(Exception e){motion="";try{Validation.log("GALLERY MOTION FAILED "+e);}catch(Exception ignored){}throw new RuntimeException(e);}
  }
  private static void creative(Minecraft mc,String view) throws Exception {
    CreativeVehicleChecks.mode(mc,"CREATIVE");mc.setScreen(new net.minecraft.class_585(mc.player));
    Object screen=mc.currentScreen;
    field(screen,"creative_normalGUI",view.equals("survival"));
    if(view.equals("survival")) {
      for(int i=0;i<27;i++)mc.player.inventory.main[i+9]=new ItemStack(new int[]{1,5,20,4,17,35,44,50,45}[i%9],64,0);
      for(int i=0;i<4;i++)mc.player.inventory.armor[i]=new ItemStack(313-i,1,0);
    } else {
      int index=Integer.parseInt(view);
      var registry=Class.forName("local.luke.power.creative.inventory.registry.TabRegistry");
      Object tab=registry.getMethod("getTabByIndex",int.class).invoke(null,index);
      field(screen,"creative_items",tab.getClass().getMethod("getItems").invoke(tab));
      field(screen,"creative_tabKey",tab.getClass().getMethod("getTranslationKey").invoke(tab));
      field(screen,"creative_tabIndex",index%7);field(screen,"creative_tabPage",index/7);field(screen,"creative_rowIndex",0);call(screen,"creative_updateMaxIndex");
    }
    Mouse.setCursorPosition(0,0);
  }
}
