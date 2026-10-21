package local.luke.power.offhand;

import net.minecraft.screen.slot.Slot;

/** Shared access to the native shifted slot hit test and the last rendered GUI pointer. */
public interface OffhandSlotPicker {
  Slot power$offhandSlotAt(int x, int y);
  int power$offhandPointerX();
  int power$offhandPointerY();
  void power$offhandPointer(int x, int y);
  boolean power$offhandWidgetAt(int x, int y);
}
