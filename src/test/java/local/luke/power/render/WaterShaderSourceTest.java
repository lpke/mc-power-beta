package local.luke.power.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaterShaderSourceTest {
  @Test void preservesVersionProjectionAndFogInputs() {
    String source="#version 150\nvoid main() { vertexColor = Color; gl_Position = ProjMat * position; }";
    String extended=WaterShaderSource.extend(source);
    assertTrue(extended.startsWith("#version 150\n"));
    assertTrue(extended.contains("in vec4 PowerWaterColor;"));
    assertTrue(extended.contains("mix(Color, PowerWaterColor, PowerWaterBrightness)"));
    assertTrue(extended.contains("gl_Position = ProjMat * position;"));
  }
  @Test void rejectsIncompatibleShaderInsteadOfSilentlyReadingWrongVertexData() {
    assertThrows(IllegalStateException.class,()->WaterShaderSource.extend("#version 150\nother();"));
    assertThrows(IllegalStateException.class,()->WaterShaderSource.extend("vertexColor = Color;"));
  }
}
