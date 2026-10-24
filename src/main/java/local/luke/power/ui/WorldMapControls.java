package local.luke.power.ui;

import java.util.*;
import java.util.function.ToIntFunction;

/** Bottom navigation shares measured drawing and pointer bounds. */
record WorldMapControls(List<Button> buttons, Box scale) {
  static final int TEXT_PADDING = 8;
  enum Action { TOGGLES, WAYPOINTS, SETTINGS, LAYER, DIMENSION, CENTER, GO_TO, CLOSE }
  record Box(int x, int y, int width, int height) {
    boolean contains(int px, int py) { return px >= x && py >= y && px < x + width && py < y + height; }
    boolean intersects(Box other) { return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y; }
    Box translated(int dx, int dy) { return new Box(x + dx, y + dy, width, height); }
  }
  record Button(Action action, String label, Box bounds) {}
  static WorldMapControls at(int left, int width, int height, String[] labels, ToIntFunction<String> measure) {
    if (labels.length != Action.values().length) throw new IllegalArgumentException("Missing map control labels");
    int[] sizes=Arrays.stream(labels).mapToInt(label->Math.max(42,measure.applyAsInt(label)+TEXT_PADDING)).toArray();
    int available=Math.max(1,width-16);
    for(int i=0;i<5;i++)sizes[i]=Math.min(sizes[i],available);
    fit(sizes,5,8,available);
    int leftWidth=total(sizes,0,5),rightWidth=total(sizes,5,8);
    int rightY=height-24,leftY=rightY-(leftWidth+rightWidth+8<=available?0:22);
    var buttons=new ArrayList<Button>();
    var rows=leftRows(sizes,available);
    for(int i=0;i<rows.size();i++)row(buttons,labels,sizes,rows.get(i)[0],rows.get(i)[1],left+8,leftY-i*22);
    row(buttons,labels,sizes,5,8,left+width-8-rightWidth,rightY);
    return new WorldMapControls(List.copyOf(buttons),new Box(left+width-124,leftY-(rows.size()-1)*22-26,116,20));
  }
  int navigationTop() { return buttons.stream().mapToInt(button->button.bounds.y()).min().orElse(0); }
  /** Keep the toggle switch in the lowest left row and wrap complete labels above it. */
  private static List<int[]> leftRows(int[] widths,int available) {
    if(total(widths,0,5)<=available)return List.of(new int[]{0,5});
    int split=-1,balance=Integer.MAX_VALUE;
    for(int i=1;i<5;i++) {
      int lower=total(widths,0,i),upper=total(widths,i,5);
      if(lower<=available&&upper<=available&&Math.abs(lower-upper)<balance) {
        split=i;balance=Math.abs(lower-upper);
      }
    }
    if(split>=0)return List.of(new int[]{0,split},new int[]{split,5});
    var rows=new ArrayList<int[]>();
    for(int first=0;first<5;) {
      int end=first+1;
      while(end<5&&total(widths,first,end+1)<=available)end++;
      rows.add(new int[]{first,end});first=end;
    }
    return rows;
  }
  private static void fit(int[] widths,int start,int end,int available) {
    int total=total(widths,start,end);if(total<=available)return;
    int room=Math.max(end-start,available-(end-start-1)*4),original=total-(end-start-1)*4,used=0;
    for(int i=start;i<end;i++) {
      int width=i==end-1?room-used:Math.max(1,widths[i]*room/original);
      widths[i]=width;used+=width;
    }
  }
  private static int total(int[] widths,int start,int end) {
    int total=(end-start-1)*4;for(int i=start;i<end;i++)total+=widths[i];return total;
  }
  private static void row(List<Button> buttons,String[] labels,int[] sizes,int start,int end,int x,int y) {
    for(int i=start;i<end;i++) {buttons.add(new Button(Action.values()[i],labels[i],new Box(x,y,sizes[i],18)));x+=sizes[i]+4;}
  }
  Button at(int x,int y) { return buttons.stream().filter(button->button.bounds.contains(x,y)).findFirst().orElse(null); }
  Button button(Action action) { return buttons.stream().filter(button->button.action==action).findFirst().orElseThrow(); }
}
