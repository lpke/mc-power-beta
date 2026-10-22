package local.luke.power.recipes.planner;

import java.util.*;

/** Full capacity and ingredient planning. This class never calls a native slot or recipe callback. */
public final class RecipeFillPlanner {
  private RecipeFillPlanner() {}
  public record Plan(List<RecipeStack> main,RecipeStack offhand,List<RecipeStack> grid,int amount,boolean ghost,RecipeStack output) {
    public Plan(List<RecipeStack> main,RecipeStack offhand,List<RecipeStack> grid,int amount,boolean ghost){this(main,offhand,grid,amount,ghost,null);}
    public Plan { main=Collections.unmodifiableList(new ArrayList<>(main));grid=Collections.unmodifiableList(new ArrayList<>(grid)); }
  }
  /** Preflight the complete crafted result before any native ingredient is consumed. */
  public static Plan receive(List<RecipeStack> main, RecipeStack offhand, List<RecipeStack> grid,
      RecipeStack result, int selected, boolean offhandWritable) {
    if (main.size() != 36 || selected < 0 || selected > 8 || grid.size() > 9)
      throw new IllegalArgumentException("Unsupported recipe inventory layout");
    ReturnState destination = new ReturnState(main, offhand, selected, offhandWritable);
    if (!destination.insert(result)) return null;
    return new Plan(destination.main, destination.offhand, grid, 0, false);
  }
  /** Return one native item per occupied ingredient without rearranging the remaining grid. */
  public static Plan removeOne(List<RecipeStack> originalMain,RecipeStack originalOffhand,List<RecipeStack> originalGrid,
      int selected,boolean offhandWritable,boolean matches) {
    if(originalMain.size()!=36||originalGrid.isEmpty()||originalGrid.size()>9||selected<0||selected>8)
      throw new IllegalArgumentException("Unsupported recipe inventory layout");
    if(!matches||originalGrid.stream().allMatch(Objects::isNull))return null;
    ReturnState returns=new ReturnState(originalMain,originalOffhand,selected,offhandWritable);
    List<RecipeStack> grid=new ArrayList<>(originalGrid);int remaining=Integer.MAX_VALUE;
    for(int i=0;i<grid.size();i++) {
      RecipeStack stack=grid.get(i);if(stack==null)continue;
      if(!returns.insert(stack.count(1)))return null;
      grid.set(i,stack.count(stack.count()-1));remaining=Math.min(remaining,stack.count()-1);
    }
    return new Plan(returns.main,returns.offhand,grid,remaining,false);
  }
  public static Plan plan(List<RecipeStack> originalMain,RecipeStack originalOffhand,List<RecipeStack> originalGrid,
      List<RecipeIngredient> cells,int selected,boolean offhandWritable,boolean matches,boolean maximum) {
    return plan(originalMain,originalOffhand,originalGrid,cells,selected,offhandWritable,matches,maximum,null);
  }
  public static Plan plan(List<RecipeStack> originalMain,RecipeStack originalOffhand,List<RecipeStack> originalGrid,
      List<RecipeIngredient> cells,int selected,boolean offhandWritable,boolean matches,boolean maximum,RecipeStack originalOutput) {
    if(originalMain.size()!=36||originalGrid.size()!=cells.size()||cells.size()>9||selected<0||selected>8)
      throw new IllegalArgumentException("Unsupported recipe inventory layout");
    if(originalOutput!=null&&cells.size()!=1)throw new IllegalArgumentException("Only furnace output is returned as an item");
    // Vanilla checks whether the original grid can be returned before any recipe is placed.
    ReturnState clear=new ReturnState(originalMain,originalOffhand,selected,offhandWritable);
    for(RecipeStack stack:originalGrid)if(!clear.insert(stack))return null;
    int offhandBeforeOutput=clear.offhand==null?0:clear.offhand.count();
    if(!clear.insert(originalOutput))return null;
    List<RecipeStack> supply=supply(originalMain,originalGrid,originalOutput,clear,offhandBeforeOutput);
    // Native matching accepts mirrored, offset and permuted inputs. Placement selects
    // ingredients again for the canonical layout, independently of those old positions.
    List<String> required=Collections.nCopies(cells.size(),null);int minimum=64;
    for(RecipeStack stack:originalGrid)if(stack!=null)minimum=Math.min(minimum,stack.count());
    RecipeMatcher.Match one=RecipeMatcher.match(cells,supply,1,required);
    if(one==null)return new Plan(clear.main,clear.offhand,Collections.nCopies(cells.size(),null),0,true);
    int low=1,high=maximum||matches?64:1;RecipeMatcher.Match best=one;
    while(low<=high){int middle=(low+high)>>>1;var match=RecipeMatcher.match(cells,supply,middle,required);if(match==null)high=middle-1;else{best=match;low=middle+1;}}
    // ServerPlaceRecipe checks this before both ordinary and maximum placement.
    // A matching grid that cannot grow must keep its existing quantities and layout.
    if(matches)for(RecipeStack stack:originalGrid)if(stack!=null
        &&Math.min(best.amount(),stack.limit())<stack.count()+1)
      return new Plan(originalMain,originalOffhand,originalGrid,minimum,false,originalOutput);
    if(!maximum&&matches)best=RecipeMatcher.match(cells,supply,minimum+1,required);
    if(best==null)return null;
    List<RecipeStack> main=new ArrayList<>(originalMain),left=new ArrayList<>(originalGrid),
        returnedOutput=new ArrayList<>(Collections.singletonList(originalOutput)),grid=new ArrayList<>(Collections.nCopies(cells.size(),null));
    int[] assigned=best.cells();
    for(int cell=0;cell<cells.size();cell++)if(assigned[cell]>=0) {
      RecipeStack template=best.supplies().get(assigned[cell]).representative();int needed=best.amount();
      // Existing inputs stay a source even if returning them would merge into an occupied offhand.
      needed=consume(left,template.identity(),needed);needed=consume(main,template.identity(),needed);
      needed=consume(returnedOutput,template.identity(),needed);
      if(needed!=0)throw new IllegalStateException("Recipe allocation changed while planning");
      grid.set(cell,template.count(best.amount()));
    }
    ReturnState returns=new ReturnState(main,originalOffhand,selected,offhandWritable);
    for(RecipeStack stack:left)if(!returns.insert(stack))return null;
    if(!returns.insert(returnedOutput.get(0)))return null;
    return new Plan(returns.main,returns.offhand,grid,best.amount(),false);
  }
  /** Finished furnace items become ingredient supply only where their full return reaches main storage. */
  public static List<RecipeStack> supply(List<RecipeStack> main,RecipeStack offhand,List<RecipeStack> grid,
      int selected,boolean offhandWritable,RecipeStack output) {
    if(main.size()!=36||grid.size()>9||selected<0||selected>8||output!=null&&grid.size()!=1)
      throw new IllegalArgumentException("Unsupported recipe inventory layout");
    ReturnState clear=new ReturnState(main,offhand,selected,offhandWritable);
    for(RecipeStack stack:grid)if(!clear.insert(stack))return supply(main,grid,null,clear,0);
    int before=clear.offhand==null?0:clear.offhand.count();
    if(!clear.insert(output))return supply(main,grid,null,clear,0);
    return supply(main,grid,output,clear,before);
  }
  private static List<RecipeStack> supply(List<RecipeStack> main,List<RecipeStack> grid,RecipeStack output,
      ReturnState clear,int offhandBeforeOutput) {
    List<RecipeStack> result=new ArrayList<>(main);result.addAll(grid);
    if(output!=null) {
      int toOffhand=(clear.offhand==null?0:clear.offhand.count())-offhandBeforeOutput;
      RecipeStack returned=output.count(output.count()-toOffhand);if(returned!=null)result.add(returned);
    }
    return result;
  }
  private static int consume(List<RecipeStack> source,String identity,int needed) {
    for(int i=0;i<source.size()&&needed>0;i++) {
      RecipeStack stack=source.get(i);if(stack==null||!stack.usable()||!stack.identity().equals(identity))continue;
      int amount=Math.min(stack.count(),needed);source.set(i,stack.count(stack.count()-amount));needed-=amount;
    }
    return needed;
  }
  private static final class ReturnState {
    final List<RecipeStack> main;RecipeStack offhand;final int selected;final boolean offhandWritable;
    ReturnState(List<RecipeStack> main,RecipeStack offhand,int selected,boolean offhandWritable){this.main=new ArrayList<>(main);this.offhand=offhand;this.selected=selected;this.offhandWritable=offhandWritable;}
    boolean insert(RecipeStack incoming) {
      if(incoming==null)return true;int left=incoming.count();
      left=merge(selected,incoming,left);
      if(offhandWritable&&offhand!=null&&offhand.identity().equals(incoming.identity())) {
        int amount=Math.min(left,Math.max(0,offhand.limit()-offhand.count()));offhand=offhand.count(offhand.count()+amount);left-=amount;
      }
      for(int i=0;i<36&&left>0;i++)left=merge(i,incoming,left);
      for(int i=0;i<36&&left>0;i++)if(main.get(i)==null){int amount=Math.min(left,incoming.limit());main.set(i,incoming.count(amount));left-=amount;}
      return left==0;
    }
    int merge(int index,RecipeStack incoming,int left) {
      RecipeStack existing=main.get(index);if(existing==null||!existing.identity().equals(incoming.identity()))return left;
      int amount=Math.min(left,Math.max(0,existing.limit()-existing.count()));main.set(index,existing.count(existing.count()+amount));return left-amount;
    }
  }
}
