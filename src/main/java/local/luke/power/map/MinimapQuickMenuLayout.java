package local.luke.power.map;

/** One layout supplies drawing and hit testing; the selected corner chooses the inward edge. */
public final class MinimapQuickMenuLayout {
  public static final int ROW_HEIGHT = 20, ROWS = 5, BUTTON_SIZE = 16;
  public record Rect(int x, int y, int width, int height) {
    public boolean contains(int px, int py) { return px>=x && py>=y && px<x+width && py<y+height; }
  }
  public record Layout(Rect gear, Rect menu) {
    public Rect row(int index) { return new Rect(menu.x+4, menu.y+4+index*ROW_HEIGHT, menu.width-8, ROW_HEIGHT); }
    public int rowAt(int x,int y) { for(int index=0;index<ROWS;index++)if(row(index).contains(x,y))return index;return -1; }
  }
  private MinimapQuickMenuLayout() {}
  public static Layout at(MinimapGeometry.Layout map, boolean circular, int corner, int width, int height, int labelWidth) {
    boolean left=corner%2==0, top=corner<2;
    // Keep the whole button beyond the circle's diagonal edge. Square maps
    // retain their existing corner placement; window clamping still applies.
    double inset=circular?map.size()*(1-Math.sqrt(.5))/2-BUTTON_SIZE/2d-2:0;
    int cx=(int)Math.round(map.x()+(left?map.size()-inset:inset));
    int cy=(int)Math.round(map.y()+(top?inset:map.size()-inset));
    Rect gear=new Rect(Math.max(0,Math.min(width-BUTTON_SIZE,cx-BUTTON_SIZE/2)),
        Math.max(0,Math.min(height-BUTTON_SIZE,cy-BUTTON_SIZE/2)),BUTTON_SIZE,BUTTON_SIZE);
    int menuWidth=Math.max(40,Math.min(width-8,labelWidth+30)),menuHeight=8+ROWS*ROW_HEIGHT;
    int x=left?gear.x+gear.width+2:gear.x-menuWidth-2;
    int y=top?gear.y:gear.y+gear.height-menuHeight;
    return new Layout(gear,new Rect(Math.max(4,Math.min(width-menuWidth-4,x)),
        Math.max(4,Math.min(height-menuHeight-4,y)),menuWidth,menuHeight));
  }
}
