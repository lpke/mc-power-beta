package local.luke.power.inventory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.inventory.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.recipe.SmeltingRecipeManager;
import net.minecraft.screen.slot.CraftingResultSlot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;


@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class ContainerBaseMixin extends Screen implements local.luke.power.input.InventoryDrag {

	@Override public void power$cancelItemDrag() {
		powerInventory_resetRightClickDragVariables();
		powerInventory_resetLeftClickDragVariables();
		powerInventory$shiftDouble.clear();
		powerInventory$gatherDouble.clear();
		powerInventory$dragOwnership.clear();
	}

	@Shadow
	protected int backgroundWidth;

	@Shadow
	protected int backgroundHeight;

	@Shadow
	public net.minecraft.screen.ScreenHandler container;

	@Shadow
	protected abstract Slot getSlotAt(int x, int y);

	@Shadow
	protected abstract boolean isPointOverSlot(Slot slot, int x, int Y);

	@Unique private final DoubleClick powerInventory$shiftDouble = new DoubleClick();
	@Unique private final DoubleClick powerInventory$gatherDouble = new DoubleClick();
	@Unique private final DragOwnership powerInventory$dragOwnership = new DragOwnership();
	@Unique private final java.util.Set<Slot> powerInventory$shiftVisited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

	@WrapMethod(method = {"mouseClicked", "mouseReleased"})
	private void powerInventory$ownedDrag(int x, int y, int button, Operation<Void> original) {
		if (minecraft.player != null && !powerInventory$dragOwnership.current(container, minecraft.player.inventory.getCursorStack())) {
			power$cancelItemDrag();
			return;
		}
		original.call(x, y, button);
		if (minecraft.player != null && (isLeftClickDragStarted || isRightClickDragStarted || isLeftClickDragMouseTweaksStarted)) {
			List<Slot> visited = new ArrayList<>(leftClickHoveredSlots);
			visited.addAll(rightClickHoveredSlots);
			visited.addAll(powerInventory$shiftVisited);
			if (isLeftClickDragStarted || isLeftClickDragMouseTweaksStarted) visited.add(lastLMBSlot);
			if (isRightClickDragStarted) visited.add(lastRMBSlot);
			powerInventory$dragOwnership.capture(container, minecraft.player.inventory.getCursorStack(), visited);
		} else powerInventory$dragOwnership.clear();
	}

	@Unique private boolean powerInventory$shiftDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	@Unique private boolean powerInventory$eligible() {
		return minecraft.world != null && minecraft.player != null && !minecraft.player.dead
				&& !(minecraft.player instanceof local.luke.power.input.PlayerMode mode && !mode.power$inventoryEditable())
				&& minecraft.player.health > 0 && minecraft.currentScreen == (Object) this
				&& minecraft.player.currentScreenHandler == container && org.lwjgl.opengl.Display.isActive();
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void powerInventory$checkGesture(int x, int y, float delta, CallbackInfo ci) {
		if (!powerInventory$eligible()) { power$cancelItemDrag(); return; }
		if (!powerInventory$shiftDouble.pending() && !powerInventory$gatherDouble.pending()) return;
		ItemStack cursor = minecraft.player.inventory.getCursorStack();
		var contents = InventoryStacks.data(cursor);
		if (!powerInventory$shiftDown() || !powerInventory$shiftDouble.owns(container, cursor, contents))
			powerInventory$shiftDouble.clear();
		if (!powerInventory$gatherDouble.owns(container, cursor, contents)) powerInventory$gatherDouble.clear();
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void powerInventory$close(CallbackInfo ci) { power$cancelItemDrag(); }

	@Unique private boolean powerInventory$shiftDoubleClick(Slot clicked, int button) {
		ItemStack cursor = minecraft.player.inventory.getCursorStack();
		if (button != 0 || !powerInventory$shiftDown()
				|| !Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.ShiftDoubleClickTransfer
				|| !QuickTransfers.eligible(minecraft, container) || !QuickTransfers.writable(clicked, cursor)) {
			powerInventory$shiftDouble.clear();
			return false;
		}
		long now = MouseClickClock.now();
		var contents = InventoryStacks.data(cursor);
		if (powerInventory$shiftDouble.second(container, clicked, cursor, contents, now)
				&& (!clicked.hasStack() || InventoryStacks.same(cursor, clicked.getStack()))) {
			power$cancelItemDrag();
			QuickTransfers.matching(minecraft, container, clicked, cursor);
			return true;
		}
		powerInventory$shiftDouble.clear();
		if (!InventoryStacks.same(cursor, clicked.getStack())) return false;
		powerInventory$gatherDouble.clear();
		powerInventory_resetRightClickDragVariables();
		powerInventory_resetLeftClickDragVariables();
		powerInventory$shiftDouble.start(container, clicked, cursor, contents, now);
		minecraft.interactionManager.clickSlot(container.syncId, clicked.id, 0, true, minecraft.player);
		// A held first press still supports the existing matching-item Shift-drag.
		leftClickMouseTweaksPersistentStack = cursor;
		isLeftClickDragMouseTweaksStarted = true;
		lastLMBSlotId = clicked.id;
		lastLMBSlot = clicked;
		return true;
	}

	@Unique private Slot slot;

	@Unique Slot lastRMBSlot = null;

	@Unique Slot lastLMBSlot = null;

	@Unique int lastRMBSlotId = -1;

	@Unique int lastLMBSlotId = -1;

	@Unique
	private ItemStack leftClickMouseTweaksPersistentStack = null;

	@Unique
	private ItemStack leftClickPersistentStack = null;

	@Unique
	private ItemStack rightClickPersistentStack = null;

	@Unique
	private boolean isLeftClickDragMouseTweaksStarted = false;

	@Unique
	private boolean isLeftClickDragStarted = false;

	@Unique
	private boolean isRightClickDragStarted = false;

	@Unique
	private final List<Slot> leftClickHoveredSlots = new ArrayList<>();

	@Unique final List<Slot> rightClickHoveredSlots = new ArrayList<>();

	@Unique Integer leftClickItemAmount;

	@Unique Integer rightClickItemAmount;

	@Unique final List<Integer> leftClickExistingAmount = new ArrayList<>();

	@Unique final List<Integer> rightClickExistingAmount = new ArrayList<>();

	@Unique List<Integer> leftClickAmountToFillPersistent = new ArrayList<>();

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	protected void powerInventory_mouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
		if (local.luke.power.input.InventoryPanels.modal(this) || local.luke.power.input.InventoryPanels.contains(this, mouseX, mouseY)) { power$cancelItemDrag(); ci.cancel(); return; }
		isLeftClickDragMouseTweaksStarted = false;
		Slot clickedSlot = this.getSlotAt(mouseX, mouseY);
		if (!powerInventory$eligible()) { power$cancelItemDrag(); ci.cancel(); return; }
		if (powerInventory$shiftDoubleClick(clickedSlot, button)) { ci.cancel(); return; }
		if (button != 0 || powerInventory$shiftDown()) powerInventory$gatherDouble.clear();

		/** - Handle crafting slot click */
		if (clickedSlot instanceof CraftingResultSlot) {
			/** - Handle ctrl click crafting */
			if (Config.INVENTORY_TWEAKS_CONFIG.CRAFTING_RESULT_CONFIG.EnableCtrlClickCrafting) {
				if (powerInventory_handleCtrlClickCrafting(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}

			/** - Handle right click crafting */
			if (Config.INVENTORY_TWEAKS_CONFIG.CRAFTING_RESULT_CONFIG.EnableRightClickCrafting) {
				if (powerInventory_handleRightClickCrafting(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}

			/** - Handle shift click crafting */
			if (Config.INVENTORY_TWEAKS_CONFIG.CRAFTING_RESULT_CONFIG.EnableShiftClickCrafting) {
				if (powerInventory_handleShiftClickCrafting(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}
		}

		/** - Check special click behavior for current screen */
		if (minecraft.currentScreen instanceof InventoryScreen) {
			/** - Handle shift click into armor slots */
			if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.EnableShiftClickingItemsIntoArmorSlots) {
				if (powerInventory_handleShiftClickIntoArmorSlots(mouseX, mouseY, button, clickedSlot)) {
					if (button == 0 && minecraft.player.inventory.getCursorStack() == null
							&& Config.INVENTORY_TWEAKS_CONFIG.MOUSE_TWEAKS_CONFIG.LMBTweakShiftClickAny)
						powerInventory$startShiftDrag(clickedSlot);
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}
		} else if (minecraft.currentScreen instanceof FurnaceScreen) {
			/** - Handle shift click into furnace */
			if (minecraft.world.isRemote && minecraft.player.inventory.getCursorStack() == null
					&& Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.EnableShiftClickingItemsIntoFurnaces) {
				if (powerInventory_handleShiftClickIntoFurnace(mouseX, mouseY, button, clickedSlot)) {
					/** - Handle if a button was clicked */
					super.mouseClicked(mouseX, mouseY, button);
					ci.cancel();
					return;
				}
			}
		}

		/** - Check if client is on a server */
		boolean isClientOnServer = minecraft.world.isRemote;

		/** - Right-click */
		if (button == 1) {
			boolean exitFunction = false;

			/** - Should click cancel Left-click + Drag */
			if (!powerInventory_cancelLeftClickDrag(isClientOnServer)) {

				/** - Handle Right-click */
				if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.EnableRightClickDrag) {
					exitFunction = powerInventory_handleRightClick(mouseX, mouseY, clickedSlot);
				}
			} else {
				exitFunction = true;
			}

			if (exitFunction) {
				/** - Handle if a button was clicked */
				super.mouseClicked(mouseX, mouseY, button);
				ci.cancel();
				return;
			}
		}

		/** - Left-click */
		if (button == 0) {
			boolean exitFunction = false;

			/** - Should click cancel Right-click + Drag */
			if (!powerInventory_cancelRightClickDrag(isClientOnServer)) {

				/** - Handle Left-click */
				ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
				if (cursorStack != null) {
					/** - Check for double left-click fill cursor stack (checking for second click close to time of first click) */
					if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.DoubleLeftClickItemPickup) {
						if (null != clickedSlot && false == clickedSlot.hasStack() && null != minecraft.world) {
							if (!powerInventory$shiftDown() && powerInventory$gatherDouble.second(container, clickedSlot, cursorStack, InventoryStacks.data(cursorStack), MouseClickClock.now())) {
								powerInventory$gatherDouble.clear();
								if (powerInventory_handleDoubleClickEmptyCursor(mouseX, mouseY, button, clickedSlot)) {
									/** - Handle if a button was clicked */
									super.mouseClicked(mouseX, mouseY, button);
									ci.cancel();
									return;
								}
							}
						}
					}

					if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.EnableLeftClickDrag) {
						exitFunction = powerInventory_handleLeftClickWithItem(cursorStack, clickedSlot, isClientOnServer);
					}
				} else {

					exitFunction = powerInventory_handleLeftClickWithoutItem(clickedSlot);
				}
			} else {
				exitFunction = true;
			}

			if (exitFunction) {
				/** - Handle if a button was clicked */
				super.mouseClicked(mouseX, mouseY, button);
				ci.cancel();
				return;
			}
		}
	}

	@Unique private List<Object> powerInventory$craftState() {
		List<Object> state = new ArrayList<>(container.slots.size() + 1);
		for (Object entry : container.slots) state.add(InventoryStacks.data(((Slot) entry).getStack()));
		state.add(InventoryStacks.data(minecraft.player.inventory.getCursorStack()));
		return state;
	}

	@Unique private boolean powerInventory_handleCtrlClickCrafting(int mouseX, int mouseY, int button, Slot clickedSlot) {
		boolean isCtrlKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
		/** - Ctrl-click */
		if (true == isCtrlKeyDown && clickedSlot.hasStack()) {
			ItemStack result = InventoryStacks.copy(clickedSlot.getStack(), 1);
			int maxStackSize = clickedSlot.getStack().getMaxCount();
			int numCrafted = 0;
			for (int craftingAttempts = 0; craftingAttempts < 256; craftingAttempts++) {
				var before = powerInventory$craftState();
				if (clickedSlot.hasStack() && InventoryStacks.same(result, clickedSlot.getStack()) && numCrafted < maxStackSize) {
					numCrafted += clickedSlot.getStack().count;
					this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, button, true, this.minecraft.player);
					if (before.equals(powerInventory$craftState())) break;
				} else {
					break;
				}
			}
			return true;
		}

		return false;
	}

	@Unique private boolean powerInventory_handleRightClickCrafting(int mouseX, int mouseY, int button, Slot clickedSlot) {
		/** - Right-click */
		if (button == 1 && clickedSlot.hasStack()) {
			/** - Abort and do normal shift key crafting if shift key is down */
			boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
			if (!isShiftKeyDown) {
				ItemStack result = InventoryStacks.copy(clickedSlot.getStack(), 1);
				int maxStackSize = clickedSlot.getStack().getMaxCount();
				int numCrafted = 0;
				for (int craftingAttempts = 0; craftingAttempts < 256; craftingAttempts++) {
					var before = powerInventory$craftState();
					if (clickedSlot.hasStack() && InventoryStacks.same(result, clickedSlot.getStack()) && numCrafted < maxStackSize) {
						numCrafted += clickedSlot.getStack().count;
						powerInventory_internalMouseClicked(mouseX, mouseY, button);
						if (before.equals(powerInventory$craftState())) break;
					} else {
						break;
					}
				}
				return true;
			}
		}

		return false;
	}

	@Unique private boolean powerInventory_handleShiftClickCrafting(int mouseX, int mouseY, int button, Slot clickedSlot) {
		boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
		if (true == isShiftKeyDown && clickedSlot.hasStack()) {
			ItemStack result = InventoryStacks.copy(clickedSlot.getStack(), 1);
			for (int craftingAttempts = 0; craftingAttempts < 256; craftingAttempts++) {
				var before = powerInventory$craftState();
				if (!clickedSlot.hasStack() || !InventoryStacks.same(result, clickedSlot.getStack())) break;
				powerInventory_internalMouseClicked(mouseX, mouseY, button);
				if (before.equals(powerInventory$craftState())) break;
			}
			return true;
		}

		return false;
	}

	@Unique private boolean powerInventory_handleShiftClickIntoArmorSlots(int mouseX, int mouseY, int button, Slot clickedSlot) {
		return powerInventory$shiftDown() && ArmorTransfers.equip(minecraft, container, clickedSlot, button);
	}

	@Unique private void powerInventory$startShiftDrag(Slot origin) {
		powerInventory_resetLeftClickDragVariables();
		isLeftClickDragMouseTweaksStarted = true;
		leftClickMouseTweaksPersistentStack = null;
		lastLMBSlotId = origin.id;
		lastLMBSlot = origin;
		powerInventory$shiftVisited.add(origin);
	}

	@Unique private boolean powerInventory_handleShiftClickIntoFurnace(int mouseX, int mouseY, int button, Slot clickedSlot) {
		boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
		if (isShiftKeyDown) {

			if (null != clickedSlot && clickedSlot.hasStack()) {
				FurnaceScreen furnaceScreen = (FurnaceScreen) minecraft.currentScreen;

				if (  (clickedSlot != ((Slot)furnaceScreen.container.slots.get(0)))
				   && (clickedSlot != ((Slot)furnaceScreen.container.slots.get(1)))
				   && (clickedSlot != ((Slot)furnaceScreen.container.slots.get(2)))
				) {
					try {
						ItemStack slotStack = clickedSlot.getStack();
						int shiftToSlot = -1;

						if (FabricLoader.getInstance().isModLoaded("stationapi")) {
							if (null != ModHelperStationAPI.getResultFor(slotStack)) {
								if (0 <= ModHelper.canItemFitInSlot(slotStack, ((Slot)furnaceScreen.container.slots.get(0)))) {
									shiftToSlot = 0;
								} else {
									shiftToSlot = -2;
								}
							} else if (0 < ModHelperStationAPI.getFuelTime(slotStack)) {
								if (0 <= ModHelper.canItemFitInSlot(slotStack, ((Slot)furnaceScreen.container.slots.get(1)))) {
									shiftToSlot = 1;
								} else {
									shiftToSlot = -2;
								}
							}
						} else {
							if (null != SmeltingRecipeManager.getInstance().craft(slotStack.getItem().id)) {
								if (0 <= ModHelper.canItemFitInSlot(slotStack, ((Slot)furnaceScreen.container.slots.get(0)))) {
									shiftToSlot = 0;
								} else {
									shiftToSlot = -2;
								}
							} else {
								FurnaceBlockEntity furnace = ((FurnaceScreenAccessor)furnaceScreen).getFurnaceBlockEntity();

								if (0 < ((FurnaceBlockEntityAccessor)furnace).powerInventory_getFuelTime(slotStack)) {
									if (0 <= ModHelper.canItemFitInSlot(slotStack, ((Slot)furnaceScreen.container.slots.get(1)))) {
										shiftToSlot = 1;
									} else {
										shiftToSlot = -2;
									}
								}
							}
						}

						if (-2 == shiftToSlot) {
							return true;
						} else if (0 <= shiftToSlot) {
							if (null != minecraft.player.inventory.getCursorStack()) {
								this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, button, false, this.minecraft.player);
								this.minecraft.interactionManager.clickSlot(this.container.syncId, ((Slot)furnaceScreen.container.slots.get(shiftToSlot)).id, button, false, this.minecraft.player);
								this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, button, false, this.minecraft.player);
							} else {
								this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, button, false, this.minecraft.player);
								this.minecraft.interactionManager.clickSlot(this.container.syncId, ((Slot)furnaceScreen.container.slots.get(shiftToSlot)).id, button, false, this.minecraft.player);
								if (null != minecraft.player.inventory.getCursorStack()) {
									this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, button, false, this.minecraft.player);
								}
							}

							return true;
						}
					} catch (Exception ex) {
						/* Do nothing */
					}
				}
			}
		}

		return false;
	}

	@Unique private boolean powerInventory_handleDoubleClickEmptyCursor(int mouseX, int mouseY, int button, Slot clickedSlot) {
		if (clickedSlot == null) return false;
		powerInventory_resetRightClickDragVariables();
		powerInventory_resetLeftClickDragVariables();
		GatherStacks.gather(minecraft, container, clickedSlot);
		return true;
	}

	@Unique private void powerInventory_internalMouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		if (button == 0 || button == 1) {
			Slot var4 = this.getSlotAt(mouseX, mouseY);
			int var5 = (this.width - this.backgroundWidth) / 2 + local.luke.power.input.InventoryPanels.offset(this);
			int var6 = (this.height - this.backgroundHeight) / 2;
			boolean var7 = mouseX < var5 || mouseY < var6 || mouseX >= var5 + this.backgroundWidth || mouseY >= var6 + this.backgroundHeight;
			int var8 = -1;
			if (var4 != null) {
				var8 = var4.id;
			}

			if (var7 && !(var4 instanceof local.luke.power.inventory.PlayerSideSlot)) {
				var8 = -999;
			}

			if (var8 != -1) {
				boolean var9 = var8 != -999 && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54));
				this.minecraft.interactionManager.clickSlot(this.container.syncId, var8, button, var9, this.minecraft.player);
			}
		}
	}

	@Inject(method = "mouseReleased", at = @At("RETURN"), cancellable = true)
	private void powerInventory_mouseReleasedOrSlotChanged(int mouseX, int mouseY, int button, CallbackInfo ci) {
		slot = this.getSlotAt(mouseX, mouseY);
		if (local.luke.power.input.InventoryPanels.modal(this) || local.luke.power.input.InventoryPanels.contains(this, mouseX, mouseY)) { power$cancelItemDrag(); return; }
		if (!powerInventory$eligible()) { power$cancelItemDrag(); return; }
		powerInventory$shiftDouble.move(slot);
		powerInventory$gatherDouble.move(slot);
		if (button == 0) { powerInventory$shiftDouble.release(); powerInventory$gatherDouble.release(); }

		/** - Do nothing if mouse is not over a slot */
		if (slot == null)
			return;

		if (Config.INVENTORY_TWEAKS_CONFIG.MOUSE_TWEAKS_CONFIG.SCROLL_WHEEL_CONFIG.enableScrollWheelTweaks) {
			if (!minecraft.world.isRemote) {
				int currentWheelDegrees = local.luke.power.input.InventoryWheel.takeEvent(this, org.lwjgl.opengl.Display.isActive());
				if ((0 != currentWheelDegrees)
						&& (isLeftClickDragStarted == false)
						&& (isRightClickDragStarted == false)
				) {
					powerInventory$shiftDouble.clear();
					powerInventory$gatherDouble.clear();
					powerInventory_handleScrollWheel(currentWheelDegrees);
				}
			}
		}

		/** - Right-click + Drag logic = distribute one item from held items to each slot */
		if (  ( button == -1 )
		   && ( Mouse.isButtonDown(1) )
		   && ( isLeftClickDragStarted == false )
		   && ( isLeftClickDragMouseTweaksStarted == false )
		   && ( rightClickPersistentStack != null )
		) {
			ItemStack slotItemToExamine = slot.getStack();
			if (!QuickTransfers.writable(slot, rightClickPersistentStack)) return;

			/** - Do nothing if slot item does not match held item or if the slot is full */
			if (  (null != slotItemToExamine)
			   && (  (!InventoryStacks.same(slotItemToExamine, rightClickPersistentStack))
				  || (slotItemToExamine.count >= Math.min(rightClickPersistentStack.getMaxCount(), slot.getMaxItemCount()))
			      )
			) {
				return;
			}

			/** - Do nothing if there are no more items to distribute */
			ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
			if (null == cursorStack) {
				return;
			}

			if (!rightClickHoveredSlots.contains(slot)) {
				powerInventory_handleRightClickDrag(slotItemToExamine);
			} else if (Config.INVENTORY_TWEAKS_CONFIG.MOUSE_TWEAKS_CONFIG.RMBTweak) {
				powerInventory_handleRightClickDragMouseTweaks();
			}
		} else {
			powerInventory_resetRightClickDragVariables();
		}

		/** - Left-click + Drag logic = evenly distribute held items over slots */
		if (  ( button == -1 )
		   && ( Mouse.isButtonDown(0) )
		   && ( isRightClickDragStarted == false )
		) {
			if (isLeftClickDragMouseTweaksStarted) {
				powerInventory_handleLeftClickDragMouseTweaks();
			} else if ( leftClickPersistentStack != null ) {
				if (powerInventory_handleLeftClickDrag()) {
					return;
				}
			} else {
				powerInventory_resetLeftClickDragVariables();
			}
		} else {
			powerInventory_resetLeftClickDragVariables();
		}
	}

	@Unique private void powerInventory_handleScrollWheel(int wheelDegrees) {
		ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
		ItemStack slotItemToExamine = slot.getStack();

		if (  (null != cursorStack)
		   || (null != slotItemToExamine)
		   )
		{
			//boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
			boolean transferAllowed = true;
			float numberOfTurns = (float)wheelDegrees / 120.0f;
			int cursorStackAmount = 0;
			int slotStackAmount = 0;
			ItemStack itemBeingTransfered = null;

			if (null != cursorStack) {
				itemBeingTransfered = cursorStack;
				cursorStackAmount = cursorStack.count;
			}

			if (null != slotItemToExamine) {
				itemBeingTransfered = slotItemToExamine;
				slotStackAmount = slotItemToExamine.count;
			}

			/** - Allow transfers if one or both of the slots are empty */
			if (  (null != cursorStack)
			   && (null != slotItemToExamine)
			) {
				/** - Prevent transfers if items in slots do not match */
				transferAllowed = InventoryStacks.same(cursorStack, slotItemToExamine);
			}

			/** - Prevent illegal transfers that can cause bugs/dupes */
			if (  (slot.id == 0) && (container instanceof CraftingScreenHandler)
			   || (slot.id == 2) && (container instanceof FurnaceScreenHandler)
			   || (  (container instanceof PlayerScreenHandler)
				  && (  (slot.id == 0)
					 ||	(slot.id == 5)
					 ||	(slot.id == 6)
					 ||	(slot.id == 7)
					 ||	(slot.id == 8)
				     )
			      )
			) {
				transferAllowed = false;
			}

			if (transferAllowed) {
				powerInventory_scrollCursorSlotTransfer(numberOfTurns, cursorStackAmount, slotStackAmount, itemBeingTransfered);
			}
		}
	}

	@Unique private void powerInventory_scrollCursorSlotTransfer(float turns, int cursorAmount, int slotAmount, ItemStack item) {
		if (!QuickTransfers.writable(slot, item)) return;
		if (Config.INVENTORY_TWEAKS_CONFIG.MOUSE_TWEAKS_CONFIG.SCROLL_WHEEL_CONFIG.invertScrollCursorSlotDirection) turns = -turns;
		int steps = (int) Math.ceil(Math.abs(turns));
		int amount;
		if (turns < 0) {
			amount = Math.min(steps, Math.min(cursorAmount, Math.max(0, Math.min(item.getMaxCount(), slot.getMaxItemCount()) - slotAmount)));
			cursorAmount -= amount;
			slotAmount += amount;
		} else {
			amount = Math.min(steps, Math.min(slotAmount, Math.max(0, item.getMaxCount() - cursorAmount)));
			cursorAmount += amount;
			slotAmount -= amount;
		}
		if (amount <= 0) return;
		ItemStack cursor = InventoryStacks.copy(item, cursorAmount);
		ItemStack stored = InventoryStacks.copy(item, slotAmount);
		slot.setStack(stored);
		minecraft.player.inventory.setCursorStack(cursor);
	}

	@Unique private boolean powerInventory_handleRightClick(int mouseX, int mouseY, Slot clickedSlot) {
		if (clickedSlot != null && ((clickedSlot.getClass() != Slot.class && !(clickedSlot instanceof PlayerSideSlot))
				|| !clickedSlot.canInsert(minecraft.player.inventory.getCursorStack()))) return false;
		/** - Get held item */
		ItemStack cursorStack = minecraft.player.inventory.getCursorStack();

		/** - Handle Right-click if an item is held */
		if (null != cursorStack) {

			/** - Ensure a slot was clicked */
			if (null != clickedSlot) {

				/** - Record how many items are in the slot */
				if (null != clickedSlot.getStack()) {

					/** - Let vanilla minecraft handle right click with an item onto a different item */
					if ( !InventoryStacks.same(cursorStack, clickedSlot.getStack()) ) {
						return false;
					}

					rightClickExistingAmount.add(clickedSlot.getStack().count);
				} else {
					rightClickExistingAmount.add(0);
				}

				/** - Begin Right-click + Drag */
				if (cursorStack != null && rightClickPersistentStack == null && isRightClickDragStarted == false) {
					rightClickPersistentStack = InventoryStacks.copy(cursorStack, cursorStack.count);
					rightClickItemAmount = rightClickPersistentStack.count;
					isRightClickDragStarted = true;
				}

				/** - Handle initial Right-click */
				lastRMBSlotId = clickedSlot.id;
				lastRMBSlot = clickedSlot;
				if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.RMBPreferShiftClick) {
					boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
					this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, 1, isShiftKeyDown, this.minecraft.player);

					if (isShiftKeyDown) {
						powerInventory_resetRightClickDragVariables();
					}
				} else {
					this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, 1, false, this.minecraft.player);
				}

				return true;
			}
		}

		return false;
	}

	@Unique private void powerInventory_handleRightClickDragMouseTweaks() {
		if (slot.id != lastRMBSlotId) {
			ItemStack cursorStack = minecraft.player.inventory.getCursorStack();

			if (null != cursorStack ) {
				/** - Distribute one item to the slot */
				lastRMBSlotId = slot.id;
				this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 1, false, this.minecraft.player);
			}
		}
	}

	@Unique private void powerInventory_handleRightClickDrag(ItemStack slotItemToExamine) {
		/** - First slot is handled instantly in mouseClicked function */
		if (slot.id != lastRMBSlotId) {
			if (0 == rightClickHoveredSlots.size())
			{
				/** - Add slot to item distribution */
				rightClickHoveredSlots.add(lastRMBSlot);
			}

			/** - Add slot to item distribution */
			rightClickHoveredSlots.add(slot);

			/** - Record how many items are in the slot */
			if (null != slotItemToExamine) {
				rightClickExistingAmount.add(slotItemToExamine.count);
			}
			else
			{
				rightClickExistingAmount.add(0);
			}

			/** - Distribute one item to the slot */
			lastRMBSlotId = slot.id;
			this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 1, false, this.minecraft.player);
		}
	}

	@Unique private boolean powerInventory_cancelRightClickDrag(boolean isClientOnServer)
	{
		/** - Cancel Right-click + Drag */
		if (isRightClickDragStarted) {
			if (rightClickHoveredSlots.size() > 1) {
				/** - Slots cannot return to normal on a server */
				if (!isClientOnServer) {
					/** - Return all slots to normal */
					minecraft.player.inventory.setCursorStack(InventoryStacks.copy(rightClickPersistentStack, rightClickItemAmount));
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < rightClickHoveredSlots.size(); leftClickHoveredSlotsIndex++) {
						if (0 != rightClickExistingAmount.get(leftClickHoveredSlotsIndex)) {
							rightClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(InventoryStacks.copy(rightClickPersistentStack, rightClickExistingAmount.get(leftClickHoveredSlotsIndex)));
						} else {
							rightClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(null);
						}
					}
				}

				/** - Reset Right-click + Drag variables and exit function */
				powerInventory_resetRightClickDragVariables();

				return true;
			}
		}

		return false;
	}

	@Unique private void powerInventory_resetRightClickDragVariables()
	{
		rightClickExistingAmount.clear();
		rightClickHoveredSlots.clear();
		rightClickPersistentStack = null;
		rightClickItemAmount = 0;
		isRightClickDragStarted = false;
	}

	@Unique private boolean powerInventory_handleLeftClickWithItem(ItemStack cursorStack, Slot clickedSlot, boolean isClientOnServer) {
		if (clickedSlot != null && ((clickedSlot.getClass() != Slot.class && !(clickedSlot instanceof PlayerSideSlot))
				|| !clickedSlot.canInsert(minecraft.player.inventory.getCursorStack()))) return false;
		/** - Ensure a slot was clicked */
		if (null != clickedSlot) {

			/** - Record how many items are in the slot and how many items are needed to fill the slot */
			if (null != clickedSlot.getStack()) {

				if (null != cursorStack) {
					/** - Let vanilla minecraft handle left click with an item onto any item */
					if (isClientOnServer) {
						return false;
					}

					/** - Let vanilla minecraft handle left click with an item onto a different item */
					if ( !InventoryStacks.same(cursorStack, clickedSlot.getStack()) ) {
						return false;
					}
				}

				int capacity = Math.min(cursorStack.getMaxCount(), clickedSlot.getMaxItemCount()) - clickedSlot.getStack().count;
				if (capacity <= 0) return true;
				leftClickAmountToFillPersistent.add(capacity);
				leftClickExistingAmount.add(clickedSlot.getStack().count);
			} else {
				leftClickAmountToFillPersistent.add(Math.min(cursorStack.getMaxCount(), clickedSlot.getMaxItemCount()));
				leftClickExistingAmount.add(0);
			}

			/** - Begin Left-click + Drag */
			if (cursorStack != null && leftClickPersistentStack == null && isLeftClickDragStarted == false) {
				leftClickPersistentStack = InventoryStacks.copy(cursorStack, cursorStack.count);
				leftClickItemAmount = leftClickPersistentStack.count;
				isLeftClickDragStarted = true;
			}

			/** - Handle initial Left-click */
			lastLMBSlotId = clickedSlot.id;
			lastLMBSlot = clickedSlot;
			if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.LMBPreferShiftClick) {
				boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
				this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, 0, isShiftKeyDown, this.minecraft.player);

				if (isShiftKeyDown) {
					powerInventory_resetLeftClickDragVariables();
					leftClickMouseTweaksPersistentStack = cursorStack;
					isLeftClickDragMouseTweaksStarted = true;
				}
			} else {
				this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, 0, false, this.minecraft.player);
			}

			return true;
		}

		return false;
	}

	@Unique private boolean powerInventory_handleLeftClickWithoutItem(Slot clickedSlot) {
		isLeftClickDragMouseTweaksStarted = true;

		/** - Ensure a slot was clicked */
		if (clickedSlot != null) {
			boolean isShiftKeyDown = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));

			if (isShiftKeyDown) powerInventory$startShiftDrag(clickedSlot);

			/** - Get info for MouseTweaks `Left-Click + Drag` mechanics */
			if (!isShiftKeyDown) {
				ItemStack itemInSlot = clickedSlot.getStack();
				leftClickMouseTweaksPersistentStack = itemInSlot;
			}

			/** - Handle initial Left-click */
			lastLMBSlotId = clickedSlot.id;
			lastLMBSlot = clickedSlot;
			this.minecraft.interactionManager.clickSlot(this.container.syncId, clickedSlot.id, 0, isShiftKeyDown, this.minecraft.player);
			ItemStack picked = minecraft.player.inventory.getCursorStack();
			if (!isShiftKeyDown && picked != null && clickedSlot.getClass() == Slot.class)
				powerInventory$gatherDouble.start(container, clickedSlot, picked, InventoryStacks.data(picked), MouseClickClock.now());


			return true;
		} else {
			/** - Get info for MouseTweaks `Left-Click + Drag` mechanics */
			leftClickMouseTweaksPersistentStack = null;
		}

		return false;
	}

	@Unique private void powerInventory_handleLeftClickDragMouseTweaks() {
		boolean shift = powerInventory$shiftDown();
		boolean armor = ArmorTransfers.armorSlot(container, slot);
		if (slot.getClass() != Slot.class && !(slot instanceof PlayerSideSlot)
				&& !(shift && minecraft.player.inventory.getCursorStack() == null && armor)) return;
		if (slot.id != lastLMBSlotId) {
			lastLMBSlotId = slot.id;

			ItemStack slotItemToExamine = slot.getStack();
			if (null != slotItemToExamine)
			{
				if (null != leftClickMouseTweaksPersistentStack)
				{
					if (InventoryStacks.same(slotItemToExamine, leftClickMouseTweaksPersistentStack))
					{
						if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
							if (Config.INVENTORY_TWEAKS_CONFIG.MOUSE_TWEAKS_CONFIG.LMBTweakShiftClick)
							{
								this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, true, this.minecraft.player);
							}
						} else {
							if (Config.INVENTORY_TWEAKS_CONFIG.MOUSE_TWEAKS_CONFIG.LMBTweakPickUp) {
								ItemStack cursorStack = minecraft.player.inventory.getCursorStack();

								if (cursorStack == null) {
									/** - Pick up items from slot */
									this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);
								} else if (cursorStack.count < leftClickMouseTweaksPersistentStack.getMaxCount()) {
									int amountAbleToPickUp = leftClickMouseTweaksPersistentStack.getMaxCount() - cursorStack.count;
									int amountInSlot = slotItemToExamine.count;

									/** - Pick up items from slot */
									if (amountInSlot <= amountAbleToPickUp) {
										this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);
										this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);
									} else if (cursorStack.count == leftClickMouseTweaksPersistentStack.getMaxCount()) {
										slot.setStack(InventoryStacks.copy(leftClickMouseTweaksPersistentStack, cursorStack.count));
										minecraft.player.inventory.setCursorStack(InventoryStacks.copy(leftClickMouseTweaksPersistentStack, amountInSlot));
									} else {
										this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);

										slotItemToExamine = slot.getStack();
										cursorStack = minecraft.player.inventory.getCursorStack();
										amountInSlot = slotItemToExamine.count;

										slot.setStack(InventoryStacks.copy(leftClickMouseTweaksPersistentStack, cursorStack.count));
										minecraft.player.inventory.setCursorStack(InventoryStacks.copy(leftClickMouseTweaksPersistentStack, amountInSlot));
									}
								}
							}
						}
					}
				} else if (  (Config.INVENTORY_TWEAKS_CONFIG.MOUSE_TWEAKS_CONFIG.LMBTweakShiftClickAny)
						  && (  (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
						     || (Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
				             )
				) {
					if (!powerInventory$shiftVisited.add(slot)) return;
					if (armor && (!slot.canInsert(slotItemToExamine) || slot.getMaxItemCount() < 1)) return;
					if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.EnableShiftClickingItemsIntoArmorSlots
							&& ArmorTransfers.equip(minecraft, container, slot, 0)) return;
					this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, true, this.minecraft.player);
				}
			}
		}
	}

	@Unique private boolean powerInventory_handleLeftClickDrag()
	{
		if (!QuickTransfers.writable(slot, leftClickPersistentStack)) return true;
		/** - Do nothing if slot has already been added to Left-click + Drag logic */
		if (!leftClickHoveredSlots.contains(slot)) {
			ItemStack slotItemToExamine = slot.getStack();

			/** - Check if client is on a server */
			boolean isClientOnServer = minecraft.world.isRemote;

			/** - Do nothing if slot item does not match held item */
			if (null != slotItemToExamine){

				if (isClientOnServer) {
					return true;
				}

				if (!InventoryStacks.same(slotItemToExamine, leftClickPersistentStack)) {
					return true;
				}
				if (slotItemToExamine.count >= Math.min(leftClickPersistentStack.getMaxCount(), slot.getMaxItemCount())) return true;
			}

			/** - Do nothing if there are no more items to distribute */
			if (1.0 == (double)leftClickItemAmount / (double)leftClickHoveredSlots.size()) {
				return true;
			}

			/** - First slot is handled instantly in mouseClicked function */
			if (slot.id != lastLMBSlotId) {
				if (leftClickHoveredSlots.isEmpty())
				{
					/** - Add slot to item distribution */
					leftClickHoveredSlots.add(lastLMBSlot);
				}

				/** - Add slot to item distribution */
				leftClickHoveredSlots.add(slot);

				/** - Record how many items are in the slot and how many items are needed to fill the slot */
				if (null != slotItemToExamine) {
					leftClickAmountToFillPersistent.add(Math.min(leftClickPersistentStack.getMaxCount(), slot.getMaxItemCount()) - slotItemToExamine.count);
					leftClickExistingAmount.add(slotItemToExamine.count);
				}
				else
				{
					leftClickAmountToFillPersistent.add(Math.min(leftClickPersistentStack.getMaxCount(), slot.getMaxItemCount()));
					leftClickExistingAmount.add(0);
				}

				/** - Slots cannot return to normal on a server */
				List<Integer> leftClickAmountToFill = new ArrayList<>();
				if (!isClientOnServer) {
					/** - Return all slots to normal */
					minecraft.player.inventory.setCursorStack(InventoryStacks.copy(leftClickPersistentStack, leftClickItemAmount));
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < leftClickHoveredSlots.size(); leftClickHoveredSlotsIndex++) {
						leftClickAmountToFill.add(leftClickAmountToFillPersistent.get(leftClickHoveredSlotsIndex));
						if (0 != leftClickExistingAmount.get(leftClickHoveredSlotsIndex)) {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(InventoryStacks.copy(leftClickPersistentStack, leftClickExistingAmount.get(leftClickHoveredSlotsIndex)));
						} else {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(null);
						}
					}
				}

				/** - Prepare to distribute over slots */
				int numberOfSlotsRemainingToFill = leftClickHoveredSlots.size();
				int itemsPerSlot = leftClickItemAmount / numberOfSlotsRemainingToFill;
				int leftClickRemainingItemAmount = leftClickItemAmount;
				boolean rerunLoop;

				/** - Slots cannot return to normal on a server */
				if (!isClientOnServer) {
					/** - Distribute fewer items to slots whose max stack size will be filled */
					do {
						rerunLoop = false;
						if (0 < numberOfSlotsRemainingToFill) {
							itemsPerSlot = leftClickRemainingItemAmount / numberOfSlotsRemainingToFill;

							if (0 != itemsPerSlot) {
								for (int slotsToCheckIndex = 0; slotsToCheckIndex < leftClickAmountToFill.size(); slotsToCheckIndex++) {
									if (0 != leftClickAmountToFill.get(slotsToCheckIndex) && leftClickAmountToFill.get(slotsToCheckIndex) < itemsPerSlot) {
										/** - Just fill the slot and return */
										for (int fillTheAmountIndex = 0; fillTheAmountIndex < leftClickAmountToFill.get(slotsToCheckIndex); fillTheAmountIndex++) {
											this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get(slotsToCheckIndex).id, 1, false, this.minecraft.player);
										}

										leftClickRemainingItemAmount = leftClickRemainingItemAmount - leftClickAmountToFill.get(slotsToCheckIndex);
										leftClickAmountToFill.set(slotsToCheckIndex, 0);
										numberOfSlotsRemainingToFill--;
										rerunLoop = true;
									}
								}
							}
						}
					} while (rerunLoop && 0 < numberOfSlotsRemainingToFill);
				} else {
					/** - Return slots to normal on when client is on a server */
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < (leftClickHoveredSlots.size() - 1); leftClickHoveredSlotsIndex++)
					{
						ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
						if (leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).hasStack() && leftClickHoveredSlots.size() > 1)
						{
							if (cursorStack != null)
							{
								this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
							}
							this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
						}
					}
				}

				/** - Distribute remaining items evenly over remaining slots that were not already filled to max stack size */
				for (int distributeSlotsIndex = 0; distributeSlotsIndex < leftClickHoveredSlots.size(); distributeSlotsIndex++) {
					if (isClientOnServer) {
						if (0 != leftClickAmountToFillPersistent.get(distributeSlotsIndex)) {
							for (int addSlotIndex = 0; addSlotIndex < itemsPerSlot; addSlotIndex++) {
								this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get(distributeSlotsIndex).id, 1, false, this.minecraft.player);
							}
						}
					} else {
						if (0 != leftClickAmountToFill.get(distributeSlotsIndex)) {
							for (int addSlotIndex = 0; addSlotIndex < itemsPerSlot; addSlotIndex++) {
								this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get(distributeSlotsIndex).id, 1, false, this.minecraft.player);
							}
						}
					}
				}
			}
		}

		return false;
	}

	@Unique private boolean powerInventory_cancelLeftClickDrag(boolean isClientOnServer)
	{
		/** - Cancel Left-click + Drag */
		if (isLeftClickDragStarted) {
			if (leftClickHoveredSlots.size() > 1) {
				/** - Check if client is running on a server or not */
				if (!isClientOnServer) {
					/** - Return all slots to normal */
					minecraft.player.inventory.setCursorStack(InventoryStacks.copy(leftClickPersistentStack, leftClickItemAmount));
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < leftClickHoveredSlots.size(); leftClickHoveredSlotsIndex++) {
						if (0 != leftClickExistingAmount.get(leftClickHoveredSlotsIndex)) {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(InventoryStacks.copy(leftClickPersistentStack, leftClickExistingAmount.get(leftClickHoveredSlotsIndex)));
						} else {
							leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).setStack(null);
						}
					}
				} else {
					/** - Return slots to normal on when client is on a server */
					for (int leftClickHoveredSlotsIndex = 0; leftClickHoveredSlotsIndex < (leftClickHoveredSlots.size() - 1); leftClickHoveredSlotsIndex++)
					{
						ItemStack cursorStack = minecraft.player.inventory.getCursorStack();
						if (leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).hasStack() && leftClickHoveredSlots.size() > 1)
						{
							if (cursorStack != null)
							{
								this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
							}
							this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get(leftClickHoveredSlotsIndex).id, 0, false, this.minecraft.player);
						}
					}
					this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get((leftClickHoveredSlots.size() - 1)).id, 0, false, this.minecraft.player);
					this.minecraft.interactionManager.clickSlot(this.container.syncId, leftClickHoveredSlots.get((leftClickHoveredSlots.size() - 1)).id, 0, false, this.minecraft.player);
				}

				/** - Reset Left-click + Drag variables and exit function */
				powerInventory_resetLeftClickDragVariables();
				return true;
			}
		}

		return false;
	}

	@Unique private void powerInventory_resetLeftClickDragVariables()
	{
		powerInventory$shiftVisited.clear();
		leftClickExistingAmount.clear();
		leftClickAmountToFillPersistent.clear();
		leftClickHoveredSlots.clear();
		leftClickPersistentStack = null;
		leftClickMouseTweaksPersistentStack = null;
		leftClickItemAmount = 0;
		isLeftClickDragStarted = false;
		isLeftClickDragMouseTweaksStarted = false;
	}

	@Inject(method = "keyPressed", at = @At("RETURN"))
	private void powerInventory_keyPressed(char character, int keyCode, CallbackInfo ci) {
		// Beta dispatches the mouse batch before queued keyboard events. The Shift press
		// may therefore arrive here after the matching first click has already started.
		if (keyCode != Keyboard.KEY_LSHIFT && keyCode != Keyboard.KEY_RSHIFT) powerInventory$shiftDouble.clear();
		powerInventory$gatherDouble.clear();
		if (!powerInventory$eligible()) { power$cancelItemDrag(); return; }
		if (this.slot == null) {
			return;
		}

		if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.UseDropKeyInInventory) {
			if (keyCode == local.luke.power.input.Bindings.eventCode(this.minecraft.options.dropKey)) {
				if (this.minecraft.player.inventory.getCursorStack() != null) {
					return;
				}

				this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);
				if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.LCtrlStackDrop) {
					this.minecraft.interactionManager.clickSlot(this.container.syncId, -999, Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ? 0 : 1, false, this.minecraft.player);
				} else {
					this.minecraft.interactionManager.clickSlot(this.container.syncId, -999, 1, false, this.minecraft.player);
				}
				this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);
			}
		}

		if (Config.INVENTORY_TWEAKS_CONFIG.MODERN_MINECRAFT_CONFIG.NumKeyHotbarSwap) {
			if (ModHelper.isControlFeaturesModLoaded) {
				keyCode = ModHelperControlFeatures.remapKeyCodes(keyCode);
			}

			if (keyCode >= Keyboard.KEY_1 && keyCode <= Keyboard.KEY_9) {
				if (  (null != this.container.slots)
				   && (10 <= this.container.slots.size())
				) {
					int hotbar = -1;
					for (Object entry : this.container.slots) {
						Slot candidate = (Slot)entry;
						SlotAccessor access = (SlotAccessor)candidate;
						if (access.powerInventory$inventory() == this.minecraft.player.inventory
						    && access.powerInventory$index() == keyCode - Keyboard.KEY_1) { hotbar = candidate.id; break; }
					}
					if (hotbar < 0 || hotbar == slot.id) return;
					if (this.minecraft.player.inventory.getCursorStack() == null) {
						this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);
					}
					this.minecraft.interactionManager.clickSlot(this.container.syncId, hotbar, 0, false, this.minecraft.player);
					this.minecraft.interactionManager.clickSlot(this.container.syncId, slot.id, 0, false, this.minecraft.player);
				}
			}
		}
	}
}
