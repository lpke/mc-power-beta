package local.luke.power.camera;

import net.minecraft.util.math.Box;
import local.luke.power.input.HeadGeometry;
import local.luke.power.camera.client.FreecamController;
import local.luke.power.camera.client.SaveManager;

public class Freecam {
    public static final Box cameraBoundingBox = Box.create(-0.2d, -HeadGeometry.EYE_ABOVE_BASE, -0.2d, 0.2d, 0.2d, 0.2d);
    public static FreecamController freecamController = new FreecamController();
    public static SaveManager saveManager = new SaveManager();
}
