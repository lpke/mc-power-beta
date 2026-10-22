package local.luke.power.recipes;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeBookSettingsTest {
  @Test void visibilityIsSharedWhileCraftabilityFiltersRemainIndependent() {
    var settings=new RecipeBookSettings();
    settings.state(true,true,true);
    assertTrue(settings.craftingOpen);assertTrue(settings.furnaceOpen);
    settings.state(false,false,false);
    assertFalse(settings.craftingOpen);assertFalse(settings.furnaceOpen);
    assertTrue(settings.furnaceFilter);assertFalse(settings.craftingFilter);
    var restored=new Gson().fromJson(new Gson().toJson(settings),RecipeBookSettings.class);
    assertFalse(restored.craftingOpen);assertFalse(restored.furnaceOpen);assertTrue(restored.furnaceFilter);
  }
  @Test void existingSettingsRetainModernButtonAndBookState() {
    var settings=new Gson().fromJson("{\"craftingOpen\":true,\"furnaceFilter\":true}",RecipeBookSettings.class);
    assertEquals(0,settings.buttonStyle);assertTrue(settings.craftingOpen);assertTrue(settings.furnaceFilter);
  }
  @Test void subtleStyleRoundTripsWithTheGameGsonVersion() {
    var gson=new Gson();var settings=new RecipeBookSettings();settings.buttonStyle=1;
    assertEquals(1,gson.fromJson(gson.toJson(settings),RecipeBookSettings.class).buttonStyle);
  }
}
