package local.luke.power.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import local.luke.power.input.*;
import local.luke.power.map.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Photo tools stay on the photo screen, including nested waypoint/color editor drafts. */
public final class PhotoModeTools extends UiScreen implements PhotoModeHooks.Tools {
  private enum PhotoToggle implements MapTogglePanel.Toggle {
    PLAYER(null),ITEMS(MapToggle.ITEMS),MOBS(MapToggle.MOBS),WAYPOINTS(MapToggle.WAYPOINTS),CHUNKS(MapToggle.CHUNKS),LIGHT(MapToggle.LIGHT);
    final MapToggle map;
    PhotoToggle(MapToggle map){this.map=map;}
    @Override public String label(){return map==null?"Show player":map.label;}
  }
  private enum Action {
    CREATE("Create waypoint"),COPY("Copy coordinates"),TELEPORT("Teleport"),DESTINATION("Set destination");
    final String label;
    Action(String label){this.label=label;}
  }
  private static final List<PhotoToggle> TOGGLES=List.of(PhotoToggle.values());
  private static final List<Action> WITH_TELEPORT=List.of(Action.values());
  private static final List<Action> WITHOUT_TELEPORT=List.of(Action.CREATE,Action.COPY,Action.DESTINATION);
  private static final PhotoModeTools INSTANCE=new PhotoModeTools();
  private static boolean registered;
  private final PhotoTerrainSurvey survey=new PhotoTerrainSurvey();
  private Screen host;
  private UiScreen layer;
  private PhotoControlsLayout controls;
  private Object visitWorld;
  private boolean items=true,mobs=true,chunks,spawnable,toggles,context,contextSafe;
  private int scroll,contextX,contextY,pointerX,pointerY,toggleLabelWidth;
  private Waypoint target;
  private List<Action> contextActions=WITHOUT_TELEPORT;
  private PhotoRay.Hit hovered;
  private String message="";
  private PhotoModeTools() {}
  public static boolean active() {
    return INSTANCE.host!=null && INSTANCE.minecraft!=null && INSTANCE.minecraft.currentScreen==INSTANCE.host
        && INSTANCE.minecraft.world==INSTANCE.visitWorld;
  }
  public static boolean waypoints() {return active()&&MapConfig.current().photoWaypoints;}
  public static boolean waypointsVisible() {return waypoints()&&INSTANCE.layer==null;}
  public static boolean hidden(Entity entity) {
    if(!active())return false;
    return entity instanceof net.minecraft.class_142 ? !INSTANCE.items
        : entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && !INSTANCE.mobs;
  }
  public static void tick(Minecraft mc) {
    if(!registered){PhotoModeHooks.register(INSTANCE);registered=true;}
    if(!active()) {if(INSTANCE.host!=null)INSTANCE.removed(INSTANCE.host);return;}
    if(!Display.isActive()){INSTANCE.hovered=null;return;}
    if(INSTANCE.layer!=null)INSTANCE.layer.tick();
    if(INSTANCE.context){
      if(INSTANCE.contextActions.contains(Action.TELEPORT)!=Maps.canTeleport(mc,INSTANCE.target))INSTANCE.context=false;
      else INSTANCE.contextSafe=INSTANCE.contextActions.contains(Action.TELEPORT)&&INSTANCE.safeTeleport();
    }
    INSTANCE.survey.tick(mc,INSTANCE.chunks,INSTANCE.spawnable);
    if(INSTANCE.chunks && INSTANCE.layer==null && !INSTANCE.context && !INSTANCE.owns(INSTANCE.pointerX,INSTANCE.pointerY))
      INSTANCE.hovered=PhotoPicking.pick(mc,INSTANCE.pointerX,INSTANCE.pointerY,2048);
    else INSTANCE.hovered=null;
  }
  public static void capture(Minecraft mc,float delta){
    if(active()&&INSTANCE.layer==null)PhotoPicking.capture(mc,delta);else PhotoPicking.clear();
  }
  public static void renderWorld(Minecraft mc,float delta) {
    if(!active())return;
    INSTANCE.survey.render(mc,delta);
    if(INSTANCE.chunks && INSTANCE.hovered!=null)
      local.luke.power.debug.DebugWorld.renderPhotoBorders(mc,delta,INSTANCE.hovered.x(),INSTANCE.hovered.z());
  }
  @Override public void init(Object screen,Object client) {
    var mc=(Minecraft)client;
    if(host!=screen || visitWorld!=mc.world) {
      if(host!=null)removed(host);
      host=(Screen)screen;visitWorld=mc.world;items=mobs=toggles=true;chunks=spawnable=context=false;
      message="";scroll=0;
    }
    super.init(mc,host.width,host.height);
    controls=PhotoControlsLayout.at(width,height,textRenderer::getWidth);
    toggleLabelWidth=MapTogglePanel.labelWidth(mc,TOGGLES);
    if(MapConfig.current().photoWaypoints)Maps.ensureWaypoints(mc);
    if(layer!=null)layer.init(mc,width,height);
    pointerX=Mouse.getX()*width/Math.max(1,mc.displayWidth);
    pointerY=height-Mouse.getY()*height/Math.max(1,mc.displayHeight)-1;
  }
  @Override public void removed(Object screen) {
    if(host!=screen)return;
    if(layer!=null){layer.removed();layer=null;}
    host=null;visitWorld=null;target=null;hovered=null;context=false;survey.clear();PhotoPicking.clear();
  }
  @Override public boolean blocked(Object screen){return screen==host&&(layer!=null||context);}
  @Override public boolean controlsHidden(Object screen){return screen==host&&layer!=null;}
  @Override public boolean renderButton(Object client,String label,int x,int y,int span,int h,
      int mx,int my,boolean enabled,Runnable overlay){
    if(client!=minecraft||!active())return false;
    button(label,x,y,span,h,mx,my,enabled,-1,overlay);return true;
  }
  @Override public boolean renderSlider(Object client,String label,int x,int y,int span,int mx,int my,double fraction){
    if(client!=minecraft||!active())return false;
    slider(label,x,y,span,mx,my,fraction);return true;
  }
  private PhotoControlsLayout footer(){return controls;}
  private int toggleY(){return footer().bottomY();}
  private MapTogglePanel.Layout<PhotoToggle> panel(){
    return MapTogglePanel.at(footer().toggleX(),footer().panelBottom(),Math.max(1,width-16),Math.max(8,footer().panelBottom()-48),
        toggleLabelWidth,scroll,TOGGLES);
  }
  private boolean owns(int x,int y){return host instanceof PhotoModeHooks.Camera camera&&camera.powerPhoto$ownsPointer(x,y)
      || inside(x,y,footer().toggleX(),toggleY(),footer().toggleWidth(),footer().buttonHeight())||toggles&&panel().bounds().contains(x,y);}
  private boolean enabled(MapToggle toggle){return switch(toggle){case ITEMS->items;case MOBS->mobs;
    case WAYPOINTS->MapConfig.current().photoWaypoints;case CHUNKS->chunks;case LIGHT->spawnable;default->false;};}
  private boolean enabled(PhotoToggle toggle){return toggle==PhotoToggle.PLAYER
      ?host instanceof PhotoModeHooks.Camera camera&&camera.powerPhoto$showPlayer():enabled(toggle.map);}
  private void toggle(PhotoToggle toggle){
    if(toggle==PhotoToggle.PLAYER){if(host instanceof PhotoModeHooks.Camera camera)camera.powerPhoto$showPlayer(!camera.powerPhoto$showPlayer());}
    else toggle(toggle.map);
  }
  private void toggle(MapToggle toggle){
    switch(toggle){case ITEMS->items=!items;case MOBS->mobs=!mobs;
      case WAYPOINTS->{var settings=MapConfig.copy();settings.photoWaypoints=!settings.photoWaypoints;MapConfig.queued(settings);if(settings.photoWaypoints)Maps.ensureWaypoints(minecraft);}
      case CHUNKS->{chunks=!chunks;hovered=null;survey.clear();}
      case LIGHT->{spawnable=!spawnable;survey.clear();}default->{}}
  }
  private void showLayer(Screen screen){
    if(layer!=null)layer.removed();
    if(screen==host || screen==null){layer=null;return;}
    if(!(screen instanceof UiScreen ui))throw new IllegalArgumentException("Photo editor must support modal layers");
    layer=ui;ui.layerNavigation(this::showLayer);ui.init(minecraft,width,height);
  }
  private void edit(Waypoint point){Maps.ensureWaypoints(minecraft);showLayer(new WaypointEditorScreen(host,point));}
  @Override public boolean key(Object screen,char character,int key) {
    if(screen!=host)return false;
    if(layer!=null){layer.layerKey(character,key);return true;}
    if(context){if(key==Keyboard.KEY_ESCAPE)context=false;return true;}
    if(key==Keyboard.KEY_ESCAPE || key==Keyboard.KEY_Q || key==Keyboard.KEY_E || key==Keyboard.KEY_SPACE
        || key==Keyboard.KEY_W || key==Keyboard.KEY_A || key==Keyboard.KEY_S || key==Keyboard.KEY_D)return false;
    if(Keyboard.isRepeatEvent()||!Display.isActive())return true;
    featureKey(key);return true;
  }
  private void featureKey(int key){
    if(Bindings.matches(MapKeys.WORLD_WAYPOINTS,key)||Bindings.matches(MapKeys.MAP_WAYPOINTS,key))toggle(MapToggle.WAYPOINTS);
    else if(Bindings.matches(MapKeys.GRID,key))toggle(MapToggle.CHUNKS);
    else if(Bindings.matches(MapKeys.RADAR,key))toggle(MapToggle.MOBS);
    else if(Bindings.matches(MapKeys.WAYPOINT,key))edit(MapKeys.newWaypoint());
    else if(Bindings.matches(MapKeys.CLEAR_DESTINATION,key))Maps.destination(null);
    else if(Bindings.matches(GameplayKeys.LIGHT,key))toggle(MapToggle.LIGHT);
    else if(Bindings.matches("key.power_controls.take_screenshot",key)&&host instanceof PhotoModeHooks.Camera camera)camera.powerPhoto$screenshot();
    else if(Bindings.down(local.luke.power.debug.DebugKeys.MODIFIER)
        && Bindings.matches(local.luke.power.debug.DebugKeys.binding(local.luke.power.debug.DebugAction.BORDERS),key))toggle(MapToggle.CHUNKS);
    else local.luke.power.render.RenderControls.photoPress(minecraft,key);
  }
  private List<Action> actions(){return contextActions;}
  private boolean safeTeleport(){
    if(target==null||!Maps.canTeleport(minecraft,target)||target.y<1||target.y>125
        ||MapSampler.loaded(minecraft.world,target.x>>4,target.z>>4)==null)return false;
    int x=target.x,y=target.y,z=target.z;
    for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
      if(MapSampler.loaded(minecraft.world,(x+dx)>>4,(z+dz)>>4)==null)return false;
    Block support=Block.BLOCKS[minecraft.world.getBlockId(x,y-1,z)];
    if(support==null||!support.method_1623()||support.id==81)return false;
    var box=support.method_1624(minecraft.world,x,y-1,z);
    if(box==null||box.maxY!=y||box.minX>x||box.maxX<x+1||box.minZ>z||box.maxZ<z+1)return false;
    for(int dy=0;dy<2;dy++) {
      int id=minecraft.world.getBlockId(x,y+dy,z);
      if(id==0)continue;
      Block block=Block.BLOCKS[id];
      if(block==null||block.field_1900==net.minecraft.block.Material.WATER
          ||block.field_1900==net.minecraft.block.Material.LAVA||block.method_1624(minecraft.world,x,y+dy,z)!=null)return false;
    }
    // Adjacent fire/cactus/lava can damage a normally sized player at the destination.
    for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
      if(MapSampler.loaded(minecraft.world,(x+dx)>>4,(z+dz)>>4)==null)return false;
      for(int dy=-1;dy<=1;dy++){int id=minecraft.world.getBlockId(x+dx,y+dy,z+dz);if(id==10||id==11||id==51||id==81)return false;}
    }
    return true;
  }
  private void action(int index){
    if(index==2&&!safeTeleport())return;
    context=false;click();
    switch(index){case 0->{
      var point=MapKeys.newWaypoint();point.x=target.x;point.y=target.y;point.z=target.z;point.dimension=target.dimension;
      edit(point);
    }case 1->{try{
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(target.coordinates()),null);message="Coordinates copied";
    }catch(RuntimeException failure){message="Clipboard is unavailable";}}
      case 2->{
        var world=minecraft.world;var player=minecraft.player;
        Maps.teleport(minecraft,target);
        if(minecraft.world==world&&minecraft.player==player&&Math.abs(player.x-target.x-.5)<.000001
            &&Math.abs(player.z-target.z-.5)<.000001&&Math.abs(player.boundingBox.minY-target.y)<.000001)
          world.method_193(player,false);
        minecraft.setScreen(host instanceof local.luke.power.controls.tweaks.photomode.PhotoModeScreen photo?photo.previousScreen:null);
      }
      case 3->{Maps.ensureWaypoints(minecraft);Maps.destination(target);}default->{}}
  }
  @Override public boolean mouse(Object screen,int x,int y,int button,boolean down,int wheel) {
    if(screen!=host)return false;
    pointerX=x;pointerY=y;
    // A native camera/widget gesture retains its originating press through release.
    if(layer==null&&!context&&host instanceof PhotoModeHooks.Camera camera&&camera.powerPhoto$dragging())return false;
    if(layer!=null){
      UiScreen current=layer;
      TextDrag.event(host,x,button,down,Display.isActive());
      if(wheel!=0)current.layerWheel(wheel);
      if(button>=0){if(down)current.layerPress(x,y,button);else current.layerRelease(x,y,button);}
      else current.layerRelease(x,y,-1);
      return true;
    }
    if(context){if(button>=0&&down){if(button==0&&inside(x,y,contextX,contextY,142,actions().size()*18))action(actions().get((y-contextY)/18).ordinal());else context=false;}return true;}
    if(toggles&&panel().bounds().contains(x,y)){
      var layout=panel();
      if(wheel!=0)scroll=layout.scroll(wheel);
      if(button==0&&down){int row=layout.rowAt(x,y);if(row>=0){click();toggle(layout.toggle(row));}}
      return true;
    }
    if(inside(x,y,footer().toggleX(),toggleY(),footer().toggleWidth(),footer().buttonHeight())){if(button==0&&down){click();toggles=!toggles;}return true;}
    if(owns(x,y))return false;
    if(button==2&&down){
      target=waypoints()?WaypointMarkers.hovered(minecraft,x,y):null;
      if(target==null){var hit=PhotoPicking.pick(minecraft,x,y,8192);if(hit==null){message="No loaded terrain under pointer";return true;}
        target=MapKeys.newWaypoint();target.x=hit.x();target.y=Math.min(127,hit.y()+1);target.z=hit.z();
      }else target=target.copy();
      contextActions=Maps.canTeleport(minecraft,target)?WITH_TELEPORT:WITHOUT_TELEPORT;
      contextSafe=contextActions.contains(Action.TELEPORT)&&safeTeleport();context=true;
      contextX=Math.max(0,Math.min(width-142,x));contextY=Math.max(0,Math.min(height-actions().size()*18,y));return true;
    }
    if(button>=3&&down){featureKey(button-100);return true;}
    return false;
  }
  @Override public void renderScene(Object screen,int x,int y) {
    if(screen==host&&layer==null)WaypointMarkers.render(minecraft,x,y);
  }
  @Override public void render(Object screen,int x,int y,float delta) {
    if(screen!=host)return;
    if(layer!=null){layer.render(x,y,delta);return;}
    int mx=layer!=null||context?-1000:x,my=layer!=null||context?-1000:y;
    button(toggles?"Hide Toggles":"Show Toggles",footer().toggleX(),toggleY(),footer().toggleWidth(),footer().buttonHeight(),mx,my,true);
    if(toggles)MapTogglePanel.draw(minecraft,panel(),mx,my,this::enabled);
    if(!message.isEmpty())text(fit(message,Math.max(1,width-170)),8,11,0xffffff);
    if(context){fill(contextX,contextY,contextX+142,contextY+actions().size()*18,0xf0202020);
      boolean safe=contextSafe;
      for(int i=0;i<actions().size();i++){
        if(inside(x,y,contextX,contextY+i*18,142,18))fill(contextX,contextY+i*18,contextX+142,contextY+(i+1)*18,0xff444444);
        var action=actions().get(i);
        text(action.label,contextX+5,contextY+i*18+5,action==Action.TELEPORT&&!safe?0x666666:0xffffff);
      }
    }
  }
}
