package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.BlockBreakingListener;
import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class NoMissDelay extends Module implements AttackListener, BlockBreakingListener, ItemUseListener {
	public enum Mode {
		MACE("Mace"),
		ONLY_WEAPONS("Only Weapons"),
		ALL_ITEMS("All Items"),
		MACE_AND_WEAPONS("Mace and Weapons");

		private final String name;

		Mode(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.MACE, Mode.class);
	private final BooleanSetting air = new BooleanSetting(EncryptedString.of("Air"), true)
			.setDescription(EncryptedString.of("Whether to stop hits directed to the air"));
	private final BooleanSetting blocks = new BooleanSetting(EncryptedString.of("Blocks"), false)
			.setDescription(EncryptedString.of("Whether to stop hits directed to blocks"));
	private final BooleanSetting chestplate = new BooleanSetting(EncryptedString.of("Chestplate"), false)
			.setDescription(EncryptedString.of("Only allows chestplate/elytra right-click equip/swap while on ground or within 2 blocks of ground"));

	public NoMissDelay() {
		super(EncryptedString.of("Triggerbot Optimizer"),
				EncryptedString.of("Only allows you to do more actions with triggerbot"),
				-1,
				Category.optimizer);
		addSettings(mode, air, blocks, chestplate);
	}

	@Override
	public void onEnable() {
		eventManager.add(AttackListener.class, this);
		eventManager.add(BlockBreakingListener.class, this);
		eventManager.add(ItemUseListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(AttackListener.class, this);
		eventManager.remove(BlockBreakingListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		super.onDisable();
	}

	@Override
	public void onItemUse(ItemUseEvent event) {
		if (!chestplate.getValue()) return;
		if (mc.player == null || mc.world == null) return;

		boolean hasShieldInHotbar = false;
		for (int i = 0; i < 9; i++) {
			if (mc.player.getInventory().getStack(i).getItem() instanceof ShieldItem) {
				hasShieldInHotbar = true;
				break;
			}
		}
		if (!hasShieldInHotbar) return;

		Item heldItem = mc.player.getMainHandStack().getItem();
		boolean isChestEquipItem = (heldItem instanceof ElytraItem)
				|| (heldItem instanceof ArmorItem armor && armor.getSlotType() == EquipmentSlot.CHEST);
		if (!isChestEquipItem) return;

		if (mc.player.isOnGround()) return;

		Vec3d start = mc.player.getPos();
		Vec3d end = start.add(0.0, -2.0, 0.0);
		HitResult groundCheck = mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
		if (groundCheck != null && groundCheck.getType() != HitResult.Type.MISS) {
			return;
		}

		event.cancel();
	}

	@Override
	public void onAttack(AttackEvent event) {
		Item heldItem = mc.player.getMainHandStack().getItem();
		if (shouldSkipAttack(heldItem)) {
			return;
		}
		switch (mc.crosshairTarget.getType()) {
			case MISS -> { if (air.getValue()) event.cancel(); }
			case BLOCK -> { if (blocks.getValue()) event.cancel(); }
		}
	}

	private boolean shouldSkipAttack(Item item) {
		if (mode.isMode(Mode.MACE)) {
			return !(item instanceof MaceItem);		}
		else if (mode.isMode(Mode.ONLY_WEAPONS)) {
			return !(item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem);
		}
		else if (mode.isMode(Mode.MACE_AND_WEAPONS)) {
			return !(item instanceof MaceItem) && !(item instanceof SwordItem || item instanceof AxeItem);
		}
		return false;
	}

	@Override
	public void onBlockBreaking(BlockBreakingEvent event) {
		Item heldItem = mc.player.getMainHandStack().getItem();
		if (mode.isMode(Mode.MACE) && !(heldItem instanceof MaceItem)) {
			return;
		}
		else if (mode.isMode(Mode.ONLY_WEAPONS) &&
				!(heldItem instanceof SwordItem || heldItem instanceof AxeItem || heldItem instanceof MaceItem)) {
			return;
		}
		else if (mode.isMode(Mode.MACE_AND_WEAPONS) &&
				!(heldItem instanceof MaceItem) &&
				!(heldItem instanceof SwordItem || heldItem instanceof AxeItem)) {
			return;
		}

		if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK && blocks.getValue()) {
			event.cancel();
		}
	}
}