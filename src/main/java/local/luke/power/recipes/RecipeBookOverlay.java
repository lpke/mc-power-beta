package local.luke.power.recipes;

import java.util.*;
import local.luke.power.input.*;
import local.luke.power.input.InventoryPanels.Rect;
import local.luke.power.ui.TextInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Recipe panel, variant chooser and search share the same geometry and input owner. */
public final class RecipeBookOverlay {
  private final Screen screen;
  private final Minecraft mc;
  private final boolean standalone;
  private RecipeContext context;
  private RecipeBookModel model;
  private final BookDrawing draw=new BookDrawing();
  private final TextInput search=new TextInput("",50);
  private boolean open,filter;
  private int category,page,ticks,generation;
  private List<List<RecipeDefinition>> groups=List.of();
  private List<RecipeDefinition> popup=List.of();
  private int popupX,popupY;
  private RecipeDefinition ghost,detail,lastUnavailable,lastRecipe;
  private String message="";
  public RecipeBookOverlay(Screen screen,Minecraft mc,RecipeContext context,boolean standalone) {
    this.screen=screen;this.mc=mc;this.context=context;this.standalone=standalone;
    var state=RecipeBookConfig.current();boolean furnace=context.kind==RecipeDefinition.Kind.FURNACE;
    open=standalone||(furnace?state.furnaceOpen:state.craftingOpen);filter=furnace?state.furnaceFilter:state.craftingFilter;
    refreshGeometry();
  }
  public boolean enabled() {
    var state=RecipeBookConfig.current();
    return mc.player!=null&&mc.world!=null&&state.enabled&&(standalone?state.pauseMenu:state.inventory)
        &&!(context.width==2&&mc.player instanceof PlayerMode mode&&mode.power$creative());
  }
  public RecipeBookLayout layout(){return new RecipeBookLayout(screen.width,screen.height,open&&enabled(),standalone);}
  public boolean replacesNative(){return enabled()&&open&&layout().narrow();}
  public boolean rendersCarried(){return !standalone&&enabled()&&open&&!InventoryPanels.modalOther(screen,this);}
  public boolean owns(int x,int y){var l=layout();return enabled()&&(l.toggle(context.kind==RecipeDefinition.Kind.FURNACE,context.width).contains(x,y)
      ||open&&(l.narrow()||l.background().contains(x,y)||l.tabs(tabs().length).contains(x,y)||!popup.isEmpty()||detail!=null));}
  public void tick() {
    if(!enabled()){generation++;closeInput();InventoryPanels.update(screen,0,List.of());return;}
    if(!org.lwjgl.opengl.Display.isActive()||InventoryPanels.modalOther(screen,this)){generation++;closeInput();ghost=null;}
    if(open&&!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)&&!Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))ticks++;
    if(open){if(model==null)model=new RecipeBookModel();model.tick(mc,context);rebuild();}
    refreshGeometry();
  }
  private int[] tabs(){return context.kind==RecipeDefinition.Kind.FURNACE?new int[]{0,1,2,3}:new int[]{0,3,1,4,2};}
  private void rebuild(){if(model==null)return;groups=model.groups(context,category,search.text(),filter);page=Math.min(page,Math.max(0,(groups.size()-1)/20));}
  private void refreshGeometry() {
    if(!enabled()){InventoryPanels.update(screen,0,List.of());return;}
    var l=layout();List<Rect> bounds=new ArrayList<>();
    if(!standalone)bounds.add(l.toggle(context.kind==RecipeDefinition.Kind.FURNACE,context.width));
    if(open){if(l.narrow())bounds.add(new Rect(0,0,screen.width,screen.height));else{bounds.add(l.background());bounds.add(l.tabs(tabs().length));}}
    InventoryPanels.update(screen,l.offset(),bounds);
    boolean modal=!popup.isEmpty()||detail!=null;InventoryPanels.modal(screen,this,modal);
    if(!InventoryPanels.modalOther(screen,this))InventoryTextInput.focus(screen,search.focused||modal);
  }
  private void click(){mc.soundManager.method_2009("random.click",1,1);}
  private void state(){if(!standalone)RecipeBookConfig.state(context.kind==RecipeDefinition.Kind.FURNACE,open,filter);refreshGeometry();}
  private void closeInput(){search.focused=false;popup=List.of();detail=null;InventoryPanels.modal(screen,this,false);if(!InventoryPanels.modalOther(screen,this))InventoryTextInput.focus(screen,false);}
  public void removed(){generation++;closeInput();InventoryPanels.update(screen,0,List.of());}
  public void kind(RecipeDefinition.Kind kind){generation++;context=RecipeContext.viewer(mc,kind);category=page=0;ghost=null;detail=null;lastRecipe=null;popup=List.of();model=null;filter=false;tick();}
  public void render(int mx,int my) {
    if(!enabled()||InventoryPanels.modalOther(screen,this))return;
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
    try {
      GL11.glTranslatef(0,0,180);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
      var l=layout();
      if(!standalone){var toggle=l.toggle(context.kind==RecipeDefinition.Kind.FURNACE,context.width);draw.image(mc,toggle.contains(mx,my)?"button_highlighted":"button",toggle.x(),toggle.y(),20,18);}
      if(!open){if(!standalone)renderGhost(mx,my,l);return;}
      draw.image(mc,"recipe_book",l.x(),l.y(),147,166,1,1,147,166,256,256);
      draw.search(mc,screen,search,l);
      boolean hover=l.filter().contains(mx,my);String prefix=context.kind==RecipeDefinition.Kind.FURNACE?"furnace_filter":"filter";
      String suffix=filter?"_enabled":"_disabled";draw.image(mc,prefix+suffix+(hover?"_highlighted":""),l.filter().x(),l.filter().y(),26,16);
      int[] tabs=tabs();
      for(int i=0;i<tabs.length;i++){var r=l.tab(i);boolean selected=category==tabs[i];draw.image(mc,selected?"tab_selected":"tab",r.x()+(selected?-2:0),r.y(),35,27);
        int[] icons=tabIcons(tabs[i]);for(int j=0;j<icons.length;j++)draw.item(mc,icon(icons[j]),r.x()+(selected?-2:0)+(icons.length==1?9:j==0?3:14),r.y()+5,1,false);}
      String tooltip=null;
      for(int i=0;i<20&&page*20+i<groups.size();i++) {
        var variants=groups.get(page*20+i);var recipe=cycling(variants);var r=l.cell(i);boolean available=variants.stream().anyMatch(model::available);
        draw.image(mc,"slot_"+(variants.size()>1?"many_":"")+(available?"craftable":"uncraftable"),r.x(),r.y(),25,25);
        boolean same=variants.size()>1&&variants.stream().allMatch(value->value.output.itemId==recipe.output.itemId&&value.output.getDamage()==recipe.output.getDamage());
        if(same)draw.item(mc,recipe.output,r.x()+5,r.y()+5,1,false);
        draw.item(mc,recipe.output,r.x()+(same?3:4),r.y()+(same?3:4),1,false);
        if(r.contains(mx,my))tooltip=recipe.name+(variants.size()>1?"\nRight-click for more":"")+(!writable()?"\nView recipe":"");
      }
      int pages=Math.max(1,(groups.size()+19)/20);
      if(pages>1){if(page>0)arrow(l.previous(),"page_backward",mx,my);if(page+1<pages)arrow(l.next(),"page_forward",mx,my);
        String label=(page+1)+"/"+pages;mc.textRenderer.draw(label,l.x()+73-mc.textRenderer.getWidth(label)/2,l.y()+141,0xffffff);}
      if(hover)tooltip=filter?"Showing craftable":"Showing all";
      for(int i=0;i<tabs.length;i++)if(l.tab(i).contains(mx,my))tooltip=tabName(tabs[i]);
      if(!standalone&&!l.narrow()){String ghostTooltip=renderGhost(mx,my,l);if(ghostTooltip!=null)tooltip=ghostTooltip;}
      if(!popup.isEmpty()||detail!=null) {
        GL11.glPushMatrix();
        try {
          // Native item rendering writes depth. Modal icons must clear the book's item layer.
          GL11.glTranslatef(0,0,200);
          tooltip=null;
          if(!popup.isEmpty())tooltip=renderPopup(mx,my);
          if(detail!=null)tooltip=renderDetail(mx,my);
        } finally {GL11.glPopMatrix();}
      }
      if(InventoryPanels.modalOther(screen,this))return;
      renderCarried(mx,my);
      if(!message.isEmpty())draw.tooltip(mc,message,l.x(),l.y()+166,screen.width,screen.height);
      else if(tooltip!=null)draw.tooltip(mc,tooltip,mx,my,screen.width,screen.height);
    }finally{GL11.glPopMatrix();GL11.glPopAttrib();}
  }
  private void arrow(Rect r,String name,int mx,int my){draw.image(mc,name+(r.contains(mx,my)?"_highlighted":""),r.x(),r.y(),r.width(),r.height());}
  private void renderCarried(int mx,int my){
    if(!rendersCarried()||mx<0||my<0)return;
    var carried=mc.player.inventory.getCursorStack();if(carried==null)return;
    GL11.glPushMatrix();
    try{GL11.glTranslatef(0,0,400);draw.item(mc,carried,mx-8,my-8,1,true);}finally{GL11.glPopMatrix();}
  }
  private RecipeDefinition cycling(List<RecipeDefinition> values){return values.get((ticks/30)%values.size());}
  private boolean writable(){return !standalone&&context.writable(mc);}
  private int[] tabIcons(int tab){if(tab==0)return new int[]{345};if(context.kind==RecipeDefinition.Kind.FURNACE)return switch(tab){case 1->new int[]{320};case 2->new int[]{1};default->new int[]{265};};
    return switch(tab){case 1->new int[]{45};case 2->new int[]{331};case 3->new int[]{258,283};default->new int[]{327,260};};}
  private String tabName(int tab){if(tab==0)return "Search";if(context.kind==RecipeDefinition.Kind.FURNACE)return switch(tab){case 1->"Food";case 2->"Blocks";default->"Miscellaneous";};return switch(tab){case 1->"Building blocks";case 2->"Redstone";case 3->"Equipment";default->"Miscellaneous";};}
  private ItemStack icon(int id){return id>=0&&id<net.minecraft.Item.ITEMS.length&&net.minecraft.Item.ITEMS[id]!=null?new ItemStack(id,1,0):null;}
  private String renderGhost(int mx,int my,RecipeBookLayout l) {
    if(ghost==null)return null;var cells=ghost.layout(context.width,context.height);if(cells==null)return null;
    String tooltip=null;
    int ingredient=0;for(int i=0;i<cells.size();i++){if(cells.get(i)==null)continue;var slot=context.inputs.get(i);int x=l.inventoryX()+slot.x,y=l.inventoryY()+slot.y;
      draw.rectangle(x,y,16,16,0x30ff0000);var choices=ghost.icons.get(ghost.shaped?(i%context.width-(ghost.width<context.width/2f?(int)Math.floor(context.width/2f-ghost.width/2f):0))+(i/context.width-(ghost.height<context.height/2f?(int)Math.floor(context.height/2f-ghost.height/2f):0))*ghost.width:ingredient++);
      if(!choices.isEmpty())draw.item(mc,choices.get((ticks/30)%choices.size()),x,y,1,false);draw.rectangle(x,y,16,16,0x30ffffff);
      if(new Rect(x,y,16,16).contains(mx,my)&&!choices.isEmpty())tooltip=itemName(choices.get((ticks/30)%choices.size()));
    }
    var out=context.output;int x=l.inventoryX()+out.x,y=l.inventoryY()+out.y;draw.rectangle(x-4,y-4,24,24,0x30ff0000);draw.item(mc,ghost.output,x,y,1,false);
    draw.rectangle(x,y,16,16,0x30ffffff);draw.decorations(mc,ghost.output,x,y);
    if(new Rect(x,y,16,16).contains(mx,my))tooltip=itemName(ghost.output);
    if(context.kind==RecipeDefinition.Kind.FURNACE&&model!=null&&!model.fuels.isEmpty()) {
      var fuel=context.handler.method_2084(1);
      if(fuel.getStack()==null) {
        int fx=l.inventoryX()+fuel.x,fy=l.inventoryY()+fuel.y;var icon=model.fuels.get((ticks/30)%model.fuels.size());
        draw.rectangle(fx,fy,16,16,0x30ff0000);draw.item(mc,icon,fx,fy,1,false);draw.rectangle(fx,fy,16,16,0x30ffffff);
        if(new Rect(fx,fy,16,16).contains(mx,my))tooltip=itemName(icon);
      }
    }
    return open?tooltip:null;
  }
  private String itemName(ItemStack stack){return net.minecraft.client.resource.language.TranslationStorage.getInstance().get(stack.getTranslationKey()+".name");}
  private int popupColumns(){return popup.size()>16?5:4;}
  private Rect popupBounds(){int cols=Math.min(popupColumns(),popup.size()),rows=(popup.size()+popupColumns()-1)/popupColumns();return new Rect(popupX,popupY,cols*25+8,rows*25+8);}
  private Rect popupCell(int index){return new Rect(popupX+4+(index%popupColumns())*25,popupY+5+(index/popupColumns())*25,24,24);}
  private void openPopup(List<RecipeDefinition> variants,Rect source){generation++;popup=variants.stream().sorted(Comparator.comparing((RecipeDefinition value)->!model.available(value))).toList();int cols=Math.min(popupColumns(),popup.size()),rows=(popup.size()+popupColumns()-1)/popupColumns();
    var l=layout();popupX=source.x();popupY=source.y();
    // Native overlays move in recipe-cell steps around the book's centre.
    int right=l.x()+123,bottom=l.y()+146,top=l.y()-4;
    if(popupX+cols*25>right)popupX-=25*((popupX+cols*25-right)/25);
    if(popupY+rows*25>bottom)popupY-=25*(int)Math.ceil((popupY+rows*25-bottom)/25d);
    if(popupY<top)popupY-=25*(int)Math.ceil((popupY-top)/25d);
    popupX=Math.max(4,Math.min(screen.width-cols*25-12,popupX));popupY=Math.max(4,Math.min(screen.height-rows*25-12,popupY));search.focused=false;refreshGeometry();}
  private String renderPopup(int mx,int my){var b=popupBounds();draw.panel(mc,b.x(),b.y(),b.width(),b.height());String tooltip=null;
    for(int i=0;i<popup.size();i++){var recipe=popup.get(i);var r=popupCell(i);
      String texture=(context.kind==RecipeDefinition.Kind.FURNACE?"furnace_overlay":"crafting_overlay")+(model.available(recipe)?"":"_disabled")+(r.contains(mx,my)?"_highlighted":"");draw.image(mc,texture,r.x(),r.y(),24,24);
      int ox=recipe.shaped&&recipe.width<1.5f?(int)Math.floor(1.5f-recipe.width/2f):0;
      int oy=recipe.shaped&&recipe.height<1.5f?(int)Math.floor(1.5f-recipe.height/2f):0;
      for(int n=0;n<recipe.icons.size();n++){var icons=recipe.icons.get(n);if(icons.isEmpty())continue;
        int gx=recipe.kind==RecipeDefinition.Kind.FURNACE?1:recipe.shaped?n%recipe.width+ox:n%3;
        int gy=recipe.kind==RecipeDefinition.Kind.FURNACE?1:recipe.shaped?n/recipe.width+oy:n/3;
        draw.item(mc,icons.get((ticks/30)%icons.size()),r.x()+2+7*gx,r.y()+2+7*gy,.375f,false);
      }
      if(r.contains(mx,my))tooltip=recipe.name;
    }return tooltip;
  }
  private Rect detailBounds(){return new Rect(Math.max(4,(screen.width-146)/2),Math.max(4,(screen.height-104)/2),146,104);}
  private String renderDetail(int mx,int my){var b=detailBounds();draw.rectangle(0,0,screen.width,screen.height,0x80000000);draw.panel(mc,b.x(),b.y(),b.width(),b.height());
    String name=detail.name;while(mc.textRenderer.getWidth(name)>134&&!name.isEmpty())name=name.substring(0,name.length()-1);mc.textRenderer.draw(name,b.x()+6,b.y()+7,0x404040);
    String tip=null;for(int i=0;i<detail.ingredients.size();i++){int row=detail.shaped?i/detail.width:i/3,col=detail.shaped?i%detail.width:i%3;int x=b.x()+8+col*18,y=b.y()+25+row*18;
      draw.rectangle(x-1,y-1,18,18,0xff8b8b8b);var icons=detail.icons.get(i);if(!icons.isEmpty()){var icon=icons.get((ticks/30)%icons.size());draw.item(mc,icon,x,y,1,false);if(new Rect(x,y,16,16).contains(mx,my))tip=itemName(icon);}}
    mc.textRenderer.draw("->",b.x()+76,b.y()+48,0x404040);draw.item(mc,detail.output,b.x()+108,b.y()+43,1,true);mc.textRenderer.draw("Click outside to return",b.x()+7,b.y()+88,0x606060);
    return tip;
  }
  public boolean mouse(int x,int y,int button) {
    if(!enabled()||InventoryPanels.modalOther(screen,this))return false;var l=layout();
    if(detail!=null){detail=null;click();refreshGeometry();return true;}
    if(!popup.isEmpty()){if(button==0)for(int i=0;i<popup.size();i++)if(popupCell(i).contains(x,y)){var value=popup.get(i);popup=List.of();choose(value);refreshGeometry();return true;}popup=List.of();refreshGeometry();return true;}
    if(!standalone&&l.toggle(context.kind==RecipeDefinition.Kind.FURNACE,context.width).contains(x,y)){if(button==0)toggle();return true;}
    if(!open)return nativeClick(x,y);
    if(l.search().contains(x,y)||l.magnifier().contains(x,y)){if(button==0){if(!search.focused)generation++;search.focused=true;if(l.search().contains(x,y))search.click(x,button);refreshGeometry();}return true;}
    search.focused=false;refreshGeometry();
    if(l.filter().contains(x,y)){if(button==0){generation++;filter=!filter;page=0;click();state();rebuild();}return true;}
    int[] tabs=tabs();for(int i=0;i<tabs.length;i++)if(l.tab(i).contains(x,y)){if(button==0){generation++;category=tabs[i];page=0;click();rebuild();}return true;}
    if(l.previous().contains(x,y)&&page>0){if(button==0){generation++;page--;click();}return true;}
    if(l.next().contains(x,y)&&(page+1)*20<groups.size()){if(button==0){generation++;page++;click();}return true;}
    for(int i=0;i<20&&page*20+i<groups.size();i++)if(l.cell(i).contains(x,y)){var variants=groups.get(page*20+i);if(button==1&&variants.size()>1){click();openPopup(variants,l.cell(i));}else if(button==0)choose(cycling(variants));return true;}
    if(owns(x,y))return true;return nativeClick(x,y);
  }
  private boolean nativeClick(int x,int y) {
    var l=layout();List<net.minecraft.screen.slot.Slot> crafting=new ArrayList<>(context.inputs);crafting.add(context.output);
    if(context.kind==RecipeDefinition.Kind.FURNACE&&!context.inputs.isEmpty())crafting.add(context.handler.method_2084(1));
    for(var slot:crafting)if(new Rect(l.inventoryX()+slot.x-1,l.inventoryY()+slot.y-1,18,18).contains(x,y)) {
      ghost=null;lastUnavailable=null;lastRecipe=null;break;
    }
    message="";generation++;return false;
  }
  private void choose(RecipeDefinition recipe) {
    choose(recipe,Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
  }
  private void choose(RecipeDefinition recipe,boolean maximum) {
    click();message="";search.focused=false;popup=List.of();refreshGeometry();
    if(!writable()){detail=recipe;refreshGeometry();return;}
    if(lastUnavailable==recipe&&!model.available(recipe)){generation++;return;}
    lastRecipe=recipe;int token=++generation;
    try {
      boolean accepted=RecipeFills.fill(mc,context,recipe,maximum,()->enabled()&&mc.currentScreen==screen&&token==generation
          &&!InventoryPanels.modal(screen)&&!search.focused&&org.lwjgl.opengl.Display.isActive(),unavailable->{
        ghost=unavailable?recipe:null;lastUnavailable=unavailable?recipe:null;if(!unavailable&&layout().narrow()){open=false;state();}
      });
      if(!accepted){message="Not enough inventory space";ghost=recipe;}
    }catch(Exception failure){message="Recipe could not be placed safely";}
  }
  public void toggle() {generation++;open=!open;closeInput();message="";click();state();if(open)tick();}
  public boolean key(char character,int key) {
    if(!enabled()||InventoryPanels.modalOther(screen,this))return false;
    if(!popup.isEmpty()||detail!=null){if(key==Keyboard.KEY_ESCAPE){popup=List.of();detail=null;refreshGeometry();}return true;}
    if(!standalone&&!search.focused&&Bindings.matches("powerbeta.recipeBook",key)&&!Keyboard.isRepeatEvent()){toggle();return true;}
    if(!open)return false;
    if(key==Keyboard.KEY_ESCAPE){if(layout().narrow()){generation++;open=false;closeInput();state();return true;}return false;}
    if(search.focused){String before=search.text();search.key(character,key);if(!before.equals(search.text())){generation++;page=0;rebuild();}return true;}
    if(Bindings.matches(mc.options.chatKey,key)){generation++;search.focused=true;refreshGeometry();return true;}
    if(lastRecipe!=null&&(key==Keyboard.KEY_SPACE||key==Keyboard.KEY_RETURN||key==Keyboard.KEY_NUMPADENTER)){
      choose(lastRecipe,local.luke.power.ui.TextEditing.shift());return true;
    }
    return false;
  }
}
