package local.luke.power.recipes;

import local.luke.power.input.InventoryPanels.Rect;

/** Pinned26.3 logical GUI coordinates, shared by drawing and hit testing. */
public record RecipeBookLayout(int screenWidth,int screenHeight,boolean open,boolean standalone,int overhang) {
  public RecipeBookLayout(int screenWidth,int screenHeight,boolean open,boolean standalone){this(screenWidth,screenHeight,open,standalone,0);}
  public boolean narrow(){return !standalone&&screenWidth<379+overhang;}
  public int offset(){return open&&!narrow()&&!standalone?77+overhang/2:0;}
  public int x(){return standalone?(screenWidth-311)/2+30:(screenWidth-147)/2-(narrow()?0:86+overhang/2);}
  public int y(){return standalone?Math.max(46,Math.min((screenHeight-166)/2,screenHeight-194)):(screenHeight-166)/2;}
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
  public Rect detail(){return new Rect(x()+143,y(),138,166);}
  public Rect kindTab(int index){return new Rect(x()+34+index*108,y()-22,104,20);}
  public Rect done(){return new Rect((screenWidth-100)/2,screenHeight-26,100,20);}
  public static int pageAfterScroll(int page,int recipes,int amount){return Math.max(0,Math.min(Math.max(0,(recipes-1)/20),page-Integer.signum(amount)));}
  public Rect toggle(boolean furnace,int gridWidth){return new Rect(inventoryX()+(furnace?20:gridWidth==2?104:5),inventoryY()+(gridWidth==2?61:34),20,18);}
}
