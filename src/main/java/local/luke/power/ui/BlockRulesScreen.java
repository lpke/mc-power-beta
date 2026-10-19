package local.luke.power.ui;

import com.google.gson.*;
import java.util.*;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Block/tool rules and title palettes use independent drafts and the common catalog. */
public final class BlockRulesScreen extends UiScreen {
  private final PowerOptionsScreen parent;
  private final Setting setting;
  private final boolean vein;
  private JsonObject draft;
  private final JsonObject original;
  private final DraftExitPrompt exitPrompt = new DraftExitPrompt();
  private final ScrollBar scrollbar=new ScrollBar();
  private final ItemIcons icons=new ItemIcons();
  private final Map<String,ItemCatalogue.Entry> blocks=new HashMap<>();
  private final Map<String,String> toolTips=new HashMap<>();
  private int scroll;
  private String error="";

  public BlockRulesScreen(PowerOptionsScreen parent,Setting setting,boolean vein){
    this.parent=parent;this.setting=setting;this.vein=vein;draft=setting.value.getAsJsonObject().deepCopy();original=draft.deepCopy();refresh();
  }
  private int span(){return Math.min(520,width-24);}
  private int left(){return (width-span())/2;}
  private int right(){return left()+span();}
  private int bottom(){return height-62;}
  private int toolsX(){return right()-122;}
  private ScrollBar.Track track(){return new ScrollBar.Track(right()-3,62,Math.max(0,bottom()-62),draft.size()*26,scroll);}
  private List<String> keys(){return new ArrayList<>(draft.keySet());}
  private void refresh(){
    blocks.clear();toolTips.clear();
    for(var row:draft.entrySet()){
      try{blocks.put(row.getKey(),ItemCatalogue.resolve(ItemCatalogue.Domain.BLOCK,vein?row.getKey():row.getValue().getAsString()));}
      catch(IllegalArgumentException ignored){}
      if(vein){
        List<String> names=new ArrayList<>();
        for(var tool:row.getValue().getAsJsonArray()){
          try{var entry=ItemCatalogue.resolve(ItemCatalogue.Domain.ITEM,tool.getAsString());names.add(entry.name()+" ("+entry.id()+")");}
          catch(IllegalArgumentException e){names.add(tool.getAsString());}
        }
        toolTips.put(row.getKey(),names.isEmpty()?"Any item. Normal harvesting requirements still apply.":String.join("\n",names));
      }
    }
  }
  public void init(){Keyboard.enableRepeatEvents(true);}
  public void removed(){Keyboard.enableRepeatEvents(false);scrollbar.release();}
  private void add(){
    if(vein){
      minecraft.setScreen(new ItemPickerScreen(this,"Add vein blocks",ItemCatalogue.Domain.BLOCK,List.of(),true,values->{
        JsonObject next=draft.deepCopy();
        for(String value:values){String key=value.equals("74")?"73":value;if(!next.has(key))next.add(key,new JsonArray());}
        setting.validate(next);draft=next;refresh();
      }));
    }else{
      var empty=new JsonPrimitive("");
      Setting character=new Setting("picker.character","","","","Title character","Enter the character used in the title lettering.",Setting.Kind.TEXT,empty,empty,0,0,1,List.of(),false);
      character.validator(v->{String s=v.getAsString();if(s.length()!=1 || Character.isWhitespace(s.charAt(0)))throw new IllegalArgumentException("Enter one visible character");if(draft.has(s))throw new IllegalArgumentException("That character already has a block");if(draft.size()>=64)throw new IllegalArgumentException("Use up to 64 characters");});
      minecraft.setScreen(new ValueScreen(character,this,()->{draft.addProperty(character.value.getAsString(),1);refresh();}));
    }
  }
  private void editBlock(String key){
    String current=vein?key:draft.get(key).getAsString();
    minecraft.setScreen(new ItemPickerScreen(this,"Choose block",ItemCatalogue.Domain.BLOCK,List.of(current),false,values->{
      String value=values.get(0);JsonObject next=draft.deepCopy();
      if(vein){
        if(value.equals("74"))value="73";
        if(!value.equals(key) && next.has(value))throw new IllegalArgumentException("That block already has a rule");
        var tools=next.remove(key);next.add(value,tools);
      }else next.addProperty(key,Integer.parseInt(value));
      setting.validate(next);draft=next;refresh();
    }));
  }
  private void editTools(String key){
    List<String> values=new ArrayList<>();draft.getAsJsonArray(key).forEach(v->values.add(v.getAsString()));
    minecraft.setScreen(new ItemPickerScreen(this,"Required tools",ItemCatalogue.Domain.ITEM,values,true,selected->{
      JsonObject next=draft.deepCopy();JsonArray tools=new JsonArray();selected.forEach(v->tools.add(Integer.parseInt(v)));next.add(key,tools);
      setting.validate(next);draft=next;refresh();
    }));
  }
  private void save(){
    try{setting.validate(draft);setting.value=draft.deepCopy();parent.changed(setting);minecraft.setScreen(parent);}
    catch(IllegalArgumentException e){error=Objects.toString(e.getMessage(),"Invalid rule");}
  }
  private void leave(){
    scrollbar.release();exitPrompt.leave(!draft.equals(original),()->minecraft.setScreen(parent));
  }
  protected void keyPressed(char c,int key){
    if(exitPrompt.key(key))return;
    if(key==Keyboard.KEY_ESCAPE)leave();
    else if(key==Keyboard.KEY_RETURN || key==Keyboard.KEY_NUMPADENTER)save();
  }
  public void onMouseEvent(){super.onMouseEvent();int wheel=Mouse.getEventDWheel();if(!exitPrompt.active() && wheel!=0)scroll=(int)Math.max(0,Math.min(track().maximum(),scroll-Math.signum(wheel)*26));}
  protected void mouseClicked(int x,int y,int button){
    if(exitPrompt.click(this,x,y,button,this::save,()->minecraft.setScreen(parent)))return;
    if(button!=0)return;
    if(scrollbar.press(track(),x,y)){scroll=(int)scrollbar.drag(track(),y,true);return;}
    if(inside(x,y,left(),62,span()-10,Math.max(0,bottom()-62))){
      int index=(y-62+scroll)/26;List<String> keys=keys();if(index>=keys.size() || (y-62+scroll)%26>=22)return;
      String key=keys.get(index);
      if(x>=right()-30){draft.remove(key);refresh();}
      else if(vein && x>=toolsX())editTools(key);
      else editBlock(key);
      return;
    }
    if(inside(x,y,left(),height-30,86,20))add();
    else if(inside(x,y,left()+90,height-30,58,20)){draft=setting.defaultValue.getAsJsonObject().deepCopy();refresh();scroll=0;}
    else if(inside(x,y,right()-140,height-30,68,20))save();
    else if(inside(x,y,right()-68,height-30,68,20))leave();
  }
  public void render(int mx,int my,float delta){
    int pointerX=mx,pointerY=my;
    if(exitPrompt.active()){mx=-1;my=-1;}
    scroll=(int)Math.max(0,Math.min(track().maximum(),scrollbar.drag(track(),my,!exitPrompt.active() && Mouse.isButtonDown(0))));
    renderBackground();drawCenteredTextWithShadow(textRenderer,setting.label,width/2,16,0xffffff);
    text(fit(vein?"Choose blocks, then their required tools.":"Choose a block for each title character.",span()),left(),38,0xaaaaaa);
    clip(left(),62,span()-8,Math.max(0,bottom()-62));String tip="";List<String> keys=keys();
    for(int i=scroll/26;i<keys.size();i++){
      int y=62+i*26-scroll;if(y>=bottom())break;String key=keys.get(i);var block=blocks.get(key);
      int labelEnd=vein?toolsX()-4:right()-34;
      boolean over=inside(mx,my,left(),y,labelEnd-left(),22) && my>=62 && my<bottom();
      if(over)fill(left(),y,labelEnd,y+22,0x50333333);
      if(block!=null)icons.draw(this,minecraft,block,left()+4,y+3);
      String name=(vein?"":"'"+key+"'  ")+(block==null?"ID "+(vein?key:draft.get(key).getAsString()):block.name());
      text(fit(name,labelEnd-left()-30),left()+26,y+7,over?0xffffa0:0xeeeeee);
      if(vein){int count=draft.getAsJsonArray(key).size();button(count==0?"Any item":count+" tools",toolsX(),y+2,88,18,mx,my,true);}
      iconButton("remove",right()-30,y+2,20,mx,my,true);
      if(my>=62 && my<bottom()){
        if(over && block!=null)tip=block.tooltip()+(vein && key.equals("73")?"\nIncludes lit redstone ore, ID 74.":"");
        else if(vein && inside(mx,my,toolsX(),y+2,88,18))tip=toolTips.get(key);
        else if(inside(mx,my,right()-30,y+2,20,18))tip="Remove rule";
      }
    }
    unclip();scrollbar.render(this,track());
    if(keys.isEmpty())text("No blocks selected.",left()+4,72,0xaaaaaa);
    text(fit(error.isEmpty()?(vein?"No required tools means any item. Redstone includes lit ore.":"Changes are saved with Done."):error,span()),left(),height-48,error.isEmpty()?0xaaaaaa:0xff8888);
    button(vein?"Add blocks":"Add character",left(),height-30,86,20,mx,my,true);
    button("Defaults",left()+90,height-30,58,20,mx,my,true);
    button("Done",right()-140,height-30,68,20,mx,my,true);
    button("Cancel",right()-68,height-30,68,20,mx,my,true);
    if(!exitPrompt.active() && !tip.isEmpty())tooltip(tip,mx,my);
    exitPrompt.render(this,vein?"Save changes to these rules?":"Save changes to this palette?",pointerX,pointerY);
  }
}
