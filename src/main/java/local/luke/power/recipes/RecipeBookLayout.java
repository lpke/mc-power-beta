package local.luke.power.recipes;

import local.luke.power.input.InventoryPanels.Rect;

/** Pinned26.3 logical GUI coordinates, shared by drawing and hit testing. */
public record RecipeBookLayout(int screenWidth,int screenHeight,boolean open,boolean standalone) {
  public boolean narrow(){return !standalone&&screenWidth<379;}
  public int offset(){return open&&!narrow()&&!standalone?77:0;}
  public int x(){return (screenWidth-147)/2-(narrow()||standalone?0:86);}
  public int y(){return (screenHeight-166)/2;}
  public int inventoryX(){return (screenWidth-176)/2+offset();}
  public int inventoryY(){return (screenHeight-166)/2;}
  public Rect background(){return new Rect(x(),y(),147,166);}
  public Rect tabs(int count){return new Rect(x()-32,y()+3,37,count*27);}
  public Rect search(){return new Rect(x()+25,y()+13,81,14);}
  public Rect magnifier(){return new Rect(x()+8,y()+13,17,14);}
  public Rect filter(){return new Rect(x()+110,y()+12,26,16);}
  public Rect cell(int index){return new Rect(x()+11+(index%5)*25,y()+31+(index/5)*25,25,25);}
  public Rect tab(int index){return new Rect(x()-30,y()+3+index*27,35,27);}
  public Rect previous(){return new Rect(x()+38,y()+137,12,17);}
  public Rect next(){return new Rect(x()+93,y()+137,12,17);}
  public Rect toggle(boolean furnace,int gridWidth){return new Rect(inventoryX()+(furnace?20:gridWidth==2?104:5),inventoryY()+(gridWidth==2?61:34),20,18);}
}
