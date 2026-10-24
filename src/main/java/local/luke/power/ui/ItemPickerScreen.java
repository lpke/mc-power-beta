package local.luke.power.ui;

import com.google.gson.JsonPrimitive;
import java.util.*;
import java.util.function.Consumer;
import local.luke.power.config.Setting;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Searchable native icons with an isolated selection, shared by every item/block setting. */
public final class ItemPickerScreen extends UiScreen {
  public record Options(boolean instant, java.util.function.Predicate<ItemCatalogue.Entry> accepts) {
    public Options { Objects.requireNonNull(accepts); }
    public static Options standard() { return new Options(false, entry -> true); }
  }
  private final Options options;
  private final Screen parent;
  private final String title;
  private final ItemCatalogue.Domain domain;
  private final boolean multiple;
  private final Consumer<List<String>> accepted;
  private final List<ItemCatalogue.Entry> catalogue;
  private final LinkedHashSet<String> selected;
  private final Set<String> original;
  private final DraftExitPrompt exitPrompt = new DraftExitPrompt();
  private final TextInput search=new TextInput("",128);
  private final ScrollBar scrollbar=new ScrollBar();
  private final ItemIcons icons=new ItemIcons();
  private List<ItemCatalogue.Entry> shown;
  private boolean selectedOnly;
  private int scroll;
  private String error="";

  public ItemPickerScreen(Screen parent,String title,ItemCatalogue.Domain domain,
      Collection<String> selected,boolean multiple,Consumer<List<String>> accepted) {
    this(parent,title,domain,selected,multiple,Options.standard(),accepted);
  }
  public ItemPickerScreen(Screen parent,String title,ItemCatalogue.Domain domain,
      Collection<String> selected,boolean multiple,Options options,Consumer<List<String>> accepted) {
    if(multiple && options.instant())throw new IllegalArgumentException("Instant selection requires a single item");
    this.options=options;
    this.parent=parent;this.title=title;this.domain=domain;this.multiple=multiple;this.accepted=accepted;
    this.selected=new LinkedHashSet<>(selected);original=Set.copyOf(selected);catalogue=new ArrayList<>(ItemCatalogue.entries(domain).stream().filter(options.accepts()).toList());
    for(String value:selected) if(catalogue.stream().noneMatch(e->e.value().equals(value))) {
      try{var entry=ItemCatalogue.resolve(domain,value);catalogue.add(entry);}catch(IllegalArgumentException ignored){}
    }
    filter();
  }
  private int span(){return Math.min(500,width-24);}
  private int left(){return (width-span())/2;}
  private int right(){return left()+span();}
  private int bottom(){return height-58;}
  private ScrollBar.Track track(){return new ScrollBar.Track(right()-4,66,Math.max(0,bottom()-66),shown.size()*24,scroll);}
  public void init(){Keyboard.enableRepeatEvents(true);}
  public void removed(){Keyboard.enableRepeatEvents(false);scrollbar.release();}
  private void filter(){
    String query=search.text().strip().toLowerCase(Locale.ROOT);
    record Match(ItemCatalogue.Entry entry, int score) {}
    shown=catalogue.stream().filter(e->!selectedOnly || selected.contains(e.value()))
        .map(e->new Match(e,e.searchScore(query)))
        .filter(m->m.score()<FuzzySearch.NONE)
        .sorted(Comparator.comparingInt(Match::score))
        .map(Match::entry).toList();
  }
  private void choose(String value){
    if(multiple) {if(!selected.remove(value)){try{resolve(value);}catch(IllegalArgumentException failure){error=failure.getMessage();return;}selected.add(value);}}
    else {selected.clear();selected.add(value);}
    error="";filter();if(options.instant())save();
  }
  private ItemCatalogue.Entry resolve(String value) {
    var entry=ItemCatalogue.resolve(domain,value);
    if(!options.accepts().test(entry))throw new IllegalArgumentException("This item is unavailable in this picker");
    return entry;
  }
  private void custom(){
    var empty=new JsonPrimitive("");
    Setting entry=new Setting("picker.custom","","","","Custom ID",
        domain==ItemCatalogue.Domain.FILTER?"Enter an item ID or identifier. Numeric IDs can include metadata, such as 35:4.":"Enter a numeric ID or a registered identifier, such as minecraft:stone.",
        Setting.Kind.TEXT,empty,empty,0,0,1,List.of(),false);
    entry.validator(v->resolve(v.getAsString()));
    showScreen(new ValueScreen(entry,options.instant()?parent:this,()->{
      var chosen=resolve(entry.value.getAsString());
      if(catalogue.stream().noneMatch(e->e.value().equals(chosen.value())))catalogue.add(chosen);
      if(!multiple)selected.clear();selected.add(chosen.value());filter();error="";if(options.instant())accepted.accept(List.copyOf(selected));
    }));
  }
  private void save(){
    if(!multiple && selected.isEmpty())return;
    try{for(String value:selected)if(!original.contains(value))resolve(value);accepted.accept(List.copyOf(selected));showScreen(parent);}
    catch(IllegalArgumentException e){error=Objects.toString(e.getMessage(),"Invalid selection");}
  }
  private void leave(){
    scrollbar.release();exitPrompt.leave(!options.instant() && !selected.equals(original),()->showScreen(parent));
  }
  protected void keyPressed(char c,int key){
    if(exitPrompt.key(key))return;
    if(key==Keyboard.KEY_ESCAPE){leave();return;}
    if(key==Keyboard.KEY_RETURN || key==Keyboard.KEY_NUMPADENTER){save();return;}
    String before=search.text();search.key(c,key);if(!before.equals(search.text())){scroll=0;filter();}
  }
  public void onMouseEvent(){super.onMouseEvent();layerWheel(Mouse.getEventDWheel());}
  @Override public void layerWheel(int wheel){if(!exitPrompt.active() && wheel!=0)scroll=(int)Math.max(0,Math.min(track().maximum(),scroll-Math.signum(wheel)*24));}
  protected void mouseClicked(int x,int y,int button){
    if(exitPrompt.click(this,x,y,button,this::save,()->showScreen(parent)))return;
    if(button!=0 && button!=1)return;
    search.focused=inside(x,y,left(),38,span()-96,18);
    if(search.focused){String before=search.text();search.click(x,button);if(!before.equals(search.text())){scroll=0;filter();}return;}
    if(button!=0)return;
    if(scrollbar.press(track(),x,y)){scroll=(int)scrollbar.drag(track(),y,true);return;}
    if(inside(x,y,right()-90,37,90,20)){click();selectedOnly=!selectedOnly;scroll=0;filter();return;}
    if(inside(x,y,left(),66,span()-10,Math.max(0,bottom()-66))){
      int row=(y-66+scroll)/24;if(row<shown.size()){click();choose(shown.get(row).value());}return;
    }
    if(inside(x,y,left(),height-30,76,20)){click();custom();}
    else if(multiple && inside(x,y,left()+80,height-30,44,20)){click();selected.clear();filter();}
    else if(!options.instant() && inside(x,y,right()-156,height-30,76,20)){click();save();}
    else if(inside(x,y,right()-76,height-30,76,20)){click();leave();}
  }
  public void render(int mx,int my,float delta){
    int pointerX=mx,pointerY=my;
    if(exitPrompt.active()){mx=-1;my=-1;}
    scroll=(int)Math.max(0,Math.min(track().maximum(),scrollbar.drag(track(),my,!exitPrompt.active() && Mouse.isButtonDown(0))));
    renderBackground();drawCenteredTextWithShadow(textRenderer,title,width/2,16,0xffffff);
    input(search,left(),38,span()-96,mx,my,"Search name or ID...");
    button(selectedOnly?"Selected":"All",right()-90,37,90,20,mx,my,true);
    clip(left(),66,span()-8,Math.max(0,bottom()-66));String tip="";
    int first=scroll/24,last=Math.min(shown.size(),(scroll+Math.max(0,bottom()-66)+23)/24);
    for(int i=first;i<last;i++){
      var entry=shown.get(i);int y=66+i*24-scroll;boolean chosen=selected.contains(entry.value());
      boolean hover=inside(mx,my,left(),y,span()-10,24) && my>=66 && my<bottom();
      if(chosen || hover)fill(left(),y,right()-10,y+23,chosen?0x80445566:0x50333333);
      icons.draw(this,minecraft,entry,left()+5,y+4);
      text(fit(entry.name(),span()-62),left()+28,y+8,hover?0xffffa0:0xeeeeee);
      if(chosen)text("+",right()-23,y+8,0xffff88);
      if(hover)tip=entry.tooltip();
    }
    unclip();scrollbar.render(this,track());
    if(shown.isEmpty())text("No matches.",left()+5,75,0xaaaaaa);
    text(fit(error.isEmpty()?selected.size()+" selected. Hover for IDs.":error,span()),left(),height-47,error.isEmpty()?0xaaaaaa:0xff8888);
    button("Custom ID",left(),height-30,76,20,mx,my,true);
    if(multiple)button("Clear",left()+80,height-30,44,20,mx,my,!selected.isEmpty());
    if(!options.instant())button("Done",right()-156,height-30,76,20,mx,my,multiple || !selected.isEmpty());
    button("Cancel",right()-76,height-30,76,20,mx,my,true);
    if(!exitPrompt.active() && !tip.isEmpty())tooltip(tip,mx,my);
    exitPrompt.render(this,"Save changes to this selection?",pointerX,pointerY);
  }
}
