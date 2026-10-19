package local.luke.power.config;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;

class ControlLinksTest {
  private Setting setting(String id) {
    boolean key=id.startsWith("keys.");
    return new Setting(id,"fixture","Camera","Controls",id,"",key?Setting.Kind.KEY:Setting.Kind.BOOLEAN,
        key?new JsonPrimitive(0):new JsonPrimitive(false),new JsonPrimitive(false),0,1,1,List.of(),false);
  }
  private ConfigSession session(List<Setting> settings) {
    var session=new ConfigSession();
    session.add(new Backend(){
      public String id(){return "fixture";}public List<Path> files(){return List.of();}
      public void validate(Map<String,JsonElement> values){}public void apply(Map<String,JsonElement> values){}
    },settings);
    return session;
  }
  @Test void removedMastersDoNotBreakLinksBetweenRetainedPreferencesAndUnboundActions() {
    for(var pair:Map.of("keys.Toggle Freecam","power_camera:config.speed",
        "keys.Auto-walk (toggle)","tweaks.autoWalkHoldToWalk",
        "keys.Auto-mine (toggle)","tweaks.autoMineAnnounceToggle",
        "keys.key.powerbeta.free_look","tweaks.freeLookToggle",
        "keys.power_building.hotbar.base","tweaks.hotbar.overlay",
        "keys.power_building.hotbar.scroll","tweaks.hotbar.reverseScroll").entrySet()) {
      var key=setting(pair.getKey());var preference=setting(pair.getValue());var session=session(List.of(key,preference));
      assertSame(preference,ControlLinks.related(session,key),key.id);
      assertEquals(List.of(preference),ControlLinks.settings(session,key));
      assertEquals(List.of(key),ControlLinks.controls(session,preference));
      assertTrue(ControlLinks.enabled(session,key));
    }
  }
  @Test void sharedSprintRemainsAvailableWithoutRetiredFeatureSwitches() {
    var key=setting("keys.power_creative.sprint");
    assertTrue(ControlLinks.enabled(session(List.of(key)),key));
  }
  @Test void groundSprintLinksRetainedTuningAndUsesTheWorldRule() {
    var key=setting("keys.powerbeta.sprint");
    var activation=setting("sprint.activation");
    var speed=setting("sprint.speed");
    var rule=setting("rules.world.allow_sprinting");
    var session=session(List.of(key,activation,speed,rule));
    assertFalse(ControlLinks.enabled(session,key));
    assertFalse(SettingAccess.visible(session,key));
    rule.value=new JsonPrimitive(true);
    assertTrue(ControlLinks.enabled(session,key));
    assertTrue(SettingAccess.visible(session,key));
    assertSame(activation,ControlLinks.related(session,key));
    assertTrue(ControlLinks.settings(session,key).containsAll(List.of(activation,speed,rule)));
    assertEquals(List.of(key),ControlLinks.controls(session,rule));
    assertTrue(ControlLinks.enabled(session(List.of(key,activation,speed)),key));
  }
}
