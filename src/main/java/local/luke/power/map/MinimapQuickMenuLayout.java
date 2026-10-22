package local.luke.power.map;

/** One layout supplies both drawing and hit testing for the inventory map controls. */
public final class MinimapQuickMenuLayout {
  public static final int ROW_HEIGHT = 20;
  public record Circle(int x, int y, int radius) {
    public boolean contains(int px, int py) { return (px-x)*(px-x)+(py-y)*(py-y)<=radius*radius; }
  }
  public record Rect(int x, int y, int width, int height) {
    public boolean contains(int px, int py) { return px>=x && py>=y && px<x+width && py<y+height; }
  }
  public record Layout(Circle gear, Rect menu) {
    public Rect row(int index) { return new Rect(menu.x+4, menu.y+4+index*ROW_HEIGHT, menu.width-8, ROW_HEIGHT); }
    public int rowAt(int x,int y) { for(int index=0;index<4;index++)if(row(index).contains(x,y))return index;return -1; }
  }
  private MinimapQuickMenuLayout() {}
  public static Layout at(MinimapGeometry.Layout map, boolean circular, int width, int height, int labelWidth) {
    double inset = circular ? map.size() * (1 - Math.sqrt(.5)) / 2 : 0;
    Circle gear = new Circle(Math.max(7, Math.min(width-8, (int)Math.round(map.x()+inset))),
        Math.max(7, Math.min(height-8, (int)Math.round(map.y()+inset))), 7);
    int menuWidth = Math.max(40, Math.min(width-8, labelWidth+30)), menuHeight = 88;
    int x = Math.max(4, Math.min(width-menuWidth-4, gear.x+9));
    int y = Math.max(4, Math.min(height-menuHeight-4, gear.y+9));
    return new Layout(gear,new Rect(x,y,menuWidth,menuHeight));
  }
}
