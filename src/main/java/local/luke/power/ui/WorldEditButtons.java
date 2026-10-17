package local.luke.power.ui;

import java.util.List;
import net.modificationstation.stationapi.api.client.gui.widget.ButtonWidgetAttachedContext;

public interface WorldEditButtons {
  List<ButtonWidgetAttachedContext> power$editButtons();
  void power$renameOwner(WorldEditControls controls);
}
