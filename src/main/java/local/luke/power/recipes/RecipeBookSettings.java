package local.luke.power.recipes;

public final class RecipeBookSettings {
  public boolean enabled=false;
  public boolean inventory=true;
  public boolean inventoryButton=true;
  public boolean pauseMenu=false;
  public int buttonStyle=0;
  // craftingOpen is shared; retain the old furnace field as a mirrored storage alias.
  public boolean craftingOpen=false,furnaceOpen=false;
  public boolean craftingFilter=false,furnaceFilter=false;
}
