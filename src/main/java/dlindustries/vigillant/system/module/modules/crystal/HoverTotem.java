package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.mixin.HandledScreenMixin;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.concurrent.ThreadLocalRandom;

public final class HoverTotem extends Module implements TickListener {
	private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0, 20, 1, 1);
	private final BooleanSetting hotbar = new BooleanSetting(EncryptedString.of("Hotbar"), true)
			.setDescription(EncryptedString.of("Puts a totem in your hotbar as well"));
	private final NumberSetting slot = new NumberSetting(EncryptedString.of("Hotbar Slot"), 1, 9, 9, 1)
			.setDescription(EncryptedString.of("Your preferred hotbar slot for replacements"));
	private final BooleanSetting dynamicDelay = new BooleanSetting(EncryptedString.of("Dynamic Delay"), true)
			.setDescription(EncryptedString.of("Adds further random timing variations to avoid detection"));

	private int clock;
	// prevent multiple swaps while inventory remains open
	private boolean swappedThisOpen;

	public HoverTotem() {
		super(EncryptedString.of("HotbarReplace - shield/axe"),
				EncryptedString.of("Replaces hovered shields or axes into the configured hotbar slot (once per inventory open)"),
				-1,
				Category.CRYSTAL);
		addSettings(delay, hotbar, slot, dynamicDelay);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		clock = 0;
		swappedThisOpen = false;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}

	@Override
	public void onTick() {
		// Only operate while inventory screen is open
		if (!(mc.currentScreen instanceof InventoryScreen inv)) {
			// Reset when inventory closed so next open allows a new swap
			clock = delay.getValueInt();
			swappedThisOpen = false;
			return;
		}

		Slot hoveredSlot = ((HandledScreenMixin) inv).getFocusedSlot();
		// keep same bounds as original: only consider normal inventory slots (avoid crafting/result/offscreen)
		if (hoveredSlot == null || hoveredSlot.getIndex() > 35) return;

		// identify shield or any AxeItem
		final var hoveredItem = hoveredSlot.getStack().getItem();
		boolean isShield = hoveredItem == Items.SHIELD;
		boolean isAxe = hoveredItem instanceof AxeItem;

		// nothing to do if hovered item isn't shield or axe
		if (!isShield && !isAxe) return;

		// only swap once per inventory open (prevents spamming)
		if (swappedThisOpen) return;

		// respect delay as before
		if (clock > 0) {
			clock--;
			return;
		}

		// Only swap into hotbar slot; never touch offhand
		if (hotbar.getValue()) {
			executeHotbarReplace(inv, hoveredSlot);
			swappedThisOpen = true;
			clock = getDynamicDelay();
		}
	}

	private int getDynamicDelay() {
		int base = delay.getValueInt();
		if (dynamicDelay.getValue()) {
			return Math.max(0, base + ThreadLocalRandom.current().nextInt(-2, 3));
		} else {
			return Math.max(0, base);
		}
	}

	private void performSwap(InventoryScreen inv, int from, int to) {
		mc.interactionManager.clickSlot(
				inv.getScreenHandler().syncId,
				from,
				to,
				SlotActionType.SWAP,
				mc.player
		);
	}

	/**
	 * Swap the hovered shield/axe into the configured hotbar slot only.
	 * This method will not touch offhand at all.
	 */
	private void executeHotbarReplace(InventoryScreen inv, Slot hoveredSlot) {
		int hotbarSlot = slot.getValueInt() - 1;

		// perform swap from hovered inventory slot to user's chosen hotbar slot
		performSwap(inv, hoveredSlot.getIndex(), hotbarSlot);
	}
}
