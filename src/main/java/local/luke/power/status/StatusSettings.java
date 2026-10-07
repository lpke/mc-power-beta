package local.luke.power.status;

import local.luke.power.input.TweakIndicators.Tweak;
import local.luke.power.light.LightSettings;

public final class StatusSettings {
  public boolean enabled = true;
  public int position = 8;
  public int offsetX, offsetY;
  public String textColor = "#FFFFFF";
  public int opacity = 100;
  public boolean fakeSneak = true, fastPlacement = true, placementRestriction = true,
      autoWalk = true, freeLook = true, cinematicCamera = true, freecamPlayerMovement = true;
  public boolean slabCompletion;
  public boolean cinematicCameraMessages;

  public boolean includes(Tweak tweak) {
    return switch (tweak) {
      case FAKE_SNEAK -> fakeSneak;
      case FAST_PLACEMENT -> fastPlacement;
      case PLACEMENT_RESTRICTION -> placementRestriction;
      case AUTO_WALK -> autoWalk;
      case FREE_LOOK -> freeLook;
      case CINEMATIC_CAMERA -> cinematicCamera;
      case FREECAM_PLAYER_MOVEMENT -> freecamPlayerMovement;
      case SLAB_COMPLETION -> slabCompletion;
    };
  }
  public void validate() {
    if (position < 0 || position > 8 || opacity < 0 || opacity > 100
        || Math.abs((long) offsetX) > 4096 || Math.abs((long) offsetY) > 4096)
      throw new IllegalArgumentException("Invalid active-tweaks position or opacity");
    LightSettings.rgb(textColor);
  }
  public int argb() { return Math.round(opacity * 255 / 100f) << 24 | LightSettings.rgb(textColor); }
}
