package local.luke.power.worldedit.core;
import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.worldedit.config.WorldOverride;
import org.junit.jupiter.api.Test;
class AccessPolicyTest {
  @Test void cheatsGateEveryOverride() {
    for(boolean cheats:new boolean[]{false,true}) for(boolean enabled:new boolean[]{false,true}) for(WorldOverride world:WorldOverride.values())
      assertEquals(cheats && enabled && world != WorldOverride.DISABLED,AccessPolicy.allows(cheats,enabled,world));
  }
  @Test void unknownSavedValuesInherit() {
    assertEquals(WorldOverride.INHERIT,WorldOverride.decode(-1)); assertEquals(WorldOverride.INHERIT,WorldOverride.decode(100));
  }
}
