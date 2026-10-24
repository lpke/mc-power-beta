package local.luke.power.recipes.planner;

import java.util.*;

/** Bounded assignment. One cell never combines stacks with different native components. */
public final class RecipeMatcher {
  private RecipeMatcher() {}
  public record Supply(RecipeStack representative,int count) {}
  /** A copied inventory snapshot, grouped once for repeated display-only queries. */
  public record PreparedSupply(List<Supply> supplies) {
    public PreparedSupply { supplies=List.copyOf(supplies);if(supplies.size()>45)throw new IllegalArgumentException("Invalid recipe supply bounds"); }
  }
  public record Match(List<Supply> supplies,int[] cells,int amount) {
    public Match { supplies=List.copyOf(supplies);cells=cells.clone(); }
    @Override public int[] cells(){return cells.clone();}
  }
  public static Match match(List<RecipeIngredient> cells,List<RecipeStack> source,int amount,List<String> required) {
    return matchPrepared(cells,prepare(source),amount,required);
  }
  public static PreparedSupply prepare(List<RecipeStack> source) {
    if(source.size()>45)throw new IllegalArgumentException("Invalid recipe supply bounds");
    LinkedHashMap<String,Supply> groups=new LinkedHashMap<>();
    for(RecipeStack stack:source)if(stack!=null&&stack.usable()) {
      Supply old=groups.get(stack.identity());
      int count=old==null?stack.count():Math.addExact(old.count(),stack.count());
      groups.put(stack.identity(),new Supply(stack,count));
    }
    return new PreparedSupply(new ArrayList<>(groups.values()));
  }
  public static Match matchPrepared(List<RecipeIngredient> cells,PreparedSupply source,int amount,List<String> required) {
    if(cells.size()>9||amount<1||amount>64||required.size()!=cells.size())throw new IllegalArgumentException("Invalid recipe matching bounds");
    List<Supply> supplies=source.supplies();int[][] candidates=new int[cells.size()][];
    int[] remaining=supplies.stream().mapToInt(Supply::count).toArray(),result=new int[cells.size()];Arrays.fill(result,-1);
    List<Integer> order=new ArrayList<>();
    for(int cell=0;cell<cells.size();cell++) {
      RecipeIngredient ingredient=cells.get(cell);if(ingredient==null){candidates[cell]=new int[0];continue;}
      List<Integer> possible=new ArrayList<>();
      for(int group=0;group<supplies.size();group++) {
        RecipeStack stack=supplies.get(group).representative();
        if(supplies.get(group).count()>=amount&&stack.limit()>=amount&&ingredient.accepts(stack)
            &&(required.get(cell)==null||required.get(cell).equals(stack.identity())))possible.add(group);
      }
      if(possible.isEmpty())return null;candidates[cell]=possible.stream().mapToInt(Integer::intValue).toArray();order.add(cell);
    }
    order.sort(Comparator.comparingInt(cell->candidates[cell].length));
    if(order.isEmpty())return null;
    // Expand at most nine capacity units per identity. Each ingredient cell needs one unit.
    List<Integer> units=new ArrayList<>();
    for(int group=0;group<remaining.length;group++)for(int n=0;n<Math.min(9,remaining[group]/amount);n++)units.add(group);
    int[] owners=new int[units.size()];Arrays.fill(owners,-1);
    Solver solver=new Solver(candidates,units,owners,result);
    for(int cell:order)if(!solver.assign(cell,new boolean[units.size()]))return null;
    return new Match(supplies,result,amount);
  }
  private static final class Solver {
    final int[][] candidates;final List<Integer> units;final int[] owners,result;
    Solver(int[][] candidates,List<Integer> units,int[] owners,int[] result){this.candidates=candidates;this.units=units;this.owners=owners;this.result=result;}
    boolean assign(int cell,boolean[] visited) {
      for(int group:candidates[cell])for(int unit=0;unit<units.size();unit++) {
        if(units.get(unit)!=group||visited[unit])continue;visited[unit]=true;
        if(owners[unit]<0||assign(owners[unit],visited)){owners[unit]=cell;result[cell]=group;return true;}
      }
      return false;
    }
  }
}
