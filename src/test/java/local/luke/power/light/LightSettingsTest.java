package local.luke.power.light;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LightSettingsTest {
  @Test void defaultsUseBetaBoundaryAndAllowTwoColourExtremes() {
    LightSettings s=new LightSettings();s.validate();
    assertFalse(s.enabled);assertEquals(0,s.lightSource);
    assertEquals(0xFF5555,s.color(7));assertEquals(0x55FF55,s.color(8));
    s.lowColor="1234ab";s.highColor="#FEDCBA";s.greenFrom=16;s.validate();
    assertEquals(0x1234AB,s.color(15));s.greenFrom=0;s.validate();assertEquals(0xFEDCBA,s.color(0));
  }
  @Test void rejectsInvalidColourRangeAndNonFiniteSize() {
    for(String color:new String[]{null,"", "red","#FFF","#FFFFFFFF","#GG0000"})
      assertThrows(IllegalArgumentException.class,()->LightSettings.rgb(color));
    LightSettings s=new LightSettings();s.radius=25;assertThrows(IllegalArgumentException.class,s::validate);
    s.radius=12;s.greenFrom=17;assertThrows(IllegalArgumentException.class,s::validate);
    s.greenFrom=8;s.textSize=Double.NaN;assertThrows(IllegalArgumentException.class,s::validate);
  }
}
